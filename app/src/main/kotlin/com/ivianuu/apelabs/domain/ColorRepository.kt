package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.LightColor
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

context(ApeLabsPrefsContext)
@Provide class ColorRepository {
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
  "Red" to LightColor(1f, 0f, 0f, 0f),
  "Green" to LightColor(0f, 1f, 0f, 0f),
  "Blue" to LightColor(0f, 0f, 1f, 0f),
  "Yellow" to LightColor(1f, 1f, 0f, 0f),
  "Pink" to LightColor(1f, 0f, 1f, 0f),
  "Cyan" to LightColor(0f, 1f, 1f, 0f),
  "White" to LightColor(0f, 0f, 0f, 1f),
  "Rgb white" to LightColor(1f, 1f, 1f, 0f),
  "Amber" to LightColor(1f, 0.75f, 0f, 0.03f)
)
