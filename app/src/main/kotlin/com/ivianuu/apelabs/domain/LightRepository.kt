package com.ivianuu.apelabs.domain

import androidx.compose.runtime.*
import arrow.fx.coroutines.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.coroutines.*
import com.ivianuu.essentials.logging.*
import com.ivianuu.essentials.ui.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Provide @Eager<UiScope> class LightRepository(
  private val coroutineContexts: CoroutineContexts,
  private val logger: Logger,
  scope: ScopedCoroutineScope<UiScope>,
  private val wappRemote: WappRemote,
  private val wappRepository: WappRepository,
) {
  val lights: Flow<List<Light>> = scope.moleculeStateFlow {
    var lights by remember { mutableStateOf(emptyList<Light>()) }
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
        .transformLatest { wapps ->
          wapps.parMap { wapp ->
            wappRemote.withWapp<Unit>(wapp.address) {
              messages.collect {
                emit(it)
              }
            }
          }
        }
        .onEach { logger.d { "on message ${it.contentToString()}" } }
        .mapNotNull { message ->
          if ((message.getOrNull(0)?.toInt() == 82 ||
                message.getOrNull(0)?.toInt() == 83) &&
            // -112 is the wapp
            message.getOrNull(1)?.toInt() != -112
          ) {
            val id = byteArrayOf(message[2], message[3], message[4], 0).toInt()
            val type = Light.Type.entries.firstOrNull { it.id == message[1].toInt() }
            val light = Light(
              id = id,
              group = message.getOrNull(10)?.toInt()?.inc() ?: 1,
              battery = if (type != Light.Type.COIN) message[6] / 100f else null,
              type = type
            )

            val oldLight = lights.singleOrNull { it.id == id }
            if (message[0].toInt() == 82 && oldLight == null)
              _groupLightsChangedEvents.emit(
                GroupLightsChangedEvent(
                  light.group,
                  listOf(light.id)
                )
              )

            light to false
          } else null
        }
        .onEach { logger.d { "${it.first.id} ping" } }
        .onStart {
          // ensure that we launch a light removal job for existing lights
          lights.forEach { emit(it to true) }
        }
        .collect { (light, fromCache) ->
          lightRemovalJobs.remove(light.id)?.cancel()

          lightRemovalJobs[light.id] = launch {
            delay(if (fromCache) 17.seconds else 35.seconds)
            logger.d { "${light.id} remove light" }
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

  private val _groupLightsChangedEvents = EventFlow<GroupLightsChangedEvent>()
  val groupLightsChangedEvents get() = _groupLightsChangedEvents

  data class GroupLightsChangedEvent(val group: Int, val lightIds: List<Int>)

  private val refreshes = EventFlow<Unit>()

  suspend fun refreshLights() {
    refreshes.emit(Unit)
  }

  suspend fun flashLights(ids: List<Int>) {
    if (ids.isEmpty()) return

    wappRepository.wapps.first().parMap { wapp ->
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

  suspend fun regroupLights(ids: List<Int>, group: Int) = withContext(coroutineContexts.io) {
    wappRepository.wapps.first().parMap { wapp ->
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

    _groupLightsChangedEvents.emit(GroupLightsChangedEvent(group, ids))
  }
}
