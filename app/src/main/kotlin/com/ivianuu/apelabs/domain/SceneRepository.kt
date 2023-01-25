package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.Scene
import com.ivianuu.apelabs.data.SceneEntity
import com.ivianuu.apelabs.data.isUUID
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.InsertConflictStrategy
import com.ivianuu.essentials.db.deleteById
import com.ivianuu.essentials.db.insert
import com.ivianuu.essentials.db.selectAllTransform
import com.ivianuu.essentials.db.selectById
import com.ivianuu.essentials.db.selectTransform
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

context(GroupConfigRepository, Db) @Provide class SceneRepository {
  val userScenes: Flow<List<Scene>>
    get() = selectAllTransform<SceneEntity, Scene> {
      it
        ?.takeUnless { it.id.isUUID }
        ?.toScene()
    }

  suspend fun createScene(id: String): Scene = transaction {
    val scene = Scene(id)
    updateScene(scene)
    scene
  }

  fun scene(id: String): Flow<Scene?> =
    selectTransform<SceneEntity, Scene>(id) { it?.toScene() }

  suspend fun updateScene(scene: Scene) = transaction {
    selectById<SceneEntity>(scene.id).first()
      ?.groupConfigs
      ?.filter { it.value != null && scene.groupConfigs[it.key]?.id != it.value }
      ?.forEach { deleteGroupConfig(it.value!!) }

    scene.groupConfigs
      .values
      .filterNotNull()
      .parForEach { updateGroupConfig(it, true) }

    insert(scene.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteScene(id: String) = transaction {
    selectById<SceneEntity>(id).first()
      ?.groupConfigs
      ?.mapNotNull { it.value }
      ?.forEach { deleteGroupConfig(it) }

    deleteById<SceneEntity>(id)
  }

  private fun Scene.toEntity() = SceneEntity(id, groupConfigs.mapValues { it.value?.id })

  private suspend fun SceneEntity.toScene() = Scene(
    id,
    groupConfigs
      .mapValues { (_, groupConfigId) ->
        groupConfigId?.let {
          groupConfig(groupConfigId)
            .first()
        }
      }
  )
}
