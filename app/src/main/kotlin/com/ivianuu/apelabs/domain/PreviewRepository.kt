package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.program.Program
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.bracket
import com.ivianuu.essentials.coroutines.share
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.time.milliseconds
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

context(Logger, NamedCoroutineScope<AppScope>)
@Provide @Scoped<AppScope> class PreviewRepository {
  private val _previewProviders =
    MutableStateFlow<List<suspend (suspend (Program?) -> Unit) -> Unit>>(emptyList())
  private val lock = Mutex()

  private val _previewsEnabled = MutableStateFlow(false)
  val previewsEnabled: StateFlow<Boolean> get() = _previewsEnabled

  val preview: Flow<Program?> = _previewsEnabled
    .flatMapLatest {
      if (it) _previewProviders
      else flowOf(emptyList())
    }
    .map { it.lastOrNull() }
    .flatMapLatest { provider ->
      if (provider == null) flowOf(null)
      else callbackFlow<Program?> {
        provider {
          trySend(it)
        }
      }
    }
    .distinctUntilChanged()
    .share(SharingStarted.WhileSubscribed(), 1)

  suspend fun updatePreviewsEnabled(value: Boolean) {
    _previewsEnabled.value = value
  }

  suspend fun providePreviews(
    block: suspend (suspend (Program?) -> Unit) -> Unit
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
