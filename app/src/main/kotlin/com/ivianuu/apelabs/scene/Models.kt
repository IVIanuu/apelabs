package com.ivianuu.apelabs.scene

import com.ivianuu.apelabs.group.GroupConfig
import com.ivianuu.apelabs.group.GroupConfigEntity
import com.ivianuu.essentials.db.AbstractEntityDescriptor
import com.ivianuu.essentials.db.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable data class Scene(
  val id: String,
  val groupConfigs: Map<Int, GroupConfig>
)

@Serializable data class SceneEntity(
  @PrimaryKey val id: String,
  val groupConfigs: Map<Int, String>
) {
  companion object : AbstractEntityDescriptor<SceneEntity>("scenes")
}
