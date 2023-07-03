package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.GroupConfigEntity
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.isUUID
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectById
import com.ivianuu.essentials.db.selectTransform
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart

@Provide class GroupConfigRepository(
  private val pref: DataStore<ApeLabsPrefs>,
  private val db: Db,
  private val programRepository: ProgramRepository
) {
  val groupConfigs: Flow<List<GroupConfig>> = groupConfigs(GROUPS.map { it.toString() })

  val selectedGroupConfigs: Flow<List<GroupConfig>> = pref.data
    .map { it.selectedGroups }
    .distinctUntilChanged()
    .flatMapLatest { groupConfigs(it.map { it.toString() }) }

  private fun groupConfigs(ids: List<String>): Flow<List<GroupConfig>> = db.tableChanges
    .onStart { emit(null) }
    .mapLatest {
      ids
        .map { db.selectById<GroupConfigEntity>(it).first() }
        .mapNotNull { it?.toGroupConfig() }
    }
    .distinctUntilChanged()

  fun groupConfig(id: String): Flow<GroupConfig?> = db.selectTransform<GroupConfigEntity, _>(id) {
    it?.toGroupConfig()
  }

  suspend fun updateGroupConfig(config: GroupConfig, manageProgram: Boolean) = db.transaction {
    if (manageProgram) {
      selectById<GroupConfigEntity>(config.id).first()
        ?.program
        ?.takeIf { it.isUUID && it != config.program.id }
        ?.let { programRepository.deleteProgram(it) }

      if (config.program.id.isUUID)
        programRepository.updateProgram(config.program)
    }

    insert(config.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun updateGroupConfigs(configs: List<GroupConfig>, manageProgram: Boolean) =
    db.transaction {
      configs.forEach { updateGroupConfig(it, manageProgram) }
    }

  suspend fun deleteGroupConfig(id: String) = db.transaction {
    db.selectById<GroupConfigEntity>(id).first()
      ?.program
      ?.takeIf { it.isUUID }
      ?.let { programRepository.deleteProgram(it) }

    db.deleteById<GroupConfigEntity>(id)
  }

  private fun GroupConfig.toEntity() =
    GroupConfigEntity(id, program.id, brightness, speed, musicMode, blackout)

  private suspend fun GroupConfigEntity.toGroupConfig() = GroupConfig(
    id,
    programRepository.program(program).first()
      ?: Program(items = listOf(Program.Item(ApeColor(white = 1f)))),
    brightness,
    speed,
    musicMode,
    blackout
  )
}
