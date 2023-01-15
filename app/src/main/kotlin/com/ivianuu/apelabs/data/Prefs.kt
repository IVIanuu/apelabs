package com.ivianuu.apelabs.data

import com.ivianuu.essentials.android.prefs.DataStoreModule
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable

@Serializable data class ApeLabsPrefs(
  val selectedGroups: Set<Int> = emptySet(),
  val groupConfigs: Map<Int, GroupConfig> = emptyMap()
) {
  companion object {
    @Provide val prefModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}

@Provide @JvmInline value class ApeLabsPrefsContext(val pref: DataStore<ApeLabsPrefs>)
