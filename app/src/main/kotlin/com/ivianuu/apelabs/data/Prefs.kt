package com.ivianuu.apelabs.data

import com.ivianuu.apelabs.color.ApeColor
import com.ivianuu.apelabs.group.GROUPS
import com.ivianuu.apelabs.group.GroupConfig
import com.ivianuu.apelabs.program.Program
import com.ivianuu.apelabs.scene.Scene
import com.ivianuu.essentials.android.prefs.DataStoreModule
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable

@Serializable data class ApeLabsPrefs(
  val selectedGroups: Set<Int> = emptySet(),
  val colors: Map<String, ApeColor> = emptyMap(),
  val programs: Map<String, Program> = emptyMap(),
  val groupConfigs: Map<Int, GroupConfig> = GROUPS.associateWith { GroupConfig() },
  val scenes: Map<String, Scene> = emptyMap()
) {
  companion object {
    @Provide val prefModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}

@Provide @JvmInline value class ApeLabsPrefsContext(val pref: DataStore<ApeLabsPrefs>)
