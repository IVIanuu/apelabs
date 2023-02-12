package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.BuiltInColors
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.isUUID
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAllTransform
import com.ivianuu.essentials.db.selectTransform
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow

@Provide class ColorRepository(private val db: Db) {
  val userColors: Flow<List<ApeColor>>
    get() = db.selectAllTransform<ApeColor, _> { it?.takeUnless { it.id.isUUID } }

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
