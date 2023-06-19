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

@Provide class SceneRepository(
  private val groupConfigRepository: GroupConfigRepository,
  private val db: Db
) {
  val userScenes: Flow<List<Scene>> = db.selectAllTransform<SceneEntity, Scene> {
    it
      ?.takeUnless { it.id.isUUID }
      ?.toScene()
  }

  suspend fun createScene(id: String): Scene = db.transaction {
    val scene = Scene(id)
    updateScene(scene)
    scene
  }

  fun scene(id: String): Flow<Scene?> =
    db.selectTransform<SceneEntity, Scene>(id) { it?.toScene() }

  suspend fun updateScene(scene: Scene) = db.transaction {
    db.selectById<SceneEntity>(scene.id).first()
      ?.groupConfigs
      ?.filter { it.value != null && scene.groupConfigs[it.key]?.id != it.value }
      ?.forEach { groupConfigRepository.deleteGroupConfig(it.value!!) }

    scene.groupConfigs
      .values
      .filterNotNull()
      .parForEach { groupConfigRepository.updateGroupConfig(it, true) }

    db.insert(scene.toEntity(), InsertConflictStrategy.REPLACE)
  }

  suspend fun deleteScene(id: String) = db.transaction {
    selectById<SceneEntity>(id).first()
      ?.groupConfigs
      ?.mapNotNull { it.value }
      ?.forEach { groupConfigRepository.deleteGroupConfig(it) }

    deleteById<SceneEntity>(id)
  }

  private fun Scene.toEntity() = SceneEntity(id, groupConfigs.mapValues { it.value?.id })

  private suspend fun SceneEntity.toScene() = Scene(
    id,
    groupConfigs
      .mapValues { (_, groupConfigId) ->
        groupConfigId?.let {
          groupConfigRepository.groupConfig(groupConfigId)
            .first()
        }
      }
  )
}
