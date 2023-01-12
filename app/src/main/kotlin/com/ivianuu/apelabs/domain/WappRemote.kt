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
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.catch
import com.ivianuu.essentials.coroutines.EventFlow
import com.ivianuu.essentials.coroutines.RateLimiter
import com.ivianuu.essentials.coroutines.RefCountedResource
import com.ivianuu.essentials.coroutines.race
import com.ivianuu.essentials.coroutines.withResource
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.time.milliseconds
import com.ivianuu.essentials.time.seconds
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.android.SystemService
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*

context(Logger, NamedCoroutineScope<AppScope>)
@Provide @Scoped<AppScope> class WappRemote(
  private val appContext: AppContext,
  private val bluetoothManager: @SystemService BluetoothManager,
  private val context: IOContext
) {
  private val servers = RefCountedResource<String, WappServer>(
    scope = this@NamedCoroutineScope,
    timeout = 5.seconds,
    create = { WappServer(it, context, appContext, bluetoothManager) },
    release = { _, server -> server.close() }
  )

  suspend fun <R> withWapp(
    address: String,
    block: suspend WappServer.() -> R
  ): R? = withContext(context) {
    servers.withResource(address) {
      race(
        {
          it.serviceChanges.first()
          block(it)
        },
        {
          it.serviceChanges.first()
          it.connectionState.first { !it }
          log { "${it.device.debugName()} cancel with wapp" }
        }
      ) as? R
    }
  }
}

context(CoroutineScope, Logger)
@SuppressLint("MissingPermission")
class WappServer(
  address: String,
  private val context: IOContext,
  appContext: AppContext,
  bluetoothManager: BluetoothManager
) {
  val connectionState = MutableSharedFlow<Boolean>(
    replay = 1,
    extraBufferCapacity = Int.MAX_VALUE,
    onBufferOverflow = BufferOverflow.SUSPEND
  )
  val serviceChanges = MutableSharedFlow<Unit>(
    replay = 1,
    extraBufferCapacity = Int.MAX_VALUE,
    onBufferOverflow = BufferOverflow.SUSPEND
  )

  val device = bluetoothManager.adapter.getRemoteDevice(address)

  val messages = EventFlow<ByteArray>()

  private val gatt = bluetoothManager.adapter
    .getRemoteDevice(address)
    .connectGatt(
      appContext,
      true,
      object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
          super.onConnectionStateChange(gatt, status, newState)
          val isConnected = newState == BluetoothProfile.STATE_CONNECTED
          log { "${device.debugName()} connection state changed $newState" }
          connectionState.tryEmit(isConnected)
          if (isConnected)
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
          super.onServicesDiscovered(gatt, status)
          log { "${device.debugName()} services discovered" }

          launch {
            val readCharacteristic = gatt
              .getService(APE_LABS_SERVICE_ID)
              .getCharacteristic(APE_LABS_READ_ID)
            writeLimiter.acquire()
            gatt.setCharacteristicNotification(readCharacteristic, true)
            val cccDescriptor = readCharacteristic.getDescriptor(CCCD_ID)
            cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            writeLimiter.acquire()
            gatt.writeDescriptor(cccDescriptor)
            onCharacteristicChanged(gatt, readCharacteristic)
            serviceChanges.tryEmit(Unit)
          }
        }

        override fun onCharacteristicChanged(
          gatt: BluetoothGatt,
          characteristic: BluetoothGattCharacteristic
        ) {
          super.onCharacteristicChanged(gatt, characteristic)

          val message = characteristic.value ?: return

          messages.tryEmit(message)
        }
      },
      BluetoothDevice.TRANSPORT_LE
    )

  private val writeLock = Mutex()
  private val writeLimiter = RateLimiter(1, 200.milliseconds)

  init {
    log { "${device.debugName()} init" }
  }

  suspend fun write(message: ByteArray) = withContext(context) {
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
      log { "${device.debugName()} write -> ${message.contentToString()}" }
      characteristic.value = message
      writeLimiter.acquire()
      gatt.writeCharacteristic(characteristic)
    }
  }

  suspend fun close() = withContext(context) {
    log { "${device.debugName()} close" }
    catch { gatt.disconnect() }
    catch { gatt.close() }
  }
}

private val CCCD_ID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val APE_LABS_SERVICE_ID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
private val APE_LABS_WRITE_ID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
private val APE_LABS_READ_ID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
