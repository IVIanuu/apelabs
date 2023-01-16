package com.ivianuu.apelabs.color

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.db.AbstractEntityDescriptor
import com.ivianuu.essentials.db.PrimaryKey
import kotlinx.serialization.Serializable

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

@Serializable data class NamedColor(
  @PrimaryKey val name: String,
  val color: ApeColor
) {
  companion object : AbstractEntityDescriptor<NamedColor>("colors")
}
