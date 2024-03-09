package com.ivianuu.apelabs.data

import com.ivianuu.essentials.db.*
import kotlinx.serialization.*

data class GroupConfig(
  val id: String = randomId(),
  val program: Program = Program.RAINBOW,
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val mode: Mode = Mode.FADE,
  val blackout: Boolean = false,
) {
  enum class Mode { FADE, MUSIC, STROBE }
}

@Serializable @Entity data class GroupConfigEntity(
  @PrimaryKey val id: String,
  val program: String,
  val brightness: Float,
  val speed: Float,
  val musicMode: Boolean,
  val strobe: Boolean,
  val blackout: Boolean
)

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
    mode = if (map { it.mode }.distinct().size == 1) first().mode else GroupConfig.Mode.FADE,
    blackout = all { it.blackout }
  )
}

val GROUPS = (1..4).toList()
