package com.ivianuu.apelabs.domain

import android.view.animation.AccelerateInterpolator
import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.time.Clock
import com.ivianuu.essentials.time.days
import com.ivianuu.essentials.unlerp
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

@Provide class ContentUsageRepository(
  private val clock: Clock,
  private val pref: DataStore<ApeLabsPrefs>
) {
  val contentUsages: Flow<Map<String, Float>>
    get() = pref.data
      .map { it.programUsages.mapToUsageScores() }
      .distinctUntilChanged()

  suspend fun contentUsed(id: String) {
    pref.updateData {
      copy(
        programUsages = mutableMapOf<String, List<Duration>>().apply {
          programUsages.keys.forEach { id ->
            val usages = programUsages[id]?.filter { it > clock() - 28.days }
            if (usages?.isNotEmpty() == true) put(id, usages)
          }

          put(id, (programUsages[id] ?: emptyList()) + listOf(clock()))
        }
      )
    }
  }

  private val usageInterpolator = AccelerateInterpolator()

  private fun Map<String, List<Duration>>.mapToUsageScores(): Map<String, Float> {
    val now = clock()
    val firstUsage = (values
      .flatten()
      .minOrNull() ?: Duration.ZERO)

    val rawScores = this
      .mapValues { usages ->
        usages.value
          .map { usage ->
            usageInterpolator.getInterpolation(
              unlerp(
                firstUsage.inWholeMilliseconds,
                now.inWholeMilliseconds,
                usage.inWholeMilliseconds
              )
            )
          }
          .sum()
      }

    val scoreRange = (rawScores.values.minOrNull() ?: 0f)..(rawScores.values.maxOrNull() ?: 0f)

    return rawScores
      .mapValues { (_, rawScore) -> unlerp(scoreRange.start, scoreRange.endInclusive, rawScore) }
  }
}
