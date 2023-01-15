package com.ivianuu.apelabs.color

import com.ivianuu.apelabs.util.isUUID
import com.ivianuu.apelabs.util.randomId
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAll
import com.ivianuu.essentials.db.selectById
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

context(Db) @Provide class ColorRepository {
  val colors: Flow<List<ApeColor>>
    get() = selectAll<ApeColorEntity>()
      .map { colors ->
        colors
          .filter { !it.id.isUUID }
          .map { it.toColor() }
      }

  fun color(id: String): Flow<ApeColor?> {
    return if (id == ApeColor.BLACK.id) flowOf(ApeColor.BLACK)
    else return selectById<ApeColorEntity>(id)
      .map { it?.toColor() }
      .distinctUntilChanged()
  }

  suspend fun createColor(): ApeColor = transaction {
    val finalColor = ApeColor(randomId())
    insert(finalColor.toEntity())
    finalColor
  }

  suspend fun updateColor(color: ApeColor) = transaction {
    insert(color.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteColor(id: String) = transaction {
    deleteById<ApeColorEntity>(id)
  }

  private fun ApeColorEntity.toColor() = ApeColor(id, red, green, blue, white)

  private fun ApeColor.toEntity() =
    ApeColorEntity(id, red, green, blue, white)
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
