package com.ivianuu.apelabs.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.ivianuu.apelabs.data.Wapp
import com.ivianuu.apelabs.data.debugName
import com.ivianuu.apelabs.data.isWapp
import com.ivianuu.apelabs.data.toWapp
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.onCancel
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.permission.PermissionStateFactory
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.android.SystemService
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Provide @Scoped<AppScope> class WappRepository(
  private val bluetoothManager: @SystemService BluetoothManager,
  private val context: IOContext,
  private val logger: Logger,
  permissionStateFactory: PermissionStateFactory,
  private val remote: WappRemote,
  scope: NamedCoroutineScope<AppScope>
) {
  val wapps: Flow<List<Wapp>> = permissionStateFactory(apeLabsPermissionKeys)
    .flatMapLatest {
      if (!it) flowOf(emptyList())
      else bleWapps()
    }
    .flowOn(context)
    .shareIn(scope, SharingStarted.WhileSubscribed(2000), 1)

  private val foundWapps = mutableSetOf<Wapp>()
  private val wappsLock = Mutex()

  @SuppressLint("MissingPermission")
  private fun bleWapps(): Flow<List<Wapp>> = callbackFlow {
    val wapps = mutableListOf<Wapp>()
    trySend(emptyList())

    fun handleWapp(wapp: Wapp) {
      launch {
        wappsLock.withLock {
          foundWapps += wapp
          if (wapps.any { it.address == wapp.address })
            return@launch
        }

        remote.withWapp<Unit>(wapp.address) {
          log { "${wapp.debugName()} add wapp" }
          wappsLock.withLock {
            wapps += wapp
            trySend(wapps.toList())
          }

          onCancel {
            if (coroutineContext.isActive) {
              log { "${wapp.debugName()} remove wapp" }
              wappsLock.withLock {
                wapps.removeAll { it.address == wapp.address }
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
