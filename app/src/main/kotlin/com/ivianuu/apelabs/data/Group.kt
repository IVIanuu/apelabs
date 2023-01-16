package com.ivianuu.apelabs.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable data class GroupConfig(
  val program: Program = Program(),
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false,
  val blackout: Boolean = false
)

fun List<GroupConfig>.merge(): GroupConfig = when {
  isEmpty() -> GroupConfig()
  size == 1 -> single()
  else -> GroupConfig(
    program = when {
      all { a -> all { a.program == it.program } } -> first().program
      else -> Program()
    },
    brightness = map { it.brightness }.average().toFloat(),
    speed = map { it.speed }.average().toFloat(),
    musicMode = all { it.musicMode },
    blackout = all { it.blackout }
  )
}

val GROUPS = (1..4).toList()

context(ApeLabsPrefsContext) val groupConfigs: Flow<Map<Int, GroupConfig>>
  get() = pref.data
    .map { it.groupConfigs }
    .distinctUntilChanged()

context(ApeLabsPrefsContext) suspend fun updateGroupConfig(group: Int, config: GroupConfig) {
  pref.updateData {
    copy(
      groupConfigs = groupConfigs.toMutableMap()
        .apply { put(group, config) }
    )
  }
}

context(ApeLabsPrefsContext) suspend fun updateGroupConfigs(configs: Map<Int, GroupConfig>) {
  pref.updateData {
    copy(
      groupConfigs = groupConfigs.toMutableMap()
        .apply { putAll(configs) }
    )
  }
}
