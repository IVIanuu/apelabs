package com.ivianuu.apelabs.domain

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ivianuu.apelabs.data.Light
import com.ivianuu.essentials.Eager
import com.ivianuu.essentials.compose.compositionStateFlow
import com.ivianuu.essentials.coroutines.CoroutineContexts
import com.ivianuu.essentials.coroutines.EventFlow
import com.ivianuu.essentials.coroutines.ScopedCoroutineScope
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.ui.UiScope
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Provide @Eager<UiScope> class LightRepository(
  private val coroutineContexts: CoroutineContexts,
  private val logger: Logger,
  scope: ScopedCoroutineScope<UiScope>,
  private val wappRemote: WappRemote,
  private val wappRepository: WappRepository,
) {
  val lights: Flow<List<Light>> = scope.compositionStateFlow {
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
          wapps.parForEach { wapp ->
            wappRemote.withWapp<Unit>(wapp.address) {
              messages.collect {
                emit(it)
              }
            }
          }
        }
        .onEach { logger.log { "on message ${it.contentToString()}" } }
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
        .onEach { logger.log { "${it.first.id} ping" } }
        .onStart {
          // ensure that we launch a light removal job for existing lights
          lights.forEach { emit(it to true) }
        }
        .collect { (light, fromCache) ->
          lightRemovalJobs.remove(light.id)?.cancel()

          lightRemovalJobs[light.id] = launch {
            delay(if (fromCache) 17.seconds else 35.seconds)
            logger.log { "${light.id} remove light" }
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

  suspend fun regroupLights(ids: List<Int>, group: Int) = withContext(coroutineContexts.io) {
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

    _groupLightsChangedEvents.emit(GroupLightsChangedEvent(group, ids))
  }
}
