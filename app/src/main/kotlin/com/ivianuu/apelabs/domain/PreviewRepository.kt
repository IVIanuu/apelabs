package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.Program
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.bracket
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

context(Logger, NamedCoroutineScope<AppScope>)
@Provide @Scoped<AppScope> class PreviewRepository {
  private val _previewProviders =
    MutableStateFlow<List<suspend (suspend (Program?) -> Unit) -> Unit>>(emptyList())
  private val lock = Mutex()

  val preview: Flow<Program?> = _previewProviders
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
    .shareIn(this@NamedCoroutineScope, SharingStarted.WhileSubscribed(), 1)

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
