package com.ivianuu.apelabs.domain

import android.annotation.*
import android.bluetooth.*
import arrow.core.*
import arrow.fx.coroutines.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.*
import com.ivianuu.essentials.coroutines.*
import com.ivianuu.essentials.logging.*
import com.ivianuu.essentials.ui.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import java.util.*
import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds

@Provide @Scoped<UiScope> class WappRemote(
  private val coroutineContexts: CoroutineContexts,
  private val logger: Logger,
  private val serverFactory: (String) -> WappServer,
  scope: ScopedCoroutineScope<UiScope>
) {
  private val servers = scope.sharedResource<String, WappServer>(
    create = { serverFactory(it) },
    release = { _, server -> server.close() }
  )

  suspend fun <R> withWapp(
    address: String,
    connectTimeout: Duration = Duration.INFINITE,
    block: suspend WappServer.() -> R,
  ): R? = withContext(coroutineContexts.io) {
    servers.use(address) { server ->
      withTimeoutOrNull(connectTimeout) { server.isConnected.first { it } }
        ?: return@use null

      raceN(
        { block(server) },
        {
          server.isConnected.first { !it }
          logger.d { "${server.device.debugName()} cancel with wapp" }
        }
      ).leftOrNull()
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
    extraBufferCapacity = Int.MAX_VALUE
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
          logger.d { "${device.debugName()} connection state changed $newState" }
          if (isConnected)
            gatt.discoverServices()
          else
            this@WappServer.isConnected.tryEmit(false)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
          super.onServicesDiscovered(gatt, status)
          logger.d { "${device.debugName()} services discovered" }

          scope.launch {
            val readCharacteristic = gatt
              .getService(APE_LABS_SERVICE_ID)
              ?.getCharacteristic(APE_LABS_READ_ID)

            if (readCharacteristic == null) {
              gatt.discoverServices()
              return@launch
            }

            gatt.setCharacteristicNotification(readCharacteristic, true)

            val cccDescriptor = readCharacteristic.getDescriptor(CCCD_ID)

            if (cccDescriptor == null) {
              gatt.discoverServices()
              return@launch
            }

            suspend fun setReadNotification(attempt: Int) {
              logger.d { "try set read notification -> $attempt" }
              gatt.writeDescriptor(cccDescriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
              withTimeoutOrNull(300.milliseconds) {
                writeResults.first { it.first == cccDescriptor }
              } ?: run { if (attempt < 5) setReadNotification(attempt + 1) }
            }

            writeLock.withLock { setReadNotification(1) }

            isConnected.emit(true)
          }
        }

        override fun onCharacteristicChanged(
          gatt: BluetoothGatt,
          characteristic: BluetoothGattCharacteristic,
          value: ByteArray
        ) {
          super.onCharacteristicChanged(gatt, characteristic, value)
          messages.tryEmit(value)
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
  private val writeLimiter = RateLimiter(1, 200.milliseconds)

  init {
    logger.d { "${device.debugName()} init" }
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

    suspend fun writeImpl(attempt: Int) {
      logger.d { "${device.debugName()} write -> ${message.contentToString()} attempt $attempt" }
      gatt.writeCharacteristic(characteristic, message, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
      withTimeoutOrNull(200.milliseconds) {
        writeResults.first { it.first == characteristic }
      } ?: run { if (attempt < 5) writeImpl(attempt + 1) }
    }

    writeLock.withLock {
      writeLimiter.acquire()
      writeImpl(1)
    }
  }

  suspend fun close(): Unit = withContext(coroutineContexts.io) {
    logger.d { "${device.debugName()} close" }
    Either.catch { gatt.disconnect() }
    Either.catch { gatt.close() }
  }
}

private val CCCD_ID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val APE_LABS_SERVICE_ID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
private val APE_LABS_WRITE_ID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
private val APE_LABS_READ_ID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
