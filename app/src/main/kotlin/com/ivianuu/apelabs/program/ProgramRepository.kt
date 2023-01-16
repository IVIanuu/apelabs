package com.ivianuu.apelabs.program

import com.ivianuu.apelabs.color.ApeColor
import com.ivianuu.apelabs.color.ColorRepository
import com.ivianuu.apelabs.util.isUUID
import com.ivianuu.apelabs.util.randomId
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAll
import com.ivianuu.essentials.db.selectById
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.time.toDuration
import com.ivianuu.essentials.time.toLong
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

context(ColorRepository, Db, Logger) @Provide @Scoped<AppScope> class ProgramRepository {
  val programs: Flow<List<Program>>
    get() = selectAll<ProgramEntity>()
      .map { it.filter { !it.id.isUUID } }
      .flatMapLatest { entities ->
        if (entities.isEmpty()) flowOf(emptyList<Program>())
        else combine(
          entities
            .map { it.toProgram() }
        ) { it.toList() }
      }

  fun program(id: String): Flow<Program?> =
    when (id) {
      Program.RAINBOW_ID -> flowOf(Program.RAINBOW)
      else -> selectById<ProgramEntity>(id)
        .flatMapLatest { it?.toProgram() ?: flowOf(null) }
        .distinctUntilChanged()
    }

  suspend fun createProgram(id: String) = transaction {
    val program = Program(
      id = id,
      items = listOf(createProgramItem())
    )

    insert(program.toEntity())

    program
  }

  suspend fun updateProgram(program: Program) = transaction {
    insert(program.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteProgram(id: String) = transaction {
    selectById<ProgramEntity>(id).first()?.items?.forEach { deleteProgramItem(it) }
    deleteById<ProgramEntity>(id)
  }

  suspend fun programItem(id: String) = selectById<ProgramEntity.Item>(id)
    .map { it?.toItem() ?: null }

  suspend fun createProgramItem(): Program.Item = transaction {
    val item = Program.Item(id = randomId(), color = ApeColor())
    insert(item.toEntity())
    item
  }

  suspend fun updateProgramItem(item: Program.Item) = transaction {
    insert(item.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteProgramItem(id: String) = transaction {
    deleteById<ProgramEntity.Item>(id)
  }

  private fun ProgramEntity.toProgram(): Flow<Program> = combine(
    items
      .map { itemId ->
        selectById<ProgramEntity.Item>(itemId)
          .map { it!!.toItem() }
      }
  ) { Program(id, it.toList()) }

  private fun Program.toEntity() = ProgramEntity(
    id = id,
    items = items.map { it.id }
  )

  private fun ProgramEntity.Item.toItem() = Program.Item(id, color, fade, hold)

  private fun Program.Item.toEntity() = ProgramEntity.Item(id, color, fade, hold)
}
