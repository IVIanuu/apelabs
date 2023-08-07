package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.coroutines.ScopedCoroutineScope
import com.ivianuu.essentials.coroutines.bracket
import com.ivianuu.essentials.coroutines.infiniteEmptyFlow
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.ui.UiScope
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Provide @Scoped<UiScope> class PreviewRepository(
  private val pref: DataStore<ApeLabsPrefs>,
  scope: ScopedCoroutineScope<UiScope>
) {
  private val _previewProviders =
    MutableStateFlow(emptyList<(List<Int>) -> Flow<List<GroupConfig>>>())
  private val lock = Mutex()

  private val _previewsEnabled = MutableStateFlow(false)
  val previewsEnabled: StateFlow<Boolean> by this::_previewsEnabled

  val previewGroupConfigs: Flow<List<GroupConfig>> = _previewsEnabled
    .flatMapLatest {
      if (it) _previewProviders
      else flowOf(emptyList())
    }
    .map { it.lastOrNull() }
    .flatMapLatest { provider ->
      pref.data
        .map { it.selectedGroups }
        .distinctUntilChanged()
        .map { it.toList() to provider }
    }
    .flatMapLatest { (groups, provider) ->
      provider?.invoke(groups) ?: flowOf(emptyList())
    }
    .shareIn(scope, SharingStarted.WhileSubscribed(), 1)
    .distinctUntilChanged()

  suspend fun updatePreviewsEnabled(value: Boolean) {
    _previewsEnabled.value = value
  }

  suspend fun providePreviews(
    block: (List<Int>) -> Flow<List<GroupConfig>>
  ): Nothing = bracket(
    acquire = {
      lock.withLock {
        _previewProviders.value = _previewProviders.value + block
      }
    },
    use = { awaitCancellation() },
    release = { _, _ ->
      lock.withLock {
        _previewProviders.value = _previewProviders.value - block
      }
    }
  )
}
