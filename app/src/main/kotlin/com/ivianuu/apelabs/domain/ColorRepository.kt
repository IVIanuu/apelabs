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

context(Db) @Provide class ColorRepository {
  val userColors: Flow<List<ApeColor>>
    get() = selectAllTransform<ApeColor, _> { it?.takeUnless { it.id.isUUID } }

  suspend fun createColor(id: String): ApeColor = transaction {
    val color = ApeColor(id = id, white = 1f)
    updateColor(color)
    color
  }

  fun color(id: String) = selectTransform<ApeColor, _>(id) {
    it ?: BuiltInColors.singleOrNull { it.id == id }
  }

  suspend fun updateColor(color: ApeColor) = transaction {
    insert(color, InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteColor(id: String) = transaction {
    deleteById<ApeColor>(id)
  }
}
