package com.ivianuu.apelabs.scene

import com.ivianuu.apelabs.group.GROUPS
import com.ivianuu.apelabs.group.GroupConfig
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
