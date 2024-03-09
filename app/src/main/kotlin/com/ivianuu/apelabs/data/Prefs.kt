package com.ivianuu.apelabs.data

import com.ivianuu.essentials.data.*
import com.ivianuu.injekt.*
import kotlinx.serialization.*
import kotlin.time.*

@Serializable data class ApeLabsPrefs(
  val selectedGroups: Set<Int> = emptySet(),
  val programUsages: Map<String, List<Duration>> = emptyMap(),
  val knownWapps: Set<Wapp> = emptySet()
) {
  @Provide companion object {
    @Provide val dataStoreModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}
