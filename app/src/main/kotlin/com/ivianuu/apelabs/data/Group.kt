package com.ivianuu.apelabs.data

import com.ivianuu.essentials.db.AbstractEntityDescriptor
import com.ivianuu.essentials.db.PrimaryKey
import kotlinx.serialization.Serializable

data class GroupConfig(
  val id: String = randomId(),
  val program: Program = Program.RAINBOW,
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false,
  val blackout: Boolean = false
)

@Serializable data class GroupConfigEntity(
  @PrimaryKey val id: String,
  val program: String,
  val brightness: Float,
  val speed: Float,
  val musicMode: Boolean,
  val blackout: Boolean
) {
  companion object : AbstractEntityDescriptor<GroupConfigEntity>("group_configs")
}

fun Collection<GroupConfig>.merge(): GroupConfig = when {
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
