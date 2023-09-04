package com.ivianuu.apelabs.data

import androidx.compose.runtime.Immutable
import com.ivianuu.essentials.time.Clock
import com.ivianuu.injekt.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable @Immutable data class SpotifyAudioAnalysis(
  @SerialName("track") val track: SpotifyAudioAnalysisTrack,
  @SerialName("bars") val bars: List<SpotifyBar>,
  @SerialName("beats") val beats: List<SpotiyBeat>,
  @SerialName("sections") val sections: List<SpotifySection>,
  @SerialName("segments") val segments: List<SpotifySegment>
)

@Serializable @Immutable data class SpotifyAudioAnalysisTrack(
  @SerialName("duration") val duration: Double,
  @SerialName("tempo") val tempo: Float,
  @SerialName("end_of_fade_in") val endOfFadeIn: Double,
  @SerialName("start_of_fade_out") val startOfFadeOut: Double
)

@Serializable @Immutable data class SpotifyBar(
  @SerialName("start") val start: Double,
  @SerialName("duration") val duration: Double
)

@Serializable @Immutable data class SpotiyBeat(
  @SerialName("start") val start: Double,
  @SerialName("duration") val duration: Double
)

@Serializable @Immutable data class SpotifySection(
  @SerialName("start") val start: Double,
  @SerialName("duration") val duration: Double
)

@Serializable @Immutable data class SpotifySegment(
  @SerialName("start") val start: Double,
  @SerialName("duration") val duration: Double
)

@Serializable @Immutable data class SpotifyPlaybackState(
  val id: String,
  val isPlaying: Boolean,
  val position: Duration,
  val duration: Duration,
  val timestamp: Duration
)

fun SpotifyPlaybackState.currentPosition(@Inject clock: Clock): Duration =
  if (!isPlaying) position else (position + clock() - timestamp)
    .coerceIn(Duration.ZERO, duration)
