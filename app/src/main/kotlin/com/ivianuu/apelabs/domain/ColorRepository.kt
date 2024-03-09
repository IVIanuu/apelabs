package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.db.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*

@Provide class ColorRepository(private val db: Db) {
  val userColors: Flow<List<ApeColor>> =
    db.selectAllTransform<ApeColor, _> { it?.takeUnless { it.id.isUUID } }

  suspend fun createColor(id: String): ApeColor = db.transaction {
    val color = ApeColor(id = id, white = 1f)
    updateColor(color)
    color
  }

  fun color(id: String) = db.selectTransform<ApeColor, _>(id) {
    it ?: BuiltInColors.singleOrNull { it.id == id }
  }

  suspend fun updateColor(color: ApeColor) = db.transaction {
    db.insert(color, InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteColor(id: String) = db.transaction {
    db.deleteById<ApeColor>(id)
  }
}
