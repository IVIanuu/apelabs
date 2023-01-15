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

fun ApeColor.toColor() = Color(red, green, blue)
  .overlay(Color.White.copy(alpha = white))

private fun Color.overlay(overlay: Color): Color {
  val alphaSum = alpha + overlay.alpha
  return Color(
    (red * alpha + overlay.red * overlay.alpha) / alphaSum,
    (green * alpha + overlay.green * overlay.alpha) / alphaSum,
    (blue * alpha + overlay.blue * overlay.alpha) / alphaSum,
    alphaSum.coerceIn(0f, 1f),
  )
}

@Serializable data class ColorEntity(
  @PrimaryKey val id: String,
  val color: ApeColor
) {
  init {
    check(id.startsWith(COLOR_ID_PREFIX))
  }

  companion object : AbstractEntityDescriptor<ColorEntity>("colors")
}

const val COLOR_ID_PREFIX = "color:"

fun colorIdOf(id: String) = COLOR_ID_PREFIX + id

fun String.baseColorId() = removePrefix(COLOR_ID_PREFIX)
