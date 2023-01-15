package com.ivianuu.apelabs.scene

import com.ivianuu.apelabs.group.GROUPS
import com.ivianuu.apelabs.group.GroupConfig
import com.ivianuu.apelabs.group.GroupConfigRepository
import com.ivianuu.apelabs.program.ProgramRepository
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAll
import com.ivianuu.essentials.db.selectById
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

context(Db, GroupConfigRepository, ProgramRepository) @Provide class SceneRepository {
  val scenes: Flow<List<Scene>>
    get() = selectAll<SceneEntity>()
      .flatMapLatest { entities ->
        if (entities.isEmpty()) flowOf(emptyList())
        else combine(
          entities
            .map { it.toScene() }
        ) { it.toList() }
      }

  suspend fun createScene(id: String) = transaction {
    val scene = Scene(
      id = id,
      groupConfigs = GROUPS.associateWith { null }
    )

    insert(scene.toEntity())

    scene
  }

  suspend fun updateScene(scene: Scene) = transaction {
    insert(scene.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteScene(id: String) = transaction {
    selectById<SceneEntity>(id).first()?.groupConfigs
      ?.values
      ?.filterNotNull()
      ?.forEach { deleteGroupConfig(it) }
    deleteById<SceneEntity>(id)
  }

  fun scene(id: String) = selectById<SceneEntity>(id)
    .flatMapLatest { it?.toScene() ?: flowOf(null) }

  private fun SceneEntity.toScene(): Flow<Scene> = combine(
    groupConfigs
      .map { (group, configId) ->
        if (configId != null)
          groupConfig(configId)
            .map { group to (it ?: GroupConfig.DEFAULT) }
        else flowOf(group to null)
      }
  ) { it.toList().toMap() }
    .map { Scene(id, it) }

  private fun Scene.toEntity() = SceneEntity(id, groupConfigs.mapValues { it.value?.id })
}
