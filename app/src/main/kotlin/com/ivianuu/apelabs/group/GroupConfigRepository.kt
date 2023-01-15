package com.ivianuu.apelabs.group

import com.ivianuu.apelabs.program.Program
import com.ivianuu.apelabs.program.ProgramRepository
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAll
import com.ivianuu.essentials.db.selectById
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

context(Db, Logger, ProgramRepository) @Provide class GroupConfigRepository {
  val groupConfigs: Flow<List<GroupConfig>>
    get() = selectAll<GroupConfigEntity>()
      .map { it.filter { it.id.toIntOrNull() in GROUPS } }
      .flatMapLatest { entities ->
        if (entities.isEmpty()) flowOf(emptyList())
        else combine(
          entities
            .map { it.toGroupConfig() }
        ) { it.toList() }
      }

  fun groupConfig(id: String): Flow<GroupConfig?> = selectById<GroupConfigEntity>(id)
    .flatMapLatest { it?.toGroupConfig() ?: flowOf(null) }

  suspend fun updateGroupConfig(config: GroupConfig) = transaction {
    insert(config.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteGroupConfig(id: String) = transaction {
    deleteById<GroupConfigEntity>(id)
  }

  private fun GroupConfigEntity.toGroupConfig(): Flow<GroupConfig> = program(program)
    .map {
      GroupConfig(id, it ?: Program.RAINBOW, brightness, speed, musicMode, blackout)
    }

  private fun GroupConfig.toEntity() =
    GroupConfigEntity(id, program.id, brightness, speed, musicMode, blackout)
}
