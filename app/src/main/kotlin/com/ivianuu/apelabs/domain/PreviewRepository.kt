package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.bracket
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.time.milliseconds
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.NamedCoroutineScope
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.coroutines.ScopedCoroutineScope
import com.ivianuu.essentials.ui.UiScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Provide @Scoped<UiScope> class PreviewRepository(
  private val logger: Logger,
  private val pref: DataStore<ApeLabsPrefs>,
  scope: ScopedCoroutineScope<UiScope>
) {
  private val _previewProviders =
    MutableStateFlow<List<suspend (List<Int>, suspend (List<GroupConfig>) -> Unit) -> Unit>>(
      emptyList()
    )
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
      if (provider == null) flowOf(emptyList())
      else callbackFlow {
        provider(groups) {
          trySend(it)
        }
      }
    }
    .shareIn(scope, SharingStarted.WhileSubscribed(), 1)
    .distinctUntilChanged()

  suspend fun updatePreviewsEnabled(value: Boolean) {
    _previewsEnabled.value = value
  }

  suspend fun providePreviews(
    block: suspend (List<Int>, suspend (List<GroupConfig>) -> Unit) -> Unit
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
