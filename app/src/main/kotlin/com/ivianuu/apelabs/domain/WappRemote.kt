package com.ivianuu.apelabs.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import com.ivianuu.apelabs.data.debugName
import com.ivianuu.essentials.AppContext
import com.ivianuu.essentials.coroutines.EventFlow
import com.ivianuu.essentials.coroutines.race
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Provide
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.SystemService
import com.ivianuu.essentials.coroutines.CoroutineContexts
import com.ivianuu.essentials.coroutines.ScopedCoroutineScope
import com.ivianuu.essentials.coroutines.sharedResource
import com.ivianuu.essentials.coroutines.use
import com.ivianuu.essentials.result.catch
import com.ivianuu.essentials.ui.UiScope
import com.ivianuu.essentials.unsafeCast
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

@Provide @Scoped<UiScope> class WappRemote(
  private val coroutineContexts: CoroutineContexts,
  private val logger: Logger,
  private val serverFactory: (String) -> WappServer,
  private val scope: ScopedCoroutineScope<UiScope>
) {
  private val servers = scope.sharedResource<String, WappServer>(
    create = { serverFactory(it) },
    release = { _, server -> server.close() }
  )

  suspend fun <R> withWapp(
    address: String,
    block: suspend WappServer.() -> R,
  ): R? = withContext(coroutineContexts.io) {
    servers.use(address) {
      it.isConnected.first { it }
      race(
        { block(it) },
        {
          it.isConnected.first { !it }
          logger.log { "${it.device.debugName()} cancel with wapp" }
        }
      ).unsafeCast()
    }
  }
}

@SuppressLint("MissingPermission")
@Provide class WappServer(
  address: String,
  appContext: AppContext,
  bluetoothManager: @SystemService BluetoothManager,
  private val coroutineContexts: CoroutineContexts,
  private val logger: Logger,
  private val scope: ScopedCoroutineScope<UiScope>,
) {
  val isConnected = MutableSharedFlow<Boolean>(
    replay = 1,
    extraBufferCapacity = Int.MAX_VALUE,
    onBufferOverflow = BufferOverflow.SUSPEND
  )

  val device: BluetoothDevice = bluetoothManager.adapter.getRemoteDevice(address)

  val messages = EventFlow<ByteArray>()
  private val writeResults = EventFlow<Pair<Any, Int>>()

  private val gatt = bluetoothManager.adapter
    .getRemoteDevice(address)
    .connectGatt(
      appContext,
      true,
      object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
          super.onConnectionStateChange(gatt, status, newState)
          val isConnected = newState == BluetoothProfile.STATE_CONNECTED
          logger.log { "${device.debugName()} connection state changed $newState" }
          if (isConnected)
            gatt.discoverServices()
          else
            this@WappServer.isConnected.tryEmit(false)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
          super.onServicesDiscovered(gatt, status)
          logger.log { "${device.debugName()} services discovered" }

          scope.launch {
            val readCharacteristic = gatt
              .getService(APE_LABS_SERVICE_ID)
              .getCharacteristic(APE_LABS_READ_ID)
            gatt.setCharacteristicNotification(readCharacteristic, true)

            val cccDescriptor = readCharacteristic.getDescriptor(CCCD_ID)
            cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccDescriptor)
            writeResults.first { it.first == cccDescriptor }

            isConnected.emit(true)
          }
        }

        override fun onCharacteristicChanged(
          gatt: BluetoothGatt,
          characteristic: BluetoothGattCharacteristic,
        ) {
          super.onCharacteristicChanged(gatt, characteristic)

          val message = characteristic.value ?: return

          messages.tryEmit(message)
        }

        override fun onDescriptorWrite(
          gatt: BluetoothGatt,
          descriptor: BluetoothGattDescriptor,
          status: Int,
        ) {
          super.onDescriptorWrite(gatt, descriptor, status)
          writeResults.tryEmit(descriptor to status)
        }

        override fun onCharacteristicWrite(
          gatt: BluetoothGatt,
          characteristic: BluetoothGattCharacteristic,
          status: Int,
        ) {
          super.onCharacteristicWrite(gatt, characteristic, status)
          writeResults.tryEmit(characteristic to status)
        }
      },
      BluetoothDevice.TRANSPORT_LE
    )

  private val writeLock = Mutex()

  init {
    logger.log { "${device.debugName()} init" }
  }

  suspend fun write(message: ByteArray) = withContext(coroutineContexts.io) {
    val service = gatt.getService(APE_LABS_SERVICE_ID) ?: error(
      "${device.debugName()} service not found ${
        gatt.services.map {
          it.uuid
        }
      }"
    )
    val characteristic = service.getCharacteristic(APE_LABS_WRITE_ID)
      ?: error("${device.debugName()} characteristic not found")

    writeLock.withLock {
      withContext(NonCancellable) {
        logger.log { "${device.debugName()} write -> ${message.contentToString()}" }
        characteristic.value = message
        gatt.writeCharacteristic(characteristic)
        withTimeoutOrNull(300.milliseconds) {
          writeResults.first { it.first == characteristic }
        }
      }
    }
  }

  suspend fun close() = withContext(coroutineContexts.io) {
    logger.log { "${device.debugName()} close" }
    catch { gatt.disconnect() }
    catch { gatt.close() }
  }
}

private val CCCD_ID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val APE_LABS_SERVICE_ID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
private val APE_LABS_WRITE_ID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
private val APE_LABS_READ_ID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
