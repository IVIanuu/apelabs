package com.ivianuu.apelabs.data

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.db.AbstractEntityDescriptor
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

fun ApeColor.toComposeColor() = Color(red, green, blue)
  .overlay(Color.White.copy(alpha = white))

fun Color.toApeColor(id: String = randomId()) = ApeColor(id, red, green, blue)

private fun Color.overlay(overlay: Color): Color {
  val alphaSum = alpha + overlay.alpha
  return Color(
    (red * alpha + overlay.red * overlay.alpha) / alphaSum,
    (green * alpha + overlay.green * overlay.alpha) / alphaSum,
    (blue * alpha + overlay.blue * overlay.alpha) / alphaSum,
    alphaSum.coerceIn(0f, 1f),
  )
}

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
