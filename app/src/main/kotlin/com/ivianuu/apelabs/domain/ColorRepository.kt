package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.LightColor
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

context(ApeLabsPrefsContext) @Provide class ColorRepository {
  val colors: Flow<Map<String, LightColor>>
    get() = pref.data
      .map { it.colors }
      .distinctUntilChanged()

  suspend fun saveColor(id: String, color: LightColor) {
    pref.updateData {
      copy(
        colors = colors.toMutableMap()
          .apply { this[id] = color }
      )
    }
  }

  suspend fun deleteColor(id: String) {
    pref.updateData {
      copy(
        colors = colors.toMutableMap()
          .apply { remove(id) }
      )
    }
  }
}

val BuiltInColors = mapOf(
  "Candlelight Classic" to LightColor(0.72f, 0.19f, 0.00f, 1.00f),
  "Candlelight Rustic" to LightColor(0.76f, 0.28f, 0.00f, 1.00f),
  "Soft Amber" to LightColor(0.53f, 0.16f, 0.00f, 1.00f),
  "Blush Pink" to LightColor(0.46f, 0.00f, 0.19f, 1.00f),
  "Lilac" to LightColor(0.25f, 0.00f, 0.98f, 1.00f),
  "Seafoam" to LightColor(0.07f, 0.38f, 0.00f, 1.00f),
  "Mint" to LightColor(0.09f, 0.62f, 0.00f, 1.00f),
  "Pastel Blue" to LightColor(0.00f, 0.58f, 1.00f, 0.20f),
  "Turquoise" to LightColor(0.00f, 1.00f, 0.42f, 0.00f),
  "Red" to LightColor(1.00f, 0.00f, 0.00f, 0.00f),
  "Green" to LightColor(0.00f, 1.00f, 0.00f, 0.00f),
  "Blue" to LightColor(0.00f, 0.00f, 1.00f, 0.00f),
  "Amber" to LightColor(1.00f, 0.75f, 0.00f, 0.03f),
  "Amber White" to LightColor(0.40f, 0.10f, 0.00f, 1.00f),
  "Warm White" to LightColor(0.25f, 0.00f, 0.00f, 1.00f),
  "White" to LightColor(0.19f, 0.00f, 0.00f, 1.00f),
  "Cool White" to LightColor(0.00f, 0.19f, 0.08f, 1.00f),
  "Violet" to LightColor(0.14f, 0.00f, 1.00f, 0.00f),
  "Purple" to LightColor(0.11f, 0.00f, 1.00f, 0.06f),
  "Lavender" to LightColor(0.15f, 0.00f, 1.00f, 0.58f),
  "Magenta" to LightColor(0.85f, 0.00f, 1.00f, 0.00f),
  "Neon Pink" to LightColor(1.00f, 0.00f, 0.34f, 0.33f),
  "Coral" to LightColor(1.00f, 0.05f, 0.00f, 0.06f),
  "Yellow" to LightColor(1.00f, 0.79f, 0.00f, 0.33f),
  "Orange" to LightColor(1.00f, 0.60f, 0.00f, 0.00f),
  "Peach" to LightColor(0.72f, 0.00f, 0.00f, 1.00f),
  "Lime" to LightColor(0.18f, 1.00f, 0.00f, 0.00f),
  "Slate Blue" to LightColor(0.00f, 0.45f, 1.00f, 0.38f),
  "Moonlight Blue" to LightColor(0.00f, 0.55f, 1.00f, 0.00f)
)
