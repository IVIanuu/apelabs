package com.ivianuu.apelabs.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ivianuu.apelabs.data.Wapp
import com.ivianuu.apelabs.data.WappState
import com.ivianuu.apelabs.data.debugName
import com.ivianuu.apelabs.data.isWapp
import com.ivianuu.apelabs.data.toWapp
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.SystemService
import com.ivianuu.essentials.compose.compositionStateFlow
import com.ivianuu.essentials.coroutines.ScopedCoroutineScope
import com.ivianuu.essentials.coroutines.guarantee
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.permission.PermissionManager
import com.ivianuu.essentials.ui.UiScope
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

@Provide @Scoped<UiScope> class WappRepository(
  private val bluetoothManager: @SystemService BluetoothManager,
  private val logger: Logger,
  permissionManager: PermissionManager,
  scope: ScopedCoroutineScope<UiScope>,
  private val wappRemote: WappRemote
) {
  @SuppressLint("MissingPermission")
  val wapps: StateFlow<List<Wapp>> = scope.compositionStateFlow {
    if (!remember { permissionManager.permissionState(apeLabsPermissionKeys) }.collectAsState(false).value)
      return@compositionStateFlow emptyList()

    var wapps by remember { mutableStateOf(emptySet<Wapp>()) }

    wapps.forEach { wapp ->
      key(wapp.address) {
        LaunchedEffect(true) {
          guarantee(
            block = {
              wappRemote.withWapp<Unit>(wapp.address, connectTimeout = 30.seconds) {
                awaitCancellation()
              } ?: run { knownWapps -= wapp }
            },
            finalizer = {
              logger.log { "${wapp.debugName()} remove wapp" }
              wapps = wapps - wapp
            }
          )
        }
      }
    }

    DisposableEffect(true) {
      wapps = wapps + bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        .filter { it.isWapp() }
        .map { it.toWapp() } + knownWapps

      val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
          super.onScanResult(callbackType, result)
          if (result.device.isWapp()) {
            val wapp = result.device.toWapp()
            wapps = wapps + wapp
            knownWapps += wapp
          }
        }
      }

      logger.log { "start scan" }
      bluetoothManager.adapter.bluetoothLeScanner.startScan(
        emptyList(),
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
        callback
      )
      onDispose {
        logger.log { "stop scan" }
        bluetoothManager.adapter.bluetoothLeScanner.stopScan(callback)
      }
    }

    remember(wapps) {
      logger.log { "wapps changed ${wapps.map { it.debugName() }}" }
    }

    wapps.toList()
  }

  val wappState: StateFlow<WappState> = scope.compositionStateFlow {
    val wapps by wapps.collectAsState()
    if (wapps.isEmpty()) return@compositionStateFlow WappState()

    produceState(WappState()) {
      channelFlow {
        wapps.parForEach { wapp ->
          wappRemote.withWapp<Unit>(wapp.address) {
            trySend(wapp to byteArrayOf())

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
        .collect { value = it }
    }.value
  }

  companion object {
    private val knownWapps = mutableSetOf<Wapp>()
  }
}
