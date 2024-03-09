package com.ivianuu.apelabs.domain

import android.view.animation.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.*
import com.ivianuu.essentials.coroutines.*
import com.ivianuu.essentials.data.*
import com.ivianuu.essentials.time.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days

@Provide class ContentUsageRepository(
  private val clock: Clock,
  private val pref: DataStore<ApeLabsPrefs>,
  private val scope: ScopedCoroutineScope<AppScope>
) {
  val contentUsages: Flow<Map<String, Float>> = pref.data
    .map { it.programUsages.mapToUsageScores() }
    .distinctUntilChanged()

  init {
    trimUsages()
  }

  suspend fun contentUsed(id: String) {
    pref.updateData {
      copy(
        programUsages = mutableMapOf<String, List<Duration>>().apply {
          programUsages.keys.forEach { id ->
            val usages = programUsages[id]?.filter { it > clock.now() - 28.days }
            if (usages?.isNotEmpty() == true) put(id, usages)
          }

          put(id, (programUsages[id] ?: emptyList()) + listOf(clock.now()))
        }
      )
    }
  }

  private fun trimUsages() {
    scope.launch {
      pref.updateData {
        copy(programUsages = programUsages.trim(84.days),)
      }
    }
  }

  private fun Map<String, List<Duration>>.trim(since: Duration): Map<String, List<Duration>> =
    mutableMapOf<String, List<Duration>>().apply {
      val now = clock.now()
      this@trim.keys.forEach { id ->
        val usages = this@trim[id]?.filter { it > now - since }
        if (usages?.isNotEmpty() == true) put(id, usages)
      }
    }

  private val usageInterpolator = AccelerateInterpolator(2f)

  private fun Map<String, List<Duration>>.mapToUsageScores(): Map<String, Float> {
    val now = clock.now()
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
