package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Program
import com.ivianuu.essentials.app.AppForegroundScope
import com.ivianuu.essentials.app.ScopeWorker
import com.ivianuu.essentials.coroutines.combine
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.lerp
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.time.milliseconds
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlin.time.Duration

context(ApeLabsPrefsContext, GroupConfigRepository, Logger, LightRepository,
PreviewRepository, WappRemote, WappRepository)
    @Provide
fun apeLabsConfigApplier() = ScopeWorker<AppForegroundScope> {
  wapps.collectLatest { wapps ->
    log { "wapps $wapps" }
    if (wapps.isEmpty()) return@collectLatest

    wapps.parForEach { wapp ->
      val cache = Cache()

      withWapp(wapp.address) {
        log { "apply for wapp $wapp" }

        combine(groupConfigs, previewGroupConfigs)
          .map { (groupConfigs, previewGroupConfigs) ->
            GROUPS
              .associateWith { group ->
                previewGroupConfigs.singleOrNull { it.id == group.toString() }
                  ?: groupConfigs.single { it.id == group.toString() }
              }
          }
          .distinctUntilChanged()
          .flatMapLatest { groupConfigs ->
            groupLightsChangedEvents
              .map { it.group }
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
          .collectLatest { applyGroupConfig(it, cache) }
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
  log { "apply $configs" }

  val appliers = mutableMapOf<List<Int>, MutableList<suspend () -> Unit>>()

  suspend fun <T> applyIfChanged(
    tag: String,
    get: GroupConfig.() -> T,
    cache: MutableMap<Int, T>,
    apply: suspend (T, List<Int>) -> Unit
  ) {
    configs
      .toList()
      .groupBy { get(it.second) }
      // filter out changed groups
      .mapValues { (value, config) ->
        config
          .map { it.first }
          .filter { cache[it] != value }
      }
      // only apply if there any groups
      .filterValues { it.isNotEmpty() }
      .forEach { (value, groups) ->
        appliers.getOrPut(groups) { mutableListOf() } += {
          // cache and apply output
          log { "apply $tag $value for $groups" }
          apply(value, groups)
          groups.forEach { cache[it] = value }
        }
      }
  }

  applyIfChanged(
    tag = "program",
    get = {
      // erase ids here to make caching work correctly
      // there could be the same program just with different ids
      if (program == Program.RAINBOW) program
      else program.copy(
        id = "",
        items = program.items
          .map { it.copy(color = it.color.copy(id = "")) }
      )
    },
    cache = cache.lastProgram,
    apply = { value, groups ->
      when {
        value.id == Program.RAINBOW.id -> {
          write(byteArrayOf(68, 68, groups.toGroupByte(), 4, 29, 0, 0, 0, 0))
        }

        value.items.size == 1 -> {
          val color = value.items.single().color
          write(
            byteArrayOf(
              68,
              68,
              groups.toGroupByte(),
              4,
              30,
              color.red.toColorByte(),
              color.green.toColorByte(),
              color.blue.toColorByte(),
              color.white.toColorByte()
            )
          )
        }
        else -> {
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
                item.fadeTime.toDurationByte(),
                0,
                item.holdTime.toDurationByte(),
                groups.toGroupByte()
              )
            )
          }
        }
      }
    }
  )

  applyIfChanged(
    tag = "speed",
    get = { speed },
    cache = cache.lastSpeed,
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
    cache = cache.lastMusicMode,
    apply = { value, groups ->
      write(byteArrayOf(68, 68, groups.toGroupByte(), 3, if (value) 1 else 0, 0))
    }
  )

  applyIfChanged(
    tag = "brightness",
    get = { if (blackout) 0f else brightness },
    cache = cache.lastBrightness,
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

  appliers
    .forEach { (_, appliers) ->
      appliers.forEach {
        withContext(NonCancellable) { it() }
      }
      delay(150.milliseconds)
    }
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

fun List<Int>.toGroupByte(): Byte = map { it.toGroupByte() }.sum().toByte()

fun Int.toByteArray() = byteArrayOf(
  (this shr 0).toByte(),
  (this shr 8).toByte(),
  (this shr 16).toByte(),
  (this shr 24).toByte()
)

fun ByteArray.toInt() = (this[3].toInt() shl 24) or
    (this[2].toInt() and 0xff shl 16) or
    (this[1].toInt() and 0xff shl 8) or
    (this[0].toInt() and 0xff)
