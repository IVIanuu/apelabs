package com.ivianuu.apelabs.data

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.db.Entity
import com.ivianuu.essentials.db.PrimaryKey
import kotlinx.serialization.Serializable


@Serializable @Entity data class ApeColor(
  @PrimaryKey val id: String = randomId(),
  val red: Float = 0f,
  val green: Float = 0f,
  val blue: Float = 0f,
  val white: Float = 0f
)

fun ApeColor.toComposeColor(): Color {
  val red = red * 255f
  val blue = blue * 255f
  val green = green * 255f
  val white = white * 255f

  if (red == 0f && green == 0f && blue == 0f && white == 0f) return Color.Transparent

  val r2 = red + (255 - red) * 0.33 * white / 255
  val g2 = green + (255 - green) * 0.33 * white / 255
  val b2 = blue + (255 - blue) * 0.33 * white / 255

  val min = (255 / r2).coerceAtMost((255 / g2).coerceAtMost(255 / b2))
  val r3 = (r2 * min).toInt().coerceIn(0, 255)
  val g3 = (g2 * min).toInt().coerceIn(0, 255)
  val b3 = (min * b2).toInt().coerceIn(0, 255)

  return Color(r3, g3, b3)
}

fun Color.toApeColor(id: String = randomId()) = ApeColor(id, red, green, blue)

val BuiltInColors = listOf(
  ApeColor("Candlelight Classic", 0.72f, 0.19f, 0.00f, 1.00f),
  ApeColor("Candlelight Rustic", 0.76f, 0.28f, 0.00f, 1.00f),
  ApeColor("Soft Amber", 0.53f, 0.16f, 0.00f, 1.00f),
  ApeColor("Blush Pink", 0.46f, 0.00f, 0.19f, 1.00f),
  ApeColor("Lilac", 0.25f, 0.00f, 0.98f, 1.00f),
  ApeColor("Seafoam", 0.07f, 0.38f, 0.00f, 1.00f),
  ApeColor("Mint", 0.09f, 0.62f, 0.00f, 1.00f),
  ApeColor("Pastel Blue", 0.00f, 0.58f, 1.00f, 0.20f),
  ApeColor("Turquoise", 0.00f, 1.00f, 0.42f, 0.00f),
  ApeColor("Red", 1.00f, 0.00f, 0.00f, 0.00f),
  ApeColor("Green", 0.00f, 1.00f, 0.00f, 0.00f),
  ApeColor("Blue", 0.00f, 0.00f, 1.00f, 0.00f),
  ApeColor("Amber", 1.00f, 0.75f, 0.00f, 0.03f),
  ApeColor("Amber White", 0.40f, 0.10f, 0.00f, 1.00f),
  ApeColor("Warm White", 0.25f, 0.00f, 0.00f, 1.00f),
  ApeColor("White", 0.19f, 0.00f, 0.00f, 1.00f),
  ApeColor("Cool White", 0.00f, 0.19f, 0.08f, 1.00f),
  ApeColor("Violet", 0.14f, 0.00f, 1.00f, 0.00f),
  ApeColor("Purple", 0.11f, 0.00f, 1.00f, 0.06f),
  ApeColor("Lavender", 0.15f, 0.00f, 1.00f, 0.58f),
  ApeColor("Magenta", 0.85f, 0.00f, 1.00f, 0.00f),
  ApeColor("Neon Pink", 1.00f, 0.00f, 0.34f, 0.33f),
  ApeColor("Coral", 1.00f, 0.05f, 0.00f, 0.06f),
  ApeColor("Yellow", 1.00f, 0.79f, 0.00f, 0.33f),
  ApeColor("Orange", 1.00f, 0.60f, 0.00f, 0.00f),
  ApeColor("Peach", 0.72f, 0.00f, 0.00f, 1.00f),
  ApeColor("Lime", 0.18f, 1.00f, 0.00f, 0.00f),
  ApeColor("Slate Blue", 0.00f, 0.45f, 1.00f, 0.38f),
  ApeColor("Moonlight Blue", 0.00f, 0.55f, 1.00f, 0.00f)
)

fun ApeColor.asProgram(id: String = randomId()) = Program(
  id = id,
  items = listOf(Program.Item(this))
)
