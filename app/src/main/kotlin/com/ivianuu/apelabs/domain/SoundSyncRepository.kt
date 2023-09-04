package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.SpotifyAudioAnalysis
import com.ivianuu.apelabs.data.SpotifyPlaybackState
import com.ivianuu.apelabs.data.currentPosition
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.coroutines.ScopedCoroutineScope
import com.ivianuu.essentials.coroutines.bracket
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.time.Clock
import com.ivianuu.essentials.util.BroadcastsFactory
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Provide @Scoped<AppScope> class SoundSyncRepository(
  broadcastsFactory: BroadcastsFactory,
  @Inject private val clock: Clock,
  private val json: Json,
  private val logger: Logger,
  private val pref: DataStore<ApeLabsPrefs>,
  scope: ScopedCoroutineScope<AppScope>
) {
  private val soundSyncInfo: Flow<Pair<SpotifyAudioAnalysis, SpotifyPlaybackState>?> =
    broadcastsFactory("spotify.current_meta")
      .map {
        json.decodeFromString<SpotifyAudioAnalysis>(it.getStringExtra("audio_analysis")!!) to
            json.decodeFromString<SpotifyPlaybackState>(it.getStringExtra("playback_state")!!)
      }
      .stateIn(scope, SharingStarted.Eagerly, null)

  private val soundSyncRequests = MutableStateFlow(0)

  suspend fun <R> soundSynced(writes: Int, block: suspend () -> R): R {
    if (!pref.data.first().soundSync) return block()
      .also { logger.log { "sound sync disabled" } }

    val (audioAnalysis, playbackState) = soundSyncInfo.first()
      ?: return block()
        .also { logger.log { "no sound sync meta" } }

    if (!playbackState.isPlaying) return block()
      .also { logger.log { "not playing" } }

    val id = UUID.randomUUID()
    bracket(
      acquire = { soundSyncRequests.update { it + writes } },
      use = {
        try {
          soundSyncRequests.collectLatest { requests ->
            val currentPosition = playbackState.currentPosition()

            val latency = (WappServer.averageWriteDuration * requests) +
                200.milliseconds // bluetooth

            logger.log { "$id current position $currentPosition latency $latency requests $requests" }

            fun Duration.toDelay() = this - currentPosition - latency

            val nextTimestamp = audioAnalysis.sections
              .map { it.start.seconds }
              .filter { it > currentPosition + latency }
              .also {
                logger.log { "target ${currentPosition + latency} sections candidates $it" }
              }
              .firstOrNull()
              ?.takeIf { it.toDelay() < 5.seconds } ?:
            audioAnalysis.bars
              .map { it.start.seconds }
              .firstOrNull { it > currentPosition + latency }
            ?: run {
              logger.log { "no next timestamp" }
              throw ShortCircuitException()
            }

            logger.log { "sound sync delay ${nextTimestamp.toDelay()}" }

            delay(nextTimestamp.toDelay())

            throw ShortCircuitException()
          }
        } catch (e: ShortCircuitException) {
        }
      },
      release = { _, _ -> soundSyncRequests.update { it - writes } }
    )

    return block()
  }

  private class ShortCircuitException : RuntimeException()
}
