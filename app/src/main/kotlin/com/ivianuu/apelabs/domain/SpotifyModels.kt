package com.ivianuu.apelabs.domain

import androidx.compose.runtime.Immutable
import com.ivianuu.essentials.time.Clock
import com.ivianuu.injekt.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable @Immutable data class PlaybackState(
  val id: String,
  val isPlaying: Boolean,
  val position: Duration,
  val duration: Duration,
  val timestamp: Duration
)

fun PlaybackState.currentPosition(@Inject clock: Clock): Duration =
  if (!isPlaying) position else (position + clock() - timestamp)
    .coerceIn(Duration.ZERO, duration)

fun PlaybackState.isCompleted(@Inject clock: Clock): Boolean =
  !isPlaying && (currentPosition() == duration || currentPosition() == Duration.ZERO)

@Serializable @Immutable data class ApiAudioAnalysis(
  @SerialName("track") val track: ApiAudioAnalysisTrack,
  @SerialName("bars") val bars: List<ApiBar>,
  @SerialName("beats") val beats: List<ApiBeat>,
  @SerialName("sections") val sections: List<ApiSection>,
  @SerialName("segments") val segments: List<ApiSegment>
)

@Serializable @Immutable data class ApiAudioAnalysisTrack(
  @SerialName("duration") val duration: Double,
  @SerialName("tempo") val tempo: Float,
  @SerialName("end_of_fade_in") val endOfFadeIn: Double,
  @SerialName("start_of_fade_out") val startOfFadeOut: Double
)

@Serializable @Immutable data class ApiBar(
  @SerialName("start") val start: Double,
  @SerialName("duration") val duration: Double
)

@Serializable @Immutable data class ApiBeat(
  @SerialName("start") val start: Double,
  @SerialName("duration") val duration: Double
)

@Serializable @Immutable data class ApiSection(
  @SerialName("start") val start: Double,
  @SerialName("duration") val duration: Double
)

@Serializable @Immutable data class ApiSegment(
  @SerialName("start") val start: Double,
  @SerialName("duration") val duration: Double
)
