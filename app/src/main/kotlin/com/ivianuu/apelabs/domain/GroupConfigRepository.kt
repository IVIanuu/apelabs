package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

context(ApeLabsPrefsContext, Logger, ProgramRepository) @Provide class GroupConfigRepository {
  val groupConfigs: Flow<Map<Int, GroupConfig>>
    get() = pref.data
      .map { it.groupConfigs }
      .distinctUntilChanged()

  suspend fun updateGroupConfig(group: Int, config: GroupConfig) {
    pref.updateData {
      copy(
        groupConfigs = groupConfigs.toMutableMap()
          .apply { put(group, config) }
      )
    }
  }

  suspend fun updateGroupConfigs(configs: Map<Int, GroupConfig>) {
    pref.updateData {
      copy(
        groupConfigs = groupConfigs.toMutableMap()
          .apply { putAll(configs) }
      )
    }
  }
}