package com.ivianuu.apelabs.domain

import android.media.audiofx.AudioEffect
import android.media.audiofx.Visualizer
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.android.prefs.PrefModule
import com.ivianuu.essentials.catch
import com.ivianuu.essentials.coroutines.infiniteEmptyFlow
import com.ivianuu.essentials.coroutines.share
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.onFailure
import com.ivianuu.essentials.onSuccess
import com.ivianuu.essentials.time.Clock
import com.ivianuu.essentials.time.seconds
import com.ivianuu.essentials.util.BroadcastsFactory
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.NamedCoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.sqrt

context(BroadcastsFactory, Logger, NamedCoroutineScope<AppScope>)
    @Provide
fun audioEvents(audioSessionPref: DataStore<AudioSessionPrefs>): @Scoped<AppScope> Flow<AudioEvent> =
  audioSessions(audioSessionPref)
    .flatMapLatest {
      if (it.isEmpty()) infiniteEmptyFlow()
      else audioEvents(it.last(), 120)
    }
    .share(SharingStarted.WhileSubscribed(), 0)

context(BroadcastsFactory)
    private fun audioSessions(audioSessionPref: DataStore<AudioSessionPrefs>) =
  callbackFlow<List<Int>> {
    val audioSessions = mutableListOf<Int>()

    broadcasts(
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
        audioSessions.removeAll { it == sessionId }

        when (event) {
          AudioSessionEvent.START -> {
            audioSessions += sessionId
            audioSessionPref.updateData {
              copy(
                knownAudioSessions = knownAudioSessions.toMutableSet().apply { add(sessionId) })
            }
          }

          AudioSessionEvent.STOP -> {
            audioSessionPref.updateData {
              copy(
                knownAudioSessions = knownAudioSessions.toMutableSet()
                  .apply { remove(sessionId) })
            }
          }
        }

        send(audioSessions.toList())
      }
  }.distinctUntilChanged()

private enum class AudioSessionEvent {
  START, STOP
}

@Serializable
data class AudioSessionPrefs(val knownAudioSessions: Set<Int> = emptySet()) {
  companion object {
    @Provide
    val prefModule = PrefModule { AudioSessionPrefs() }
  }
}

data class AudioEvent(val power: Double)

context(Clock, Logger) fun audioEvents(
  sessionId: Int,
  frequency: Int
) = callbackFlow<AudioEvent> {
  var visualizer: Visualizer? = null
  var attempt = 0

  while (visualizer == null && attempt < 5) {
    catch { Visualizer(sessionId) }
      .onFailure { it.printStackTrace() }
      .onSuccess { visualizer = it }
    attempt++
  }

  var runningSoundAvg = 0.0
  var currentAvgEnergy = 0.0
  var samplesPerSecond = 0
  var lastTimestamp = now()

  val captureListener: Visualizer.OnDataCaptureListener = object :
    Visualizer.OnDataCaptureListener {
    override fun onWaveFormDataCapture(
      visualizer: Visualizer,
      bytes: ByteArray,
      samplingRate: Int
    ) {
    }

    override fun onFftDataCapture(
      visualizer: Visualizer,
      bytes: ByteArray,
      samplingRate: Int
    ) {
      catch { visualizer.samplingRate }.onFailure { return }

      var energySum = abs(bytes[0].toInt())

      var k = 2

      val captureSize = (visualizer.captureSize / 2).toDouble()

      val sampleRate = visualizer.samplingRate / 2000

      var nextFrequency = k / 2 * sampleRate / captureSize

      while (nextFrequency < frequency) {
        energySum += sqrt(
          (bytes[k] * bytes[k] * (bytes[k + 1] * bytes[k + 1])).toDouble()
        ).toInt()
        k += 2
        nextFrequency = k / 2 * sampleRate / captureSize
      }

      val sampleAvgAudioEnergy = energySum.toDouble() / (k * 1.0 / 2.0)
      runningSoundAvg += sampleAvgAudioEnergy

      if (sampleAvgAudioEnergy > currentAvgEnergy && currentAvgEnergy > 0)
        trySend(AudioEvent(sampleAvgAudioEnergy))

      samplesPerSecond++

      val now = now()
      if (now - lastTimestamp > 1.seconds) {
        currentAvgEnergy = (runningSoundAvg / samplesPerSecond)
        samplesPerSecond = 0
        runningSoundAvg = 0.0
        lastTimestamp = now
      }
    }
  }

  visualizer!!.enabled = false
  visualizer!!.captureSize = Visualizer.getCaptureSizeRange()[1]
  visualizer!!.setDataCaptureListener(
    captureListener,
    Visualizer.getMaxCaptureRate() / 2, false, true
  )
  visualizer!!.enabled = true

  awaitClose {
    catch {
      visualizer!!.enabled = false
      visualizer!!.release()
    }.onFailure { it.printStackTrace() }
  }
}
