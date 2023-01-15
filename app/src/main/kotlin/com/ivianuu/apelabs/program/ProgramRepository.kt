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
import com.ivianuu.essentials.time.toDuration
import com.ivianuu.essentials.time.toLong
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

context(ColorRepository, Db) @Provide @Scoped<AppScope> class ProgramRepository {
  val programs: Flow<List<Program>>
    get() = selectAll<ProgramEntity>()
      .flatMapLatest { entities ->
        if (entities.isEmpty()) flowOf(emptyList<Program>())
        combine(
          entities
            .map { it.toProgram() }
        ) { it.toList() }
      }

  fun program(id: String) = selectById<ProgramEntity>(id)
    .flatMapLatest { it?.toProgram() ?: flowOf(null) }
    .distinctUntilChanged()

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

  suspend fun createProgramItem(): Program.Item = transaction {
    val item = Program.Item(id = randomId(), color = createColor())
    insert(item.toEntity())
    item
  }

  suspend fun updateProgramItem(item: Program.Item) = transaction {
    insert(item.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteProgramItem(id: String) = transaction {
    selectById<ProgramEntity.Item>(id).first()
      ?.color
      ?.takeIf { it.isUUID }
      ?.let { deleteColor(it) }

    deleteById<ProgramEntity.Item>(id)
  }

  private fun ProgramEntity.toProgram(): Flow<Program> = combine(
    items
      .map { itemId ->
        selectById<ProgramEntity.Item>(itemId)
          .flatMapLatest { it!!.toItem() }
      }
  ) { Program(id, it.toList()) }

  private fun Program.toEntity() = ProgramEntity(
    id = id,
    items = items.map { it.id }
  )

  private fun ProgramEntity.Item.toItem(): Flow<Program.Item> = color(color)
    .map { Program.Item(id, it ?: ApeColor(), fade.toDuration(), hold.toDuration()) }
    .distinctUntilChanged()

  private fun Program.Item.toEntity() = ProgramEntity.Item(
    id = id,
    color = color.id!!,
    fade = fade.toLong(),
    hold = hold.toLong()
  )
}
