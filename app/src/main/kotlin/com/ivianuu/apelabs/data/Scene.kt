package com.ivianuu.apelabs.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable data class Scene(
  val groupConfigs: Map<Int, GroupConfig?> = GROUPS.associateWith { null }
) {
  init {
    GROUPS.forEach {
      check(it in groupConfigs)
    }
  }
}

context(ApeLabsPrefsContext) val scenes: Flow<Map<String, Scene>>
  get() = pref.data
    .map { it.scenes }
    .distinctUntilChanged()

context(ApeLabsPrefsContext) fun scene(id: String) = scenes
  .map { it[id] }
  .distinctUntilChanged()

context(ApeLabsPrefsContext) suspend fun updateScene(id: String, scene: Scene) {
  pref.updateData {
    copy(
      scenes = scenes.toMutableMap()
        .apply { put(id, scene) }
    )
  }
}

context(ApeLabsPrefsContext) suspend fun deleteScene(id: String) {
  pref.updateData {
    copy(
      scenes = scenes.toMutableMap()
        .apply { remove(id) }
    )
  }
}
