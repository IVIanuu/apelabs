package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.data.*
import com.ivianuu.essentials.db.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*

@Provide class GroupConfigRepository(
  pref: DataStore<ApeLabsPrefs>,
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
    GroupConfigEntity(
      id,
      program.id,
      brightness,
      speed,
      mode == GroupConfig.Mode.MUSIC,
      mode == GroupConfig.Mode.STROBE,
      blackout
    )

  private suspend fun GroupConfigEntity.toGroupConfig() = GroupConfig(
    id,
    programRepository.program(program).first()
      ?: Program(items = listOf(Program.Item(ApeColor(white = 1f)))),
    brightness,
    speed,
    when {
      musicMode -> GroupConfig.Mode.MUSIC
      strobe -> GroupConfig.Mode.STROBE
      else -> GroupConfig.Mode.FADE
    },
    blackout
  )
}
