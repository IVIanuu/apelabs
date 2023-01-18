package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.GroupConfigEntity
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.randomId
import com.ivianuu.essentials.cast
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectById
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty

context(ApeLabsPrefsContext, Db, ProgramRepository) @Provide class GroupConfigRepository {
  val groupConfigs: Flow<List<GroupConfig>>
    get() = groupConfigs(GROUPS.map { it.toString() })

  val selectedGroupConfigs: Flow<List<GroupConfig>>
    get() = pref.data
      .map { it.selectedGroups }
      .distinctUntilChanged()
      .flatMapLatest { groupConfigs(it.map { it.toString() }) }

  private fun groupConfigs(ids: List<String>): Flow<List<GroupConfig>> = combine(
    ids
      .map { id ->
        groupConfig(id)
          .onEach {
            if (it == null) {
              updateGroupConfig(
                GroupConfig(
                  id = id,
                  program = createProgram(randomId())
                )
              )
            }
          }
          .filterNotNull()
      }
  ) { it.toList() }
    .onEmpty { emit(emptyList()) }

  fun groupConfig(id: String): Flow<GroupConfig?> = selectById<GroupConfigEntity>(id = id)
    .mapEntity { it.toGroupConfig() }

  suspend fun updateGroupConfig(config: GroupConfig) = transaction {
    insert(config.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun updateGroupConfigs(configs: List<GroupConfig>) = transaction {
    configs.forEach { updateGroupConfig(it) }
  }

  suspend fun deleteGroupConfig(id: String) = transaction {
    deleteById<GroupConfigEntity>(id)
  }

  private fun GroupConfig.toEntity() =
    GroupConfigEntity(id, program.id, brightness, speed, musicMode, blackout)

  private fun GroupConfigEntity.toGroupConfig(): Flow<GroupConfig> =
    program(program)
      .map {
        GroupConfig(id, it ?: Program.RAINBOW, brightness, speed, musicMode, blackout)
      }
}
