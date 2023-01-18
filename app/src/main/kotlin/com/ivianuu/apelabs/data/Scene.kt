package com.ivianuu.apelabs.data

import com.ivianuu.essentials.db.AbstractEntityDescriptor
import com.ivianuu.essentials.db.PrimaryKey
import kotlinx.serialization.Serializable

data class Scene(
  val id: String,
  val groupConfigs: Map<Int, GroupConfig?> = GROUPS.associateWith { null }
) {
  init {
    GROUPS.forEach {
      check(it in groupConfigs)
    }
  }
}

@Serializable data class SceneEntity(
  @PrimaryKey val id: String,
  val groupConfigs: Map<Int, String?>
) {
  companion object : AbstractEntityDescriptor<SceneEntity>("scenes")
}

