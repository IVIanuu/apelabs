package com.ivianuu.apelabs.group

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.program.Program
import com.ivianuu.apelabs.program.ProgramRepository
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

context(ApeLabsPrefsContext, Logger, ProgramRepository) @Provide class GroupConfigRepository {
  val groupConfigs: Flow<Map<Int, GroupConfig>>
    get() = pref.data
      .map { it.groupConfigs }
      .distinctUntilChanged()
      .flatMapLatest { entities ->
        if (entities.isEmpty()) flowOf(emptyMap())
        else combine(
          entities
            .map { (group, entity) ->
              entity.toGroupConfig()
                .map { group to it }
            }
        ) { it.toList().toMap() }
      }

  fun groupConfig(group: Int): Flow<GroupConfig?> = pref.data
    .map { it.groupConfigs[group] }
    .flatMapLatest { it?.toGroupConfig() ?: flowOf(null) }

  suspend fun updateGroupConfigs(configs: Map<Int, GroupConfig>) {
    pref.updateData {
      copy(
        groupConfigs = buildMap {
          putAll(groupConfigs)
          putAll(configs.mapValues { it.value.toEntity() })
        }
      )
    }
  }

  private fun GroupConfigEntity.toGroupConfig(): Flow<GroupConfig> = program(program)
    .map {
      GroupConfig(it ?: Program.RAINBOW, brightness, speed, musicMode, blackout)
    }

  private fun GroupConfig.toEntity() =
    GroupConfigEntity(program.id, brightness, speed, musicMode, blackout)
}
