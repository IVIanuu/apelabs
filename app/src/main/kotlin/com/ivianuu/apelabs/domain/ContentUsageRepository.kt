package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.time.Clock
import com.ivianuu.essentials.time.days
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

@Provide class ContentUsageRepository(
  private val clock: Clock,
  private val pref: DataStore<ApeLabsPrefs>
) {
  val contentUsages: Flow<Map<String, Double>>
    get() = pref.data
      .map {
        val now = clock()
        it.programUsages
          .mapValues {
            it.value
              .map { usage -> usage.inWholeMilliseconds * (usage / now) }
              .sum()
          }
      }
      .distinctUntilChanged()

  suspend fun contentUsed(id: String) {
    pref.updateData {
      copy(
        programUsages = mutableMapOf<String, List<Duration>>().apply {
          programUsages.keys.forEach { id ->
            val usages = programUsages[id]?.filter { it > clock() - 7.days }
            if (usages?.isNotEmpty() == true) put(id, usages)
          }

          put(id, (programUsages[id] ?: emptyList()) + listOf(clock()))
        }
      )
    }
  }
}
