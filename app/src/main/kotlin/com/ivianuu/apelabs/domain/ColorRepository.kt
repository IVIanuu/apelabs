package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

context(ApeLabsPrefsContext) @Provide class ColorRepository {
  val colors: Flow<Map<String, ApeColor>>
    get() = pref.data
      .map { it.colors }
      .distinctUntilChanged()

  suspend fun updateColor(id: String, color: ApeColor) {
    pref.updateData {
      copy(
        colors = colors.toMutableMap()
          .apply { put(id, color) }
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
  "Candlelight Classic" to ApeColor(0.72f, 0.19f, 0.00f, 1.00f),
  "Candlelight Rustic" to ApeColor(0.76f, 0.28f, 0.00f, 1.00f),
  "Soft Amber" to ApeColor(0.53f, 0.16f, 0.00f, 1.00f),
  "Blush Pink" to ApeColor(0.46f, 0.00f, 0.19f, 1.00f),
  "Lilac" to ApeColor(0.25f, 0.00f, 0.98f, 1.00f),
  "Seafoam" to ApeColor(0.07f, 0.38f, 0.00f, 1.00f),
  "Mint" to ApeColor(0.09f, 0.62f, 0.00f, 1.00f),
  "Pastel Blue" to ApeColor(0.00f, 0.58f, 1.00f, 0.20f),
  "Turquoise" to ApeColor(0.00f, 1.00f, 0.42f, 0.00f),
  "Red" to ApeColor(1.00f, 0.00f, 0.00f, 0.00f),
  "Green" to ApeColor(0.00f, 1.00f, 0.00f, 0.00f),
  "Blue" to ApeColor(0.00f, 0.00f, 1.00f, 0.00f),
  "Amber" to ApeColor(1.00f, 0.75f, 0.00f, 0.03f),
  "Amber White" to ApeColor(0.40f, 0.10f, 0.00f, 1.00f),
  "Warm White" to ApeColor(0.25f, 0.00f, 0.00f, 1.00f),
  "White" to ApeColor(0.19f, 0.00f, 0.00f, 1.00f),
  "Cool White" to ApeColor(0.00f, 0.19f, 0.08f, 1.00f),
  "Violet" to ApeColor(0.14f, 0.00f, 1.00f, 0.00f),
  "Purple" to ApeColor(0.11f, 0.00f, 1.00f, 0.06f),
  "Lavender" to ApeColor(0.15f, 0.00f, 1.00f, 0.58f),
  "Magenta" to ApeColor(0.85f, 0.00f, 1.00f, 0.00f),
  "Neon Pink" to ApeColor(1.00f, 0.00f, 0.34f, 0.33f),
  "Coral" to ApeColor(1.00f, 0.05f, 0.00f, 0.06f),
  "Yellow" to ApeColor(1.00f, 0.79f, 0.00f, 0.33f),
  "Orange" to ApeColor(1.00f, 0.60f, 0.00f, 0.00f),
  "Peach" to ApeColor(0.72f, 0.00f, 0.00f, 1.00f),
  "Lime" to ApeColor(0.18f, 1.00f, 0.00f, 0.00f),
  "Slate Blue" to ApeColor(0.00f, 0.45f, 1.00f, 0.38f),
  "Moonlight Blue" to ApeColor(0.00f, 0.55f, 1.00f, 0.00f)
)
