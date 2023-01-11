package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.Program
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
  previewColor: Flow<@Preview Program?>,
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
            .map { (pref, previewProgram) ->
              previewProgram?.let {
                pref.groupConfigs.mapValues {
                  if (it.key in pref.selectedGroups)
                    it.value.copy(program = previewProgram)
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
    @Provide val flow = MutableStateFlow<@Preview Program?>(null)
  }
}

private class Cache {
  val lastProgram = mutableMapOf<Int, Program>()
  val lastBrightness = mutableMapOf<Int, Float>()
  val lastSpeed = mutableMapOf<Int, Float>()
  val lastMusicMode = mutableMapOf<Int, Boolean>()
  val lastLights = mutableListOf<Light>()
}

private suspend fun WappServer.applyGroupConfig(
  configs: Map<Int, GroupConfig>,
  lights: List<Light>,
  cache: Cache,
  @Inject logger: Logger
) {
  // force a reapply of groups with changed lights
  lights
    .filter { it !in cache.lastLights }
    .mapTo(mutableSetOf()) { it.group }
    .forEach {
      log { "force reapply of $it" }
      cache.lastProgram.remove(it)
      cache.lastBrightness.remove(it)
      cache.lastSpeed.remove(it)
      cache.lastMusicMode.remove(it)
    }
  cache.lastLights.clear()
  cache.lastLights.addAll(lights)

  suspend fun <T> applyIfChanged(
    tag: String,
    get: GroupConfig.() -> T,
    cacheField: Cache.() -> MutableMap<Int, T>,
    apply: suspend (T, List<Int>) -> Unit
  ) {
    configs
      .toList()
      .groupBy { get(it.second) }
      // filter out changed groups
      .mapValues { (value, config) ->
        config
          .map { it.first }
          .filter { cache.cacheField()[it] != value }
      }
      // only apply if there any groups
      .filterValues { it.isNotEmpty() }
      .toList()
      // apply the group with most lights first
      .sortedByDescending { (_, groups) ->
        groups.sumOf { group -> lights.filter { it.group == group }.size }
      }
      .forEach { (value, groups) ->
        onCancel(
          block = {
            // cache and apply output
            log { "apply $tag $value for $groups" }
            apply(value, groups)
            groups.forEach { cache.cacheField()[it] = value }
          },
          onCancel = {
            // invalidate cache
            log { "apply cancelled $tag for $groups" }
            groups.forEach { cache.cacheField()[it] = value }
          }
        )
      }
  }

  applyIfChanged(
    tag = "program",
    get = { program },
    cacheField = { lastProgram },
    apply = { value, groups ->
      when (value) {
        is Program.SingleColor -> {
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
        is Program.MultiColor -> {
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
        Program.Rainbow -> write(
          byteArrayOf(68, 68, groups.toGroupByte(), 4, 29, 0, 0, 0, 0)
        )
      }
    }
  )

  applyIfChanged(
    tag = "brightness",
    get = { brightness },
    cacheField = { lastBrightness },
    apply = { value, groups ->
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

  applyIfChanged(
    tag = "speed",
    get = { speed },
    cacheField = { lastSpeed },
    apply = { value, groups ->
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

  applyIfChanged(
    tag = "music mode",
    get = { musicMode },
    cacheField = { lastMusicMode },
    apply = { value, groups ->
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

