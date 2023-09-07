package com.ivianuu.apelabs.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.toApeColor
import com.ivianuu.essentials.app.AppVisibleScope
import com.ivianuu.essentials.app.ScopeComposition
import com.ivianuu.essentials.lerp
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.time.Duration

@Provide fun apeLabsConfigApplier(
  groupConfigRepository: GroupConfigRepository,
  logger: Logger,
  lightRepository: LightRepository,
  previewRepository: PreviewRepository,
  wappRemote: WappRemote,
  wappRepository: WappRepository
) = ScopeComposition<AppVisibleScope> {
  val groupLightsState by produceState(GROUPS.associateWith { 0 }) {
    lightRepository.groupLightsChangedEvents
      .map { it.group }
      .collect { changedGroup ->
        value = value.toMutableMap().apply { put(changedGroup, get(changedGroup)!!.inc()) }
      }
  }

  val configs = remember {
    combine(
      groupConfigRepository.groupConfigs,
      previewRepository.previewGroupConfigs
    ) { groupConfigs, previewGroupConfigs ->
      GROUPS
        .associateWith { group ->
          previewGroupConfigs.singleOrNull { it.id == group.toString() }
            ?: groupConfigs.singleOrNull { it.id == group.toString() }
            ?: return@combine null
        }
    }
      .filterNotNull()
      .distinctUntilChanged()
  }.collectAsState(null).value ?: return@ScopeComposition

  wappRepository.wapps.collectAsState(null).value?.forEach { wapp ->
    key(wapp) {
      @Composable fun <T> LightConfiguration(
        tag: String,
        get: GroupConfig.() -> T,
        apply: suspend WappServer.(T, List<Int>) -> Unit,
      ) {
        val valueByGroup = GROUPS.associateWith { configs[it]!!.get() }

        val dirtyGroups = GROUPS.mapNotNull { group ->
          key(group) {
            val value = valueByGroup[group]!!
            if (!currentComposer.changed(value) and
              !currentComposer.changed(groupLightsState[group])
            ) null
            else group to value
          }
        }

        if (dirtyGroups.isNotEmpty())
          LaunchedEffect(dirtyGroups) {
            dirtyGroups
              .groupBy { it.second }
              .forEach { (value, groups) ->
                logger.log { "apply $tag $value for $groups" }
                wappRemote.withWapp(wapp.address) {
                  apply(this, value, groups.map { it.first })
                }
              }
          }
      }

      LightConfiguration(
        tag = "program",
        get = {
          // erase ids here to make caching work correctly
          // there could be the same program just with different ids
          if (mode != GroupConfig.Mode.STROBE) {
            program.copy(
              id = if (program == Program.RAINBOW) program.id else "",
              items = program.items
                .map { it.copy(color = it.color.copy(id = "")) }
            )
          } else {
            Program(
              id = if (program == Program.RAINBOW) program.id else "",
              items = (0..max(
                program.items.size,
                (when {
                  speed == 0f -> 0
                  speed <= 0.33f -> 3
                  speed <= 0.66f -> 2
                  else -> 1
                })
              )).map {
                Program.Item(
                  color = program.items.getOrNull(it)?.color?.copy(id = "")
                    ?: Color.Transparent.toApeColor(""),
                  fadeTime = Duration.ZERO,
                  holdTime = Duration.ZERO
                )
              }
            )
          }
        }
      ) { value, groups ->
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

      LightConfiguration(tag = "speed", get = { speed }) { value, groups ->
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

      LightConfiguration(tag = "music mode", get = { mode == GroupConfig.Mode.MUSIC }) { value, groups ->
        write(byteArrayOf(68, 68, groups.toGroupByte(), 3, if (value) 1 else 0, 0))
      }

      LightConfiguration(
        tag = "brightness",
        get = { if (blackout) 0f else brightness }) { value, groups ->
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
    }
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
