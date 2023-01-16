package com.ivianuu.apelabs.data

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.time.seconds
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable data class ApeColor(
  val red: Float = 0f,
  val green: Float = 0f,
  val blue: Float = 0f,
  val white: Float = 0f
)

fun ApeColor.toComposeColor() = Color(red, green, blue)
  .overlay(Color.White.copy(alpha = white))

fun Color.toApeColor() = ApeColor(red, green, blue)

private fun Color.overlay(overlay: Color): Color {
  val alphaSum = alpha + overlay.alpha
  return Color(
    (red * alpha + overlay.red * overlay.alpha) / alphaSum,
    (green * alpha + overlay.green * overlay.alpha) / alphaSum,
    (blue * alpha + overlay.blue * overlay.alpha) / alphaSum,
    alphaSum.coerceIn(0f, 1f),
  )
}

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

@Serializable data class Program(val items: List<Item> = listOf(Item())) {
  init {
    check(items.size in ITEM_RANGE)
  }

  @Serializable data class Item(
    val color: ApeColor = ApeColor(white = 1f),
    val fadeTime: Duration = 1.seconds,
    val holdTime: Duration = 1.seconds
  )

  companion object {
    val ITEM_RANGE = 1..4
    val RAINBOW = Program(
      items = listOf(
        Item(Color.Red.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Yellow.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Green.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Blue.toApeColor(), Duration.INFINITE, Duration.INFINITE)
      )
    )
  }
}

fun ApeColor.asProgram() = Program(items = listOf(Program.Item(this)))

@Serializable data class Scene(
  val groupConfigs: Map<Int, GroupConfig?> = GROUPS.associateWith { null }
) {
  init {
    GROUPS.forEach {
      check(it in groupConfigs)
    }
  }
}
