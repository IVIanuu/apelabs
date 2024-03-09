package com.ivianuu.apelabs.domain

import android.annotation.*
import android.bluetooth.*
import android.bluetooth.le.*
import androidx.compose.runtime.*
import arrow.fx.coroutines.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.coroutines.*
import com.ivianuu.essentials.data.*
import com.ivianuu.essentials.logging.*
import com.ivianuu.essentials.permission.*
import com.ivianuu.essentials.ui.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

@Provide @Eager<UiScope> class WappRepository(
  private val bluetoothManager: @SystemService BluetoothManager,
  private val logger: Logger,
  private val pref: DataStore<ApeLabsPrefs>,
  permissionManager: PermissionManager,
  scope: ScopedCoroutineScope<UiScope>,
  private val wappRemote: WappRemote
) {
  @SuppressLint("MissingPermission")
  val wapps: StateFlow<List<Wapp>> = scope.moleculeStateFlow {
    if (!permissionManager.permissionState(apeLabsPermissionKeys).state(false))
      return@moleculeStateFlow emptyList()

    var wapps by remember { mutableStateOf(emptySet<Wapp>()) }

    wapps.forEach { wapp ->
      key(wapp.address) {
        LaunchedEffect(true) {
          guarantee(
            fa = {
              wappRemote.withWapp<Unit>(wapp.address, connectTimeout = 30.seconds) {
                awaitCancellation()
              } ?: run {
                pref.updateData { copy(knownWapps = knownWapps - wapp) }
              }
            },
            finalizer = {
              logger.d { "${wapp.debugName()} remove wapp" }
              wapps -= wapp
            }
          )
        }
      }
    }

    LaunchedEffect(true) {
      wapps += bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        .filter { it.isWapp() }
        .map { it.toWapp() } + pref.data.first().knownWapps

      val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
          super.onScanResult(callbackType, result)
          if (result.device.isWapp()) {
            val wapp = result.device.toWapp()
            wapps += wapp
            launch { pref.updateData { copy(knownWapps = knownWapps + wapp) } }
          }
        }
      }

      logger.d { "start scan" }
      bluetoothManager.adapter.bluetoothLeScanner.startScan(
        emptyList(),
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
        callback
      )
      onCancel {
        logger.d { "stop scan" }
        bluetoothManager.adapter.bluetoothLeScanner.stopScan(callback)
      }
    }

    remember(wapps) {
      logger.d { "wapps changed ${wapps.map { it.debugName() }}" }
    }

    wapps.toList()
  }

  val wappState: StateFlow<WappState> = scope.moleculeStateFlow {
    val wapps = wapps.state()
    if (wapps.isEmpty()) return@moleculeStateFlow WappState()

    state(WappState(), wapps) {
      channelFlow {
        wapps.parMap { wapp ->
          wappRemote.withWapp<Unit>(wapp.address) {
            trySend(wapp to byteArrayOf())
            messages.collect { trySend(wapp to it) }
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
    }
  }
}
