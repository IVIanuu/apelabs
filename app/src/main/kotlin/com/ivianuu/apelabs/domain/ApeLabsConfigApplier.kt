package com.ivianuu.apelabs.domain

import androidx.compose.runtime.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.*
import com.ivianuu.essentials.app.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.logging.*
import com.ivianuu.essentials.ui.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import kotlin.time.*

@Provide fun apeLabsConfigApplier(
  groupConfigRepository: GroupConfigRepository,
  logger: Logger,
  lightRepository: LightRepository,
  previewRepository: PreviewRepository,
  wappRemote: WappRemote,
  wappRepository: WappRepository
) = ScopeComposition<UiScope> {
  val groupLightsVersion = state(GROUPS.associateWith { 0 }) {
    lightRepository.groupLightsChangedEvents
      .map { it.group }
      .collect { changedGroup ->
        value = value.toMutableMap().apply { put(changedGroup, get(changedGroup)!!.inc()) }
      }
  }

  val repositoryConfigs = groupConfigRepository.groupConfigs.state(emptyList())
  val previewsConfigs = previewRepository.previewGroupConfigs()
  val configs = GROUPS
    .associateWith { group ->
      previewsConfigs.singleOrNull { it.id == group.toString() }
        ?: repositoryConfigs.singleOrNull { it.id == group.toString() }
    }

  wappRepository.wapps.state(null).forEach { wapp ->
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
              !currentComposer.changed(groupLightsVersion[group])
            ) null
            else group to value
          }
        }

        val writeLock = remember { Mutex() }
        if (dirtyGroups.isNotEmpty())
          LaunchedEffect(dirtyGroups) {
            wappRemote.withWapp(wapp.address) {
              dirtyGroups
                .groupBy { it.second }
                .forEach { (value, groups) ->
                  logger.d { "apply $tag $value for $groups" }
                  writeLock.withLock {
                    apply(this, value, groups.map { it.first })
                  }
                }
            }
          }
      }

      LightConfiguration(
        tag = "program",
        get = {
          // erase ids here to make caching work correctly
          // there could be the same program just with different ids
          program.copy(
            id = if (program == Program.RAINBOW) program.id else "",
            items = program.items
              .map { it.copy(color = it.color.copy(id = "")) }
          )
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

      LightConfiguration(tag = "strobe", get = {
        !blackout && mode == GroupConfig.Mode.STROBE
      }) { value, groups ->
        write(byteArrayOf(68, 68, groups.toGroupByte(), 5, if (value) 1 else 0, 0))
      }

      LightConfiguration(
        tag = "brightness",
        get = { if (blackout) 0f else brightness }
      ) { value, groups ->
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
