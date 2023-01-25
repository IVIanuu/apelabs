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
import com.ivianuu.essentials.db.selectAllTransform
import com.ivianuu.essentials.db.selectById
import com.ivianuu.essentials.db.selectTransform
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

context(ColorRepository, Db) @Provide class ProgramRepository {
  val userPrograms: Flow<List<Program>>
    get() = selectAllTransform<ProgramEntity, Program> {
      it
        ?.takeUnless { it.id.isUUID }
        ?.toProgram()
    }

  suspend fun createProgram(id: String): Program = transaction {
    val color = ApeColor(white = 1f).also { updateColor(it) }
    val program = Program(id = id, listOf(Program.Item(color)))
    updateProgram(program)
    program
  }

  fun program(id: String): Flow<Program?> =
    if (id == Program.RAINBOW.id) flowOf(Program.RAINBOW)
    else selectTransform<ProgramEntity, _>(id) { it?.toProgram() }

  suspend fun updateProgram(program: Program) = transaction {
    selectById<ProgramEntity>(program.id).first()
      ?.items
      ?.map { it.color }
      ?.filter { it.isUUID && it !in program.items.map { it.color.id } }
      ?.parForEach { deleteColor(it) }

    program.items
      .map { it.color }
      .filter { it.id.isUUID }
      .parForEach { updateColor(it) }

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

  private fun Program.toEntity() = ProgramEntity(id, items.map {
    ProgramEntity.Item(it.color.id, it.fadeTime, it.holdTime)
  })

  private suspend fun ProgramEntity.toProgram() = Program(
    id,
    items.map { Program.Item(color(it.color).first() ?: ApeColor(), it.fadeTime, it.holdTime) }
  )
}
