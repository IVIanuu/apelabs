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
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.invoke
import com.ivianuu.essentials.time.milliseconds
import com.ivianuu.essentials.time.minutes
import com.ivianuu.essentials.time.seconds
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import com.ivianuu.injekt.inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Provide @Scoped<AppScope> class LightRepository(
  private val context: IOContext,
  private val logger: Logger,
  private val scope: NamedCoroutineScope<AppScope>,
  private val wappRemote: WappRemote,
  private val wappRepository: WappRepository
) {
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
      refreshes.collect {
        lights = emptyList()
      }
    }

    LaunchedEffect(true) {
      wappRepository.wapps
        .flatMapLatest { wapps ->
          callbackFlow {
            wapps.parForEach { wapp ->
              wappRemote.withWapp(wapp.address) {
                messages.collect {
                  trySend(it)
                }
              }
            }

            awaitClose()
          }
        }
        .onEach { logger { "on message ${it.contentToString()}" } }
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

            light to false
          } else null
        }
        .onEach { logger { "${it.first.id} ping" } }
        .onStart {
          // ensure that we launch a light removal job for existing lights
          lights.forEach { emit(it to true) }
        }
        .collect { (light, fromCache) ->
          lightRemovalJobs.remove(light.id)?.cancel()

          lightRemovalJobs[light.id] = launch {
            delay(if (fromCache) 17.seconds else 1.minutes)
            logger { "${light.id} remove light" }
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
    .shareIn(scope, SharingStarted.WhileSubscribed(2000), 1)

  private val _groupLightsChangedEvents = EventFlow<GroupLightsChangedEvent>()
  val groupLightsChangedEvents get() = _groupLightsChangedEvents

  data class GroupLightsChangedEvent(val group: Int, val lightIds: List<Int>)

  private val refreshes = EventFlow<Unit>()

  suspend fun refreshLights() {
    refreshes.emit(Unit)
  }

  suspend fun flashLights(ids: List<Int>) {
    if (ids.isEmpty()) return

    wappRepository.wapps.first().parForEach { wapp ->
      wappRemote.withWapp(wapp.address) {
        while (currentCoroutineContext().isActive) {
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
    wappRepository.wapps.first().parForEach { wapp ->
      wappRemote.withWapp(wapp.address) {
        ids.forEach { id ->
          // works more reliable if we send it twice:D
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
