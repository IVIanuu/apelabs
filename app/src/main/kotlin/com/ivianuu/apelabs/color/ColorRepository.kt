package com.ivianuu.apelabs.color

import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAll
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow

context(Db) @Provide class ColorRepository {
  val colors: Flow<List<NamedColor>>
    get() = selectAll()

  suspend fun updateColor(color: NamedColor) = transaction {
    insert(color, InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteColor(id: String) = transaction {
    deleteById<NamedColor>(id)
  }
}

val BuiltInColors = listOf(
  NamedColor("Candlelight Classic", ApeColor(0.72f, 0.19f, 0.00f, 1.00f)),
  NamedColor("Candlelight Rustic", ApeColor(0.76f, 0.28f, 0.00f, 1.00f)),
  NamedColor("Soft Amber", ApeColor(0.53f, 0.16f, 0.00f, 1.00f)),
  NamedColor("Blush Pink", ApeColor(0.46f, 0.00f, 0.19f, 1.00f)),
  NamedColor("Lilac", ApeColor(0.25f, 0.00f, 0.98f, 1.00f)),
  NamedColor("Seafoam", ApeColor(0.07f, 0.38f, 0.00f, 1.00f)),
  NamedColor("Mint", ApeColor(0.09f, 0.62f, 0.00f, 1.00f)),
  NamedColor("Pastel Blue", ApeColor(0.00f, 0.58f, 1.00f, 0.20f)),
  NamedColor("Turquoise", ApeColor(0.00f, 1.00f, 0.42f, 0.00f)),
  NamedColor("Red", ApeColor(1.00f, 0.00f, 0.00f, 0.00f)),
  NamedColor("Green", ApeColor(0.00f, 1.00f, 0.00f, 0.00f)),
  NamedColor("Blue", ApeColor(0.00f, 0.00f, 1.00f, 0.00f)),
  NamedColor("Amber", ApeColor(1.00f, 0.75f, 0.00f, 0.03f)),
  NamedColor("Amber White", ApeColor(0.40f, 0.10f, 0.00f, 1.00f)),
  NamedColor("Warm White", ApeColor(0.25f, 0.00f, 0.00f, 1.00f)),
  NamedColor("White", ApeColor(0.19f, 0.00f, 0.00f, 1.00f)),
  NamedColor("Cool White", ApeColor(0.00f, 0.19f, 0.08f, 1.00f)),
  NamedColor("Violet", ApeColor(0.14f, 0.00f, 1.00f, 0.00f)),
  NamedColor("Purple", ApeColor(0.11f, 0.00f, 1.00f, 0.06f)),
  NamedColor("Lavender", ApeColor(0.15f, 0.00f, 1.00f, 0.58f)),
  NamedColor("Magenta", ApeColor(0.85f, 0.00f, 1.00f, 0.00f)),
  NamedColor("Neon Pink", ApeColor(1.00f, 0.00f, 0.34f, 0.33f)),
  NamedColor("Coral", ApeColor(1.00f, 0.05f, 0.00f, 0.06f)),
  NamedColor("Yellow", ApeColor(1.00f, 0.79f, 0.00f, 0.33f)),
  NamedColor("Orange", ApeColor(1.00f, 0.60f, 0.00f, 0.00f)),
  NamedColor("Peach", ApeColor(0.72f, 0.00f, 0.00f, 1.00f)),
  NamedColor("Lime", ApeColor(0.18f, 1.00f, 0.00f, 0.00f)),
  NamedColor("Slate Blue", ApeColor(0.00f, 0.45f, 1.00f, 0.38f)),
  NamedColor("Moonlight Blue", ApeColor(0.00f, 0.55f, 1.00f, 0.00f))
)
