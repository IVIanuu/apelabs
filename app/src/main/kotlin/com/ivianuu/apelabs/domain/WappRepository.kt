package com.ivianuu.apelabs.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.ivianuu.apelabs.data.Wapp
import com.ivianuu.apelabs.data.WappState
import com.ivianuu.apelabs.data.debugName
import com.ivianuu.apelabs.data.isWapp
import com.ivianuu.apelabs.data.toWapp
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.onCancel
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.coroutines.share
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.permission.PermissionManager
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.android.SystemService
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.awaitCancellation
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

context(BluetoothManager, Logger, NamedCoroutineScope<AppScope>, PermissionManager, WappRemote)
@Provide @Scoped<AppScope> class WappRepository(context: IOContext) {
  val wapps: Flow<List<Wapp>> = permissionState(apeLabsPermissionKeys)
    .flatMapLatest {
      if (!it) flowOf(emptyList())
      else bleWapps()
    }
    .flowOn(context)
    .share(SharingStarted.WhileSubscribed(2000), 1)
    .distinctUntilChanged()

  private val foundWapps = mutableSetOf<Wapp>()
  private val wappsLock = Mutex()

  val wappState: Flow<WappState>
    get() = wapps
      .flatMapLatest { wapps ->
        if (wapps.isEmpty()) flowOf(WappState(null, false, null))
        else callbackFlow<Pair<Wapp, ByteArray>> {
          wapps.parForEach { wapp ->
            trySend(wapp to byteArrayOf())

            withWapp(wapp.address) {
              messages.collect {
                trySend(wapp to it)
              }
            }
          }
          awaitClose()
        }
          .filter {
            it.second.isEmpty() ||
                (it.second.getOrNull(0)?.toInt() == 83 &&
                    it.second.getOrNull(1)?.toInt() == -112)
          }
          .map { (wapp, message) ->
            WappState(
              wapp.id,
              true,
              message.getOrNull(6)?.let { it / 100f })
          }
          .onStart { emit(WappState(null, true, null)) }
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

        withWapp(wapp.address) {
          wappsLock.withLock {
            if (wapp in wapps)
              return@withWapp

            log { "${wapp.debugName()} add wapp" }

            wapps += wapp
            trySend(wapps.toList())
          }

          onCancel(block = { awaitCancellation() }) {
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

    getConnectedDevices(BluetoothProfile.GATT)
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
    adapter.bluetoothLeScanner.startScan(callback)
    awaitClose {
      log { "stop scan" }
      adapter.bluetoothLeScanner.stopScan(callback)
    }
  }
}
