package com.ivianuu.apelabs.domain

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ivianuu.apelabs.data.Light
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.compose.getValue
import com.ivianuu.essentials.compose.setValue
import com.ivianuu.essentials.compose.stateFlow
import com.ivianuu.essentials.coroutines.EventFlow
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.coroutines.share
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.time.milliseconds
import com.ivianuu.essentials.time.minutes
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import com.ivianuu.injekt.inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

context(Logger, NamedCoroutineScope<AppScope>, WappRemote, WappRepository)
@Provide
@Scoped<AppScope>
class LightRepository(private val context: IOContext) {
  val lights: SharedFlow<List<Light>> = stateFlow {
    var lights by remember {
      mutableStateOf(
        inject<LightRepository>().lights.replayCache.firstOrNull() ?: emptyList()
      )
    }
    val lightRemovalJobs = remember { mutableMapOf<Int, Job>() }

    LaunchedEffect(true) {
      _groupLightsChangedEvents.collect { event ->
        lights = lights
          .map {
            if (it.id in event.lightIds) it.copy(group = event.group)
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
          if ((message.getOrNull(0)?.toInt() == 82 ||
                message.getOrNull(0)?.toInt() == 83) &&
            // -112 is the wapp
            message.getOrNull(1)?.toInt() != -112
          ) {
            val id = byteArrayOf(message[2], message[3], message[4], 0).toInt()
            val type = Light.Type.values().firstOrNull { it.id == message[1].toInt() }
            val light = Light(
              id = id,
              group = message.getOrNull(10)?.toInt()?.inc() ?: 1,
              battery = if (type != Light.Type.COIN) message[6] / 100f else null,
              type = type
            )

            val oldLight = lights.singleOrNull { it.id == id }
            if (message[0].toInt() == 82 && oldLight == null)
              _groupLightsChangedEvents.tryEmit(
                GroupLightsChangedEvent(
                  light.group,
                  listOf(light.id)
                )
              )

            light
          } else null
        }
        .collect { light ->
          log { "${light.id} ping" }

          lightRemovalJobs.remove(light.id)?.cancel()

          lightRemovalJobs[light.id] = launch {
            delay(1.minutes)
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
    .share(SharingStarted.WhileSubscribed(2000), 1)

  private val _groupLightsChangedEvents = EventFlow<GroupLightsChangedEvent>()
  val groupLightsChangedEvents get() = _groupLightsChangedEvents

  data class GroupLightsChangedEvent(val group: Int, val lightIds: List<Int>)

  suspend fun flashLights(ids: List<Int>) {
    if (ids.isEmpty()) return

    wapps.first().parForEach { wapp ->
      withWapp(wapp.address) {
        while (coroutineContext.isActive) {
          ids
            .shuffled()
            .forEach { id ->
              write(byteArrayOf(72, 100) + id.toByteArray().dropLast(1))
              delay(100.milliseconds)
            }
        }
      }
    }
  }

  suspend fun regroupLights(ids: List<Int>, group: Int) = withContext(context) {
    wapps.first().parForEach { wapp ->
      withWapp(wapp.address) {
        ids.forEach { id ->
          repeat(2) {
            write(
              byteArrayOf(82, 10) +
                  id.toByteArray()
                    .dropLast(1) +
                  byteArrayOf((group - 1).toByte(), 1, 0, 2, 13, 10)
            )
          }
        }
      }
    }

    _groupLightsChangedEvents.tryEmit(GroupLightsChangedEvent(group, ids))
  }
}
