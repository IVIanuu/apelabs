package com.ivianuu.apelabs.domain

import android.media.audiofx.AudioEffect
import android.media.audiofx.Visualizer
import com.ivianuu.essentials.AppContext
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.android.prefs.PrefModule
import com.ivianuu.essentials.app.AppForegroundScope
import com.ivianuu.essentials.app.ScopeWorker
import com.ivianuu.essentials.catch
import com.ivianuu.essentials.coroutines.guarantee
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.onFailure
import com.ivianuu.essentials.onSuccess
import com.ivianuu.essentials.util.BroadcastsFactory
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Provide
fun audioSessionWorker(
  audioSessionPref: DataStore<AudioSessionPrefs>,
  broadcastsFactory: BroadcastsFactory,
  context: AppContext,
  logger: Logger
) = ScopeWorker<AppForegroundScope> {
  // audioSessions().collect()
}

@Serializable
data class AudioSessionPrefs(
  val knownAudioSessions: Set<Int> = emptySet()
) {
  companion object {
    @Provide
    val prefModule = PrefModule { AudioSessionPrefs() }
  }
}

private fun audioSessions(
  @Inject broadcastsFactory: BroadcastsFactory,
  @Inject logger: Logger,
  @Inject audioSessionPref: DataStore<AudioSessionPrefs>
): Flow<Map<Int, AudioSession>> = channelFlow {
  val audioSessions = mutableMapOf<Int, AudioSession>()

  guarantee(
    {
      broadcastsFactory.broadcasts(
        AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION,
        AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
      )
        .map {
          when (it.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> AudioSessionEvent.START
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> AudioSessionEvent.STOP
            else -> throw AssertionError()
          } to it.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        }
        .onStart {
          audioSessionPref.data.first().knownAudioSessions.forEach {
            emit(AudioSessionEvent.START to it)
          }
        }
        .collect { (event, sessionId) ->
          when (event) {
            AudioSessionEvent.START -> {
              var session: AudioSession? = null
              var attempt = 0

              while (attempt < 5) {
                catch { AudioSession(sessionId) }
                  .onFailure { it.printStackTrace() }
                  .onSuccess { session = it }

                attempt++
              }

              if (session != null) {
                session!!.start()
                audioSessions[sessionId] = session!!
                send(audioSessions.toMap())
                audioSessionPref.updateData {
                  copy(
                    knownAudioSessions = knownAudioSessions.toMutableSet().apply { add(sessionId) })
                }
              } else {
                audioSessionPref.updateData {
                  copy(
                    knownAudioSessions = knownAudioSessions.toMutableSet()
                      .apply { remove(sessionId) })
                }
              }
            }

            AudioSessionEvent.STOP -> {
              audioSessions.remove(sessionId)
                ?.also { it.release() }
              audioSessionPref.updateData {
                copy(
                  knownAudioSessions = knownAudioSessions.toMutableSet()
                    .apply { remove(sessionId) })
              }
              send(audioSessions.toMap())
            }
          }
        }
    },
    {
      log { "stop all sessions $audioSessions" }
      audioSessions.values.forEach { it.release() }
    }
  )
}

private enum class AudioSessionEvent {
  START, STOP
}

class AudioSession(
  private val sessionId: Int,
  @Inject val logger: Logger,
  @Inject val coroutineScope: CoroutineScope
) {
  private val visualizer = try {
    Visualizer(sessionId)
  } catch (e: Throwable) {
    // todo injekt bug
    with(logger) {
      log { "$sessionId couln't create" }
    }
    throw IllegalStateException("Couldn't create effect for $sessionId")
  }

  private var job: Job? = null

  init {
    Wuf(visualizer)
  }

  fun start() {
    log { "$sessionId -> start" }
  }

  fun release() {
    log { "$sessionId -> stop" }
    job?.cancel()
    catch { visualizer.release() }
  }
}
