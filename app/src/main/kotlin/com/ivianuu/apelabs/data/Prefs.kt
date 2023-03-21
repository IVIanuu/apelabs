package com.ivianuu.apelabs.data

import com.ivianuu.essentials.android.prefs.DataStoreModule
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable data class ApeLabsPrefs(
  val selectedGroups: Set<Int> = emptySet(),
  val programUsages: Map<String, List<Duration>> = emptyMap(),
) {
  companion object {
    @Provide val prefModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}
