package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.GroupConfigEntity
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.isUUID
import com.ivianuu.apelabs.data.randomId
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
                ),
                true
              )
            }
          }
          .filterNotNull()
      }
  ) { it.toList() }
    .onEmpty { emit(emptyList()) }

  fun groupConfig(id: String): Flow<GroupConfig?> = selectById<GroupConfigEntity>(id = id)
    .mapEntity { it.toGroupConfig() }

  suspend fun updateGroupConfig(
    config: GroupConfig,
    manageProgram: Boolean
  ) = transaction {
    if (manageProgram) {
      selectById<GroupConfigEntity>(config.id).first()
        ?.program
        ?.takeIf { it.isUUID }
        ?.let { deleteProgram(it) }

      if (config.program.id.isUUID)
        updateProgram(config.program)
    }

    insert(config.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun updateGroupConfigs(configs: List<GroupConfig>, manageProgram: Boolean) = transaction {
    configs.forEach { updateGroupConfig(it, manageProgram) }
  }

  suspend fun deleteGroupConfig(id: String) = transaction {
    selectById<GroupConfigEntity>(id).first()
      ?.program
      ?.takeIf { it.isUUID }
      ?.let { deleteProgram(it) }

    deleteById<GroupConfigEntity>(id)
  }

  private fun GroupConfig.toEntity() =
    GroupConfigEntity(id, program.id, brightness, speed, musicMode, blackout)

  private fun GroupConfigEntity.toGroupConfig(): Flow<GroupConfig> =
    program(program)
      .map {
        GroupConfig(
          id,
          it ?: Program(items = listOf(Program.Item(ApeColor(white = 1f)))),
          brightness,
          speed,
          musicMode,
          blackout
        )
      }
}
