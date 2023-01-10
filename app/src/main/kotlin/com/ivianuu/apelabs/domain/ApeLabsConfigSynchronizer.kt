package com.ivianuu.apelabs.domain

import androidx.compose.ui.graphics.Color
import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.debugName
import com.ivianuu.essentials.app.AppForegroundScope
import com.ivianuu.essentials.app.ScopeWorker
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration

@Provide fun apeLabsConfigSynchronizer(
  logger: Logger,
  pref: DataStore<ApeLabsPrefs>,
  remote: WappRemote,
  repository: WappRepository
) = ScopeWorker<AppForegroundScope> {
  repository.wapps.collectLatest { wapps ->
    if (wapps.isEmpty()) return@collectLatest

    wapps.parForEach {
      remote.withWapp(it.address) {
        val lastConfigs = mutableMapOf<Int, GroupConfig>()
        pref.data.collectLatest {
          applyGroupConfig(it.groupConfigs, lastConfigs)
        }
      }
    }
  }
}

private suspend fun WappServer.applyGroupConfig(
  configs: Map<Int, GroupConfig>,
  lastConfigs: MutableMap<Int, GroupConfig>,
  @Inject logger: Logger
) {
  configs
    .toList()
    .groupBy { it.second }
    .mapValues { it.value.map { it.first } }
    .filter { (config, groups) ->
      groups.any { lastConfigs[it] != config }
    }
    .forEach { (config, groups) ->
      log { "${device.debugName()} -> apply config $config to $groups" }
      write(singleColorModeBytes(groups, Color.White))
      write(brightnessBytes(groups, config.brightness))
      write(speedBytes(groups, config.speed))
      write(musicModeBytes(groups, config.musicMode))
      groups.forEach { lastConfigs[it] = config }
    }
}

fun singleColorModeBytes(
  groups: List<Int>,
  color: Color
) = byteArrayOf(
  68,
  68,
  groups.toGroupByte(),
  4,
  30,
  color.red.toColorByte(),
  color.green.toColorByte(),
  color.blue.toColorByte(),
  0 // white
)

fun rainbowModeBytes(groups: List<Int>) =
  byteArrayOf(68, 68, groups.toGroupByte(), 4, 29, 0, 0, 0, 0)

fun programColorBytes(
  position: Int,
  programColorsSize: Int,
  color: Color,
  fade: Duration,
  hold: Duration,
  groups: List<Int>
) = byteArrayOf(
  81,
  position.toByte(),
  programColorsSize.toByte(),
  color.red.toColorByte(),
  color.green.toColorByte(),
  color.blue.toColorByte(),
  0,
  0,
  fade.toDurationByte(),
  0,
  hold.toDurationByte(),
  groups.toGroupByte()
)

private fun Duration.toDurationByte(): Byte = 4

private fun Float.toColorByte(): Byte = if (this == 1f) -1 else (this * 255).toInt().toByte()

fun speedBytes(
  groups: List<Int>,
  speed: Float
) = byteArrayOf(68, 68, groups.toGroupByte(), 2, (speed * 100f).toInt().toByte())

fun brightnessBytes(
  groups: List<Int>,
  brightness: Float
) = byteArrayOf(68, 68, groups.toGroupByte(), 1, (brightness * 100f).toInt().toByte())

fun musicModeBytes(
  groups: List<Int>,
  enabled: Boolean
) = byteArrayOf(68, 68, groups.toGroupByte(), 3, if (enabled) 1 else 0, 0)

fun blinkDeviceBytes(id1: Int, id2: Int) =
  byteArrayOf(72, 10, id1.toByte(), id2.toByte(), 0)

fun regroupDeviceBytes(id1: Int, id2: Int, group: Int) =
  byteArrayOf(82, 10, id1.toByte(), id2.toByte(), 0, group.toByte(), 1, 0, 2, 13, 10)

val refreshBytes = byteArrayOf(88)

fun List<Int>.toGroupByte(): Byte = when {
  this == listOf(1) -> 1
  this == listOf(2) -> 2
  this == listOf(3) -> 4
  this == listOf(4) -> 8
  this == listOf(1, 2) -> 3
  this == listOf(1, 3) -> 5
  this == listOf(1, 4) -> 9
  this == listOf(2, 3) -> 6
  this == listOf(2, 4) -> 10
  this == listOf(3, 4) -> 12
  this == listOf(1, 2, 3) -> 7
  this == listOf(1, 2, 4) -> 11
  this == listOf(1, 3, 4) -> 13
  this == listOf(2, 3, 4) -> 14
  this == listOf(1, 2, 3, 4) -> 15
  else -> throw AssertionError("unexpected group combination $this")
}
