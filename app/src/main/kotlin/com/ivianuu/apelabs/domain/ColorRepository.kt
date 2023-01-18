package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.BuiltInColors
import com.ivianuu.apelabs.data.isUUID
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAll
import com.ivianuu.essentials.db.selectById
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

context(Db) @Provide class ColorRepository {
  val colors: Flow<List<ApeColor>>
    get() = selectAll<ApeColor>()

  fun color(id: String) = selectById<ApeColor>(id)

  suspend fun updateColor(color: ApeColor) = transaction {
    insert(color, InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteColor(id: String) = transaction {
    deleteById<ApeColor>(id)
  }
}
