package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.ProgramEntity
import com.ivianuu.apelabs.data.isUUID
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAll
import com.ivianuu.essentials.db.selectById
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

context(ColorRepository, Db) @Provide class ProgramRepository {
  val programs: Flow<List<Program>>
    get() = selectAll<ProgramEntity>()
      .mapEntities { it.toProgram() }

  suspend fun createProgram(id: String): Program = transaction {
    val color = ApeColor().also { updateColor(it) }
    val program = Program(id = id, listOf(Program.Item(color)))
    updateProgram(program)
    program
  }

  fun program(id: String): Flow<Program?> = selectById<ProgramEntity>(id)
    .mapEntity { it.toProgram() }

  suspend fun updateProgram(program: Program) = transaction {
    insert(program.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteProgram(id: String) = transaction {
    selectById<ProgramEntity>(id).first()
      ?.items
      ?.map { it.color }
      ?.filter { it.isUUID }
      ?.parForEach { deleteColor(it) }

    deleteById<ProgramEntity>(id)
  }

  private fun Program.toEntity() = ProgramEntity(id, items = items.map { it.toEntityItem() })

  private fun ProgramEntity.toProgram(): Flow<Program> = combine(
    items
      .map { item ->
        color(item.color)
          .map { Program.Item(it ?: ApeColor()) }
      }
  ) { Program(id, it.toList()) }

  private fun Program.Item.toEntityItem() = ProgramEntity.Item(color.id, fadeTime, holdTime)
}
