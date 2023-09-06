package com.ivianuu.apelabs.data

import com.ivianuu.essentials.data.DataStoreModule
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable data class ApeLabsPrefs(
  val selectedGroups: Set<Int> = emptySet(),
  val programUsages: Map<String, List<Duration>> = emptyMap(),
  val shuffle: Boolean = false
) {
  @Provide companion object {
    @Provide val dataStoreModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}
