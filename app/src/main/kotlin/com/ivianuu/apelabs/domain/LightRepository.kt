package com.ivianuu.apelabs.domain

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.lightIdOf
import com.ivianuu.apelabs.data.toApeLabsLightId
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.compose.stateFlow
import com.ivianuu.essentials.compose.getValue
import com.ivianuu.essentials.compose.setValue
import com.ivianuu.essentials.coroutines.EventFlow
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
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
      _groupLightsChangedEvents.collect { event ->
        lights = lights
          .map {
            if (it.id == event.lightId) it.copy(group = event.group)
            else it
          }
      }
    }

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
          log { "on message ${message.contentToString()}" }
          if ((message.getOrNull(0)?.toInt() == 82 ||
                message.getOrNull(0)?.toInt() == 83) &&
            // -112 is the wapp
            message.getOrNull(1)?.toInt() != -112
          ) {
            val id = lightIdOf(message[2], message[3])
            val type = Light.Type.values().firstOrNull { it.id == message[1].toInt() }
            val light = Light(
              id = id,
              group = message.getOrNull(10)?.toInt()?.inc() ?: 1,
              battery = if (type != Light.Type.COIN) message[6] / 100f else null,
              type = type
            )

            val oldLight = lights.singleOrNull { it.id == id }
            if ((message[0].toInt() == 82 && oldLight == null) ||
              (oldLight != null && light.group != oldLight.group)
            )
              _groupLightsChangedEvents.tryEmit(GroupLightChangedEvent(light.group, light.id))

            light
          } else null
        }
        .collect { light ->
          log { "${light.id} ping" }

          lightRemovalJobs.remove(light.id)?.cancel()

          lightRemovalJobs[light.id] = launch {
            delay(30.seconds)
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

  private val _groupLightsChangedEvents = EventFlow<GroupLightChangedEvent>()
  val groupLightsChangedEvents get() = _groupLightsChangedEvents

  data class GroupLightChangedEvent(val group: Int, val lightId: String?)

  suspend fun flashLight(id: String) {
    wapps.first().parForEach { wapp ->
      withWapp(wapp.address) {
        val (id1, id2) = id.toApeLabsLightId()
        write(byteArrayOf(72, 10, id1, id2, 0))
      }
    }
  }

  suspend fun regroupLight(id: String, group: Int) = withContext(context) {
    wapps.first().parForEach { wapp ->
      withWapp(wapp.address) {
        val (id1, id2) = id.toApeLabsLightId()
        write(byteArrayOf(82, 10, id1, id2, 0, (group - 1).toByte(), 1, 0, 2, 13, 10))
        _groupLightsChangedEvents.tryEmit(GroupLightChangedEvent(group, id))
      }
    }
  }
}
