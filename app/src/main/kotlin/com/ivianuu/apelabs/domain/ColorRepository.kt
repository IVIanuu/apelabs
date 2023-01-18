package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAll
import com.ivianuu.essentials.db.selectById
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow

context(Db) @Provide class ColorRepository {
  val colors: Flow<List<ApeColor>>
    get() = selectAll()

  fun color(id: String) = selectById<ApeColor>(id)

  suspend fun updateColor(color: ApeColor) = transaction {
    insert(color, InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteColor(id: String) = transaction {
    deleteById<ApeColor>(id)
  }
}
