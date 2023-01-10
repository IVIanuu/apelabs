package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.lightIdOf
import com.ivianuu.apelabs.data.toApeLabsId
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

@Provide @Scoped<AppScope> class LightRepository(
  private val context: IOContext,
  private val logger: Logger,
  private val remote: WappRemote,
  scope: NamedCoroutineScope<AppScope>,
  private val wappRepository: WappRepository
) {
  val lights: Flow<List<Light>> = wappRepository.wapps
    .flatMapLatest { wapps ->
      callbackFlow {
        wapps.parForEach { wapp ->
          remote.withWapp(wapp.address) {
            messages.collect {
              trySend(it)
            }
          }
        }

        awaitClose()
      }
    }
    .onStart { emit(byteArrayOf()) }
    .scan(emptyList<Light>()) { state, message ->
      val lights = state.toMutableList()
      if ((message.getOrNull(0)?.toInt() == 82 ||
            message.getOrNull(0)?.toInt() == 83) &&
        // -96 means that it's a light
        message.getOrNull(1)?.toInt() == -96
      ) {
        log { "message ${message.contentToString()}" }
        val id = lightIdOf(message[2], message[3])
        val light = Light(id, message.getOrNull(10)?.toInt()?.inc() ?: 1)
        lights.removeAll { it.id == id }
        lights.add(light)
      }

      lights
        .sortedBy { it.id }
    }
    .distinctUntilChanged()
    .flowOn(context)
    .shareIn(scope, SharingStarted.WhileSubscribed(), 1)

  suspend fun flashLight(id: String) {
    wappRepository.wapps.first().parForEach { wapp ->
      remote.withWapp(wapp.address) {
        val (id1, id2) = id.toApeLabsId()
        write(byteArrayOf(72, 10, id1, id2, 0))
      }
    }
  }

  suspend fun regroupLight(id: String, group: Int) = withContext(context) {
    wappRepository.wapps.first().parForEach { wapp ->
      remote.withWapp(wapp.address) {
        val (id1, id2) = id.toApeLabsId()
        write(byteArrayOf(82, 10, id1, id2, 0, (group - 1).toByte(), 1, 0, 2, 13, 10))
      }
    }
  }
}
