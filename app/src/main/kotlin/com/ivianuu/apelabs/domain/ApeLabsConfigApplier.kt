package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.LightColor
import com.ivianuu.apelabs.data.ProgramConfig
import com.ivianuu.essentials.app.AppForegroundScope
import com.ivianuu.essentials.app.ScopeWorker
import com.ivianuu.essentials.coroutines.combine
import com.ivianuu.essentials.coroutines.onCancel
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

@Provide fun apeLabsConfigApplier(
  lightRepository: LightRepository,
  logger: Logger,
  pref: DataStore<ApeLabsPrefs>,
  previewColor: Flow<@Preview LightColor?>,
  remote: WappRemote,
  wappRepository: WappRepository
) = ScopeWorker<AppForegroundScope> {
  wappRepository.wapps.collectLatest { wapps ->
    if (wapps.isEmpty()) return@collectLatest
    val cache = Cache()

    wapps.parForEach { wapp ->
      remote.withWapp(wapp.address) {
        combine(
          combine(pref.data, previewColor)
            .map { (pref, previewColor) ->
              previewColor?.let {
                pref.groupConfigs.mapValues {
                  if (it.key in pref.selectedGroups)
                    it.value.copy(program = ProgramConfig.SingleColor(previewColor))
                  else it.value
                }
              } ?: pref.groupConfigs
            },
          lightRepository.lights
        )
          .distinctUntilChanged()
          .collectLatest { (groupConfigs, lights) ->
            applyGroupConfig(groupConfigs, lights, cache)
          }
      }
    }
  }
}

@Tag annotation class Preview {
  companion object {
    @Provide val flow = MutableStateFlow<@Preview LightColor?>(null)
  }
}

private class Cache {
  val lastProgram = mutableMapOf<Int, ProgramConfig>()
  val lastBrightness = mutableMapOf<Int, Float>()
  val lastSpeed = mutableMapOf<Int, Float>()
  val lastMusicMode = mutableMapOf<Int, Boolean>()
  val lastLights = mutableListOf<Light>()

  var run = 0
}

private suspend fun WappServer.applyGroupConfig(
  configs: Map<Int, GroupConfig>,
  lights: List<Light>,
  cache: Cache,
  @Inject logger: Logger
) {
  cache.run++

  log { "apply group config $configs $lights" }

  // force a rewrite of groups with changed lights
  lights
    .filter { it !in cache.lastLights }
    .mapTo(mutableSetOf()) { it.group }
    .forEach {
      log { "force rewrite of $it" }
      cache.lastProgram.remove(it)
      cache.lastBrightness.remove(it)
      cache.lastSpeed.remove(it)
      cache.lastMusicMode.remove(it)
    }
  cache.lastLights.clear()
  cache.lastLights.addAll(lights)

  suspend fun <T> writeIfChanged(
    tag: String,
    get: GroupConfig.() -> T,
    cacheField: Cache.() -> MutableMap<Int, T>,
    write: suspend (T, List<Int>) -> Unit
  ) {
    configs
      .toList()
      .groupBy { get(it.second) }
      .mapValues { (value, config) ->
        config
          .map { it.first }
          .filter { cache.cacheField()[it] != value }
      }
      .filterValues { it.isNotEmpty() }
      .toList()
      .sortedByDescending { (_, groups) ->
        groups.sumOf { group -> lights.filter { it.group == group }.size }
      }
      .forEach { (value, groups) ->
        onCancel(
          block = {
            log { "write $tag $value for $groups" }
            write(value, groups)
            groups.forEach { cache.cacheField()[it] = value }
          },
          onCancel = {
            log { "write cancelled $tag for $groups" }
            groups.forEach { cache.cacheField()[it] = value }
          }
        )
      }
  }

  writeIfChanged(
    tag = "program",
    get = { program },
    cacheField = { lastProgram },
    write = { value, groups ->
      when (value) {
        is ProgramConfig.SingleColor -> {
          write(
            byteArrayOf(
              68,
              68,
              groups.toGroupByte(),
              4,
              30,
              value.color.red.toColorByte(),
              value.color.green.toColorByte(),
              value.color.blue.toColorByte(),
              value.color.white.toColorByte()
            )
          )
        }
        is ProgramConfig.MultiColor -> {
          value.items.forEachIndexed { index, item ->
            write(
              byteArrayOf(
                81,
                index.toByte(),
                value.items.size.toByte(),
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
    }
  )

  writeIfChanged(
    tag = "brightness",
    get = { brightness },
    cacheField = { lastBrightness },
    write = { value, groups ->
      write(
        byteArrayOf(
          68,
          68,
          groups.toGroupByte(),
          1,
          (value * 100f).toInt().toByte()
        )
      )
    }
  )

  writeIfChanged(
    tag = "speed",
    get = { speed },
    cacheField = { lastSpeed },
    write = { value, groups ->
      write(
        byteArrayOf(
          68,
          68,
          groups.toGroupByte(),
          2,
          (value * 100f).toInt().toByte()
        )
      )
    }
  )

  writeIfChanged(
    tag = "music mode",
    get = { musicMode },
    cacheField = { lastMusicMode },
    write = { value, groups ->
      write(byteArrayOf(68, 68, groups.toGroupByte(), 3, if (value) 1 else 0, 0))
    }
  )
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

