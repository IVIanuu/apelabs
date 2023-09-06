package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.coroutines.timerFlow
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

@Provide @Scoped<AppScope> class GroupConfigProvider(
  groupConfigRepository: GroupConfigRepository,
  previewRepository: PreviewRepository,
  pref: DataStore<ApeLabsPrefs>
) {
  val groupConfigs: Flow<Map<Int, GroupConfig>> = combine(
    groupConfigRepository.groupConfigs,
    previewRepository.previewGroupConfigs,
    pref.data
  ) { groupConfigs, previewGroupConfigs, prefs ->
    GROUPS
      .associateWith { group ->
        previewGroupConfigs.singleOrNull { it.id == group.toString() }
          ?: groupConfigs.singleOrNull { it.id == group.toString() }
          ?: return@combine null
      }
  }
    .flatMapLatest {

    }
    .filterNotNull()
    .distinctUntilChanged()

  private fun shuffled(groupConfigs: Map<Int, GroupConfig>): Flow<Map<Int, GroupConfig>> =
    timerFlow(10.seconds)
      .map {
        val (shuffleableGroupConfigs, otherGroupConfigs) =
          groupConfigs.toList().partition { it.second.shuffle }

        (shuffleableGroupConfigs.map { it.second }.shuffled().mapIndexed { index, config ->
          config.id.toInt() to shuffleableGroupConfigs[index].second.copy(id = config.id)
        } + otherGroupConfigs).toMap()
      }
}
