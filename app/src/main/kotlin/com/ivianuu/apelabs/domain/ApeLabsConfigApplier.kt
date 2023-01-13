package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Program
import com.ivianuu.essentials.app.AppForegroundScope
import com.ivianuu.essentials.app.ScopeWorker
import com.ivianuu.essentials.coroutines.combine
import com.ivianuu.essentials.coroutines.onCancel
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.lerp
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration

context(ApeLabsPrefsContext, Logger, LightRepository, PreviewRepository, WappRemote, WappRepository)
    @Provide fun apeLabsConfigApplier() = ScopeWorker<AppForegroundScope> {
  wapps.collectLatest { wapps ->
    if (wapps.isEmpty()) return@collectLatest
    val cache = Cache()

    wapps.parForEach { wapp ->
      withWapp(wapp.address) {
        combine(pref.data, preview)
          .map { (pref, previewProgram) ->
            previewProgram?.let {
              pref.groupConfigs.mapValues {
                if (it.key in pref.selectedGroups)
                  it.value.copy(program = previewProgram)
                else it.value
              }
            } ?: pref.groupConfigs
          }
          .distinctUntilChanged()
          .flatMapLatest { groupConfigs ->
            groupLightsChangedEvents
              .onEach { changedGroup ->
                log { "force reapply for $changedGroup" }
                cache.lastProgram.remove(changedGroup)
                cache.lastBrightness.remove(changedGroup)
                cache.lastSpeed.remove(changedGroup)
                cache.lastMusicMode.remove(changedGroup)
              }
              .onStart<Any?> { emit(Unit) }
              .map { groupConfigs }
          }
          .collectLatest { groupConfigs ->
            log { "values $groupConfigs " }
            applyGroupConfig(groupConfigs, cache)
          }
      }
    }
  }
}

private class Cache {
  val lastProgram = mutableMapOf<Int, Program>()
  val lastBrightness = mutableMapOf<Int, Float>()
  val lastSpeed = mutableMapOf<Int, Float>()
  val lastMusicMode = mutableMapOf<Int, Boolean>()
}

context(Logger, WappServer) private suspend fun applyGroupConfig(
  configs: Map<Int, GroupConfig>,
  cache: Cache
) {
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
    get = { if (blackout) 0f else brightness },
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

private fun Float.toColorByte(): Byte = lerp(0, 255, this)
  .let { if (it > 127) it - 256 else it }
  .toByte()

private fun Int.toGroupByte() = when (this) {
  1 -> 1
  2 -> 2
  3 -> 4
  4 -> 8
  else -> throw AssertionError("Unexpected group $this")
}

private fun List<Int>.toGroupByte(): Byte = map { it.toGroupByte() }.sum().toByte()
