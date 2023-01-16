package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.Scene
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

context(ApeLabsPrefsContext, GroupConfigRepository, ProgramRepository)
@Provide class SceneRepository {
  val scenes: Flow<Map<String, Scene>>
    get() = pref.data
      .map { it.scenes }
      .distinctUntilChanged()

  fun scene(id: String) = scenes
    .map { it[id] }
    .distinctUntilChanged()

  suspend fun updateScene(id: String, scene: Scene) {
    pref.updateData {
      copy(
        scenes = scenes.toMutableMap()
          .apply { put(id, scene) }
      )
    }
  }

  suspend fun deleteScene(id: String) {
    pref.updateData {
      copy(
        scenes = scenes.toMutableMap()
          .apply { remove(id) }
      )
    }
  }
}