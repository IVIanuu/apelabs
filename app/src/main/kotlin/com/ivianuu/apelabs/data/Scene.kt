package com.ivianuu.apelabs.data

import com.ivianuu.essentials.db.*
import kotlinx.serialization.*

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

@Serializable @Entity data class SceneEntity(
  @PrimaryKey val id: String,
  val groupConfigs: Map<Int, String?>
)
