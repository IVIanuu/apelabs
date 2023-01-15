package com.ivianuu.apelabs.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.ivianuu.apelabs.domain.WappRemote
import com.ivianuu.apelabs.domain.apeLabsPermissionKeys
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.onCancel
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.permission.PermissionManager
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.android.SystemService
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Provide @Scoped<AppScope> class WappRepository(
  private val bluetoothManager: @SystemService BluetoothManager,
  context: IOContext,
  private val logger: Logger,
  permissionManager: PermissionManager,
  private val remote: WappRemote,
  scope: NamedCoroutineScope<AppScope>
) {
  val wapps: Flow<List<Wapp>> = permissionManager.permissionState(apeLabsPermissionKeys)
    .flatMapLatest {
      if (!it) flowOf(emptyList())
      else bleWapps()
    }
    .flowOn(context)
    .shareIn(scope, SharingStarted.WhileSubscribed(2000), 1)

  private val foundWapps = mutableSetOf<Wapp>()
  private val wappsLock = Mutex()

  val wappState: Flow<WappState>
    get() = wapps
      .flatMapLatest { wapps ->
        if (wapps.isEmpty()) flowOf(WappState(false, null))
        else callbackFlow<ByteArray> {
          wapps.parForEach { wapp ->
            remote.withWapp(wapp.address) {
              messages.collect {
                trySend(it)
              }
            }
          }

          awaitClose()
        }
          .filter {
            it.getOrNull(0)?.toInt() == 83 &&
                it.getOrNull(1)?.toInt() == -112
          }
          .map { message -> WappState(true, message[6] / 100f) }
          .onStart { emit(WappState(true, null)) }
      }
      .distinctUntilChanged()

  @SuppressLint("MissingPermission")
  private fun bleWapps(): Flow<List<Wapp>> = callbackFlow {
    val wapps = mutableSetOf<Wapp>()
    trySend(emptyList())

    fun handleWapp(wapp: Wapp) {
      launch {
        wappsLock.withLock {
          foundWapps += wapp
          if (wapp in wapps)
            return@launch
        }

        remote.withWapp(wapp.address) {
          wappsLock.withLock {
            if (wapp in wapps)
              return@withWapp

            log { "${wapp.debugName()} add wapp" }

            wapps += wapp
            trySend(wapps.toList())
          }

          onCancel {
            if (coroutineContext.isActive) {
              log { "${wapp.debugName()} remove wapp" }
              wappsLock.withLock {
                wapps.remove(wapp)
                trySend(wapps.toList())
              }
            }
          }
        }
      }
    }

    bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
      .filter { it.isWapp() }
      .forEach { handleWapp(it.toWapp()) }

    foundWapps.forEach { handleWapp(it) }

    val callback = object : ScanCallback() {
      override fun onScanResult(callbackType: Int, result: ScanResult) {
        super.onScanResult(callbackType, result)
        if (result.device.isWapp())
          handleWapp(result.device.toWapp())
      }
    }

    log { "start scan" }
    bluetoothManager.adapter.bluetoothLeScanner.startScan(callback)
    awaitClose {
      log { "stop scan" }
      bluetoothManager.adapter.bluetoothLeScanner.stopScan(callback)
    }
  }
}
