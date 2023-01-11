package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.ProgramConfig
import com.ivianuu.apelabs.data.debugName
import com.ivianuu.essentials.app.AppForegroundScope
import com.ivianuu.essentials.app.ScopeWorker
import com.ivianuu.essentials.coroutines.combine
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration

@Provide fun apeLabsConfigSynchronizer(
  lightRepository: LightRepository,
  logger: Logger,
  pref: DataStore<ApeLabsPrefs>,
  remote: WappRemote,
  wappRepository: WappRepository
) = ScopeWorker<AppForegroundScope> {
  wappRepository.wapps.collectLatest { wapps ->
    if (wapps.isEmpty()) return@collectLatest
    val lastConfigs = mutableMapOf<Int, GroupConfig>()

    wapps.parForEach {
      remote.withWapp(it.address) {
        combine(pref.data, lightRepository.lights).collectLatest { (prefs) ->
          applyGroupConfig(prefs.groupConfigs, lastConfigs)
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

      if (groups.any { lastConfigs[it]?.program != config.program })
        when (config.program) {
          is ProgramConfig.SingleColor -> {
            write(
              byteArrayOf(
                68,
                68,
                groups.toGroupByte(),
                4,
                30,
                config.program.color.red.toColorByte(),
                config.program.color.green.toColorByte(),
                config.program.color.blue.toColorByte(),
                config.program.color.white.toColorByte()
              )
            )
          }
          is ProgramConfig.MultiColor -> {
            config.program.items.forEachIndexed { index, item ->
              write(
                byteArrayOf(
                  81,
                  index.toByte(),
                  config.program.items.size.toByte(),
                  item.color.red.toColorByte(),
                  item.color.green.toColorByte(),
                  item.color.blue.toColorByte(),
                  item.color.white.toColorByte(),
                  0,
                  item.fade.toDurationByte(),
                  0,
                  item.hold.toDurationByte(),
                  groups.toGroupByte()
                )
              )
            }
          }
          ProgramConfig.Rainbow -> write(
            byteArrayOf(68, 68, groups.toGroupByte(), 4, 29, 0, 0, 0, 0)
          )
        }

      if (groups.any { lastConfigs[it]?.brightness != config.brightness })
        write(
          byteArrayOf(
            68,
            68,
            groups.toGroupByte(),
            1,
            (config.brightness * 100f).toInt().toByte()
          )
        )

      if (groups.any { lastConfigs[it]?.speed != config.speed })
        write(byteArrayOf(68, 68, groups.toGroupByte(), 2, (config.speed * 100f).toInt().toByte()))

      if (groups.any { lastConfigs[it]?.musicMode != config.musicMode })
        write(byteArrayOf(68, 68, groups.toGroupByte(), 3, if (config.musicMode) 1 else 0, 0))

      groups.forEach { lastConfigs[it] = config }
    }
}

private fun Duration.toDurationByte(): Byte = (inWholeMilliseconds / 1000f * 4).toInt().toByte()

private fun Float.toColorByte(): Byte = if (this == 1f) -1 else (this * 255).toInt().toByte()

private fun Int.toGroupByte() = when (this) {
  1 -> 1
  2 -> 2
  3 -> 4
  4 -> 8
  else -> throw AssertionError("Unexpected group $this")
}

private fun List<Int>.toGroupByte(): Byte = map { it.toGroupByte() }.sum().toByte()

