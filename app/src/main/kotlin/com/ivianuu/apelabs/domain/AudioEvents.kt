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
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.onFailure
import com.ivianuu.essentials.onSuccess
import com.ivianuu.essentials.time.Clock
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
import kotlin.random.Random

context(BroadcastsFactory, Logger, NamedCoroutineScope<AppScope>)
    @Provide
fun audioEvents(sessions: Flow<List<Int>>): @Scoped<AppScope> Flow<AudioEvent> =
  sessions
    .flatMapLatest {
      if (it.isEmpty()) infiniteEmptyFlow()
      else audioEvents(it.last(), listOf(80))
    }
    .share(SharingStarted.WhileSubscribed(), 0)

context(BroadcastsFactory, NamedCoroutineScope<AppScope>)
    @Provide
fun audioSessions(pref: DataStore<AudioSessionPrefs>): @Scoped<AppScope> Flow<List<Int>> =
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
        pref.data.first().knownAudioSessions.forEach {
          emit(AudioSessionEvent.START to it)
        }
      }
      .collect { (event, sessionId) ->
        audioSessions.removeAll { it == sessionId }

        when (event) {
          AudioSessionEvent.START -> {
            audioSessions += sessionId
            pref.updateData {
              copy(
                knownAudioSessions = knownAudioSessions.toMutableSet().apply { add(sessionId) })
            }
          }

          AudioSessionEvent.STOP -> {
            pref.updateData {
              copy(
                knownAudioSessions = knownAudioSessions.toMutableSet()
                  .apply { remove(sessionId) })
            }
          }
        }

        send(audioSessions.toList())
      }
  }.distinctUntilChanged()
    .share(SharingStarted.WhileSubscribed(), 1)

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

object AudioEvent

context(Clock, Logger) fun audioEvents(
  sessionId: Int,
  frequencies: List<Int>
) = callbackFlow<AudioEvent> {
  var visualizer: Visualizer? = null
  var attempt = 0

  while (visualizer == null && attempt < 5) {
    catch { Visualizer(sessionId) }
      .onFailure { it.printStackTrace() }
      .onSuccess { visualizer = it }
    attempt++
  }

  val windowRMS = 16
  val window = mutableListOf<Double>()
  val sensitivity = 2f

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
      catch { visualizer.samplingRate }.onFailure {
        it.printStackTrace()
        return
      }

      var energySum = 0
      var totalPower = 0.0
      var k = 2
      val captureSize = visualizer.captureSize / 2.0
      val sampleRate = visualizer.samplingRate / 2000

      var nextFrequency = k / 2 * sampleRate / captureSize

      energySum += abs(bytes[0].toInt())
      while (nextFrequency <= 300) {
        energySum += sqrt(
          (bytes[k] * bytes[k]
              * (bytes[k + 1] * bytes[k + 1])).toDouble()
        ).toInt()
        k += 2
        nextFrequency = k / 2 * sampleRate / captureSize
      }

      var sampleAvgAudioEnergy = energySum.toDouble() / (k * 1.0 / 2.0)
      val lowPower = sampleAvgAudioEnergy
      totalPower += lowPower

      while (nextFrequency < 2500) {
        energySum += sqrt(
          (bytes[k] * bytes[k]
              * (bytes[k + 1] * bytes[k + 1])).toDouble()
        ).toInt()
        k += 2
        nextFrequency = k / 2 * sampleRate / captureSize
      }
      sampleAvgAudioEnergy = energySum.toDouble() / (k * 1.0 / 2.0)
      val medPower = sampleAvgAudioEnergy
      //totalPower += medPower

      energySum = abs(bytes[1].toInt())
      while (nextFrequency < 10000 && k < bytes.size) {
        energySum += sqrt(
          (bytes[k] * bytes[k]
              * (bytes[k + 1] * bytes[k + 1])).toDouble()
        ).toInt()
        k += 2
        nextFrequency = k / 2 * sampleRate / captureSize
      }
      sampleAvgAudioEnergy = energySum.toDouble() / (k * 1.0 / 2.0)
      val highPower = sampleAvgAudioEnergy
      //totalPower += highPower

      window.add(totalPower)

      if (window.size > windowRMS) {
        window.removeAt(0)

        val totalAverage = window.average()
        val current = totalPower

        if (current >= totalAverage * sensitivity) {
          trySend(AudioEvent)

          log { "beat yes $current $totalAverage" }

          repeat(window.size) {
            window[it] = current * sensitivity * 2f
          }
        } else {
          log { "beat nop $current $totalAverage" }
        }
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
