package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Provide @Scoped<AppScope> class UndoManager(
  private val groupConfigRepository: GroupConfigRepository,
  private val logger: Logger
) {
  private val groupConfigHistory = MutableStateFlow<List<Map<Int, GroupConfig?>>>(emptyList())

  val canUndo: Flow<Boolean> = groupConfigHistory
    .map { it.isNotEmpty() }
    .distinctUntilChanged()

  suspend fun takeConfigSnapshot() {
    val configs = groupConfigRepository.groupConfigs.first()
    synchronized(this) {
      groupConfigHistory.value += GROUPS
        .associateWith { group ->
          configs.firstOrNull { it.id == group.toString() }
        }

      while (groupConfigHistory.value.size > MAX_HISTORY_SIZE)
        groupConfigHistory.value = groupConfigHistory.value.drop(1)
    }

    logger.log { "took snapshot $configs" }
  }

  suspend fun undoLast() {
    val configs = synchronized(this) {
      groupConfigHistory.value.lastOrNull()?.values?.filterNotNull()
        ?.also { groupConfigHistory.value = groupConfigHistory.value.dropLast(1) }
    } ?: return

    groupConfigRepository.updateGroupConfigs(configs, true)

    logger.log { "undo to $configs" }
  }

  private companion object {
    private const val MAX_HISTORY_SIZE = 50
  }
}
