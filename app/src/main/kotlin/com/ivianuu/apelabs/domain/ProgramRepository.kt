package com.ivianuu.apelabs.domain

import arrow.fx.coroutines.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.db.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*

@Provide class ProgramRepository(
  private val colorRepository: ColorRepository,
  private val db: Db
) {
  val userPrograms: Flow<List<Program>> = db.selectAllTransform<ProgramEntity, Program> {
    it
      ?.takeUnless { it.id.isUUID }
      ?.toProgram()
  }

  suspend fun createProgram(id: String): Program = db.transaction {
    val color = ApeColor(white = 1f).also { colorRepository.updateColor(it) }
    val program = Program(id = id, listOf(Program.Item(color)))
    updateProgram(program)
    program
  }

  fun program(id: String): Flow<Program?> =
    if (id == Program.RAINBOW.id) flowOf(Program.RAINBOW)
    else db.selectTransform<ProgramEntity, _>(id) { it?.toProgram() }

  suspend fun updateProgram(program: Program) = db.transaction {
    selectById<ProgramEntity>(program.id).first()
      ?.items
      ?.map { it.color }
      ?.filter { it.isUUID && it !in program.items.map { it.color.id } }
      ?.parMap { colorRepository.deleteColor(it) }

    program.items
      .map { it.color }
      .filter { it.id.isUUID }
      .parMap { colorRepository.updateColor(it) }

    insert(program.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteProgram(id: String) = db.transaction {
    selectById<ProgramEntity>(id).first()
      ?.items
      ?.map { it.color }
      ?.filter { it.isUUID }
      ?.parMap { colorRepository.deleteColor(it) }

    deleteById<ProgramEntity>(id)
  }

  private fun Program.toEntity() = ProgramEntity(id, items.map {
    ProgramEntity.Item(it.color.id, it.fadeTime, it.holdTime)
  })

  private suspend fun ProgramEntity.toProgram() = Program(
    id,
    items.map {
      Program.Item(
        colorRepository.color(it.color).first()
          ?: ApeColor(), it.fadeTime, it.holdTime
      )
    }
  )
}
