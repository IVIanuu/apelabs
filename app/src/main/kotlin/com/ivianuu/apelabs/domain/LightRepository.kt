package com.ivianuu.apelabs.domain

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.lightIdOf
import com.ivianuu.apelabs.data.toApeLabsId
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.state.stateFlow
import com.ivianuu.essentials.time.seconds
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

context(Logger, WappRemote, NamedCoroutineScope<AppScope>, WappRepository)
@Provide @Scoped<AppScope> class LightRepository(
  private val context: IOContext
) {
  val lights: Flow<List<Light>> = stateFlow {
    var lights by remember { mutableStateOf(emptyList<Light>()) }
    val lightRemovalJobs = remember { mutableMapOf<String, Job>() }

    LaunchedEffect(true) {
      wapps
        .flatMapLatest { wapps ->
          callbackFlow {
            wapps.parForEach { wapp ->
              withWapp(wapp.address) {
                messages.collect {
                  trySend(it)
                }
              }
            }

            awaitClose()
          }
        }
        .mapNotNull { message ->
          if ((message.getOrNull(0)?.toInt() == 82 ||
                message.getOrNull(0)?.toInt() == 83) &&
            // -96 means that it's a light
            message.getOrNull(1)?.toInt() == -96
          ) {
            val id = lightIdOf(message[2], message[3])
            Light(id, message.getOrNull(10)?.toInt()?.inc() ?: 1)
          } else null
        }
        .collect { light ->
          log { "${light.id} ping" }

          lightRemovalJobs.remove(light.id)?.cancel()

          lightRemovalJobs[light.id] = launch {
            delay(20.seconds)
            log { "${light.id} remove light" }
            lights = lights
              .filter { it.id != light.id }
          }

          lights = lights
            .filter { it.id != light.id } + light
        }
    }

    lights
      .sortedBy { it.id }
  }
    .distinctUntilChanged()
    .shareIn(this@NamedCoroutineScope, SharingStarted.WhileSubscribed(), 1)

  suspend fun flashLight(id: String) {
    wapps.first().parForEach { wapp ->
      withWapp(wapp.address) {
        val (id1, id2) = id.toApeLabsId()
        write(byteArrayOf(72, 10, id1, id2, 0))
      }
    }
  }

  suspend fun regroupLight(id: String, group: Int) = withContext(context) {
    wapps.first().parForEach { wapp ->
      withWapp(wapp.address) {
        val (id1, id2) = id.toApeLabsId()
        write(byteArrayOf(82, 10, id1, id2, 0, (group - 1).toByte(), 1, 0, 2, 13, 10))
      }
    }
  }
}
