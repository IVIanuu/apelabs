package com.ivianuu.apelabs.data

import com.ivianuu.essentials.android.prefs.DataStoreModule
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable

@Serializable data class ApeLabsPrefs(val selectedGroups: Set<Int> = emptySet()) {
  companion object {
    @Provide val prefModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}
