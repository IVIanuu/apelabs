package com.ivianuu.apelabs.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.ivianuu.apelabs.data.Wapp
import com.ivianuu.apelabs.data.WappState
import com.ivianuu.apelabs.data.debugName
import com.ivianuu.apelabs.data.isWapp
import com.ivianuu.apelabs.data.toWapp
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.onCancel
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.permission.PermissionManager
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.android.SystemService
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.compose.compositionStateFlow
import com.ivianuu.essentials.coroutines.ScopedCoroutineScope
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Provide @Scoped<AppScope> class WappRepository(
  private val bluetoothManager: @SystemService BluetoothManager,
  private val logger: Logger,
  permissionManager: PermissionManager,
  scope: ScopedCoroutineScope<AppScope>,
  private val wappRemote: WappRemote
) : SynchronizedObject() {
  private val foundWapps = mutableSetOf<Wapp>()

  @SuppressLint("MissingPermission")
  val wapps: StateFlow<List<Wapp>> = scope.compositionStateFlow {
    if (!permissionManager.permissionState(apeLabsPermissionKeys).collectAsState(false).value)
      return@compositionStateFlow emptyList()

    var wapps by remember {
      mutableStateOf(
        buildSet<Wapp> {
          this += bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            .filter { it.isWapp() }
            .map { it.toWapp() }

          this += synchronized(this) { foundWapps.toList() }
        }
      )
    }

    remember(wapps) { foundWapps += wapps }

    wapps.forEach { wapp ->
      key(wapp.address) {
        LaunchedEffect(true) {
          wappRemote.withWapp<Unit>(wapp.address) {
            onCancel {
              logger.log { "${wapp.debugName()} remove wapp" }
              wapps = wapps - wapp
            }
          }
        }
      }
    }

    DisposableEffect(true) {
      val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
          super.onScanResult(callbackType, result)
          if (result.device.isWapp())
            wapps = wapps + result.device.toWapp()
        }
      }

      logger.log { "start scan" }
      bluetoothManager.adapter.bluetoothLeScanner.startScan(callback)
      onDispose {
        logger.log { "stop scan" }
        bluetoothManager.adapter.bluetoothLeScanner.stopScan(callback)
      }
    }

    wapps.toList()
  }

  val wappState: StateFlow<WappState> = scope.compositionStateFlow {
    val wapps by wapps.collectAsState()
    if (wapps.isEmpty()) return@compositionStateFlow WappState()

    produceState(WappState()) {
      callbackFlow {
        wapps.parForEach { wapp ->
          wappRemote.withWapp<Unit>(wapp.address) {
            messages.collect {
              trySend(wapp to it)
            }
          }
        }
        awaitClose()
      }
        .filter {
          it.second.getOrNull(0)?.toInt() == 83 &&
              it.second.getOrNull(1)?.toInt() == -112
        }
        .map { (wapp, message) ->
          WappState(
            wapp.id,
            true,
            message.getOrNull(6)?.let { it / 100f })
        }
        .collect { value = it }
    }.value
  }
}
