package com.ivianuu.apelabs.domain

import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import kotlin.math.abs
import kotlin.math.sqrt

context(Logger) class Wuf(visualizer: Visualizer) {
  private var runningSoundAvg = DoubleArray(3)
  private var currentAvgEnergyOneSec = doubleArrayOf(-1.0, -1.0, -1.0)
  private var numberOfSamplesInOneSec = 0
  private var systemTimeStartSec = 0L

  private val captureListener: OnDataCaptureListener = object : OnDataCaptureListener {
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
      var energySum = 0
      energySum += abs(bytes[0].toInt())
      var k = 2
      val captureSize = (visualizer.captureSize / 2).toDouble()
      val sampleRate = visualizer.samplingRate / 2000
      var nextFrequency = k / 2 * sampleRate / captureSize
      while (nextFrequency < LOW_FREQUENCY) {
        energySum += sqrt(
          (bytes[k] * bytes[k]
              * (bytes[k + 1] * bytes[k + 1])).toDouble()
        ).toInt()
        k += 2
        nextFrequency = k / 2 * sampleRate / captureSize
      }
      var sampleAvgAudioEnergy = energySum.toDouble() / (k * 1.0 / 2.0)
      runningSoundAvg[0] += sampleAvgAudioEnergy
      if (sampleAvgAudioEnergy > currentAvgEnergyOneSec[0] && currentAvgEnergyOneSec[0] > 0) {
        fireBeatDetectedLowEvent(sampleAvgAudioEnergy)
      }
      energySum = 0
      while (nextFrequency < MID_FREQUENCY) {
        energySum += sqrt(
          (bytes[k] * bytes[k]
              * (bytes[k + 1] * bytes[k + 1])).toDouble()
        ).toInt()
        k += 2
        nextFrequency = k / 2 * sampleRate / captureSize
      }
      sampleAvgAudioEnergy = energySum.toDouble() / (k * 1.0 / 2.0)
      runningSoundAvg[1] += sampleAvgAudioEnergy
      if (sampleAvgAudioEnergy > currentAvgEnergyOneSec[1] && currentAvgEnergyOneSec[1] > 0) {
        fireBeatDetectedMidEvent(sampleAvgAudioEnergy)
      }
      energySum = abs(bytes[1].toInt())
      while (nextFrequency < HIGH_FREQUENCY && k < bytes.size) {
        energySum += sqrt(
          (bytes[k] * bytes[k]
              * (bytes[k + 1] * bytes[k + 1])).toDouble()
        ).toInt()
        k += 2
        nextFrequency = k / 2 * sampleRate / captureSize
      }
      sampleAvgAudioEnergy = energySum.toDouble() / (k * 1.0 / 2.0)
      runningSoundAvg[2] += sampleAvgAudioEnergy
      if (sampleAvgAudioEnergy > currentAvgEnergyOneSec[2] && currentAvgEnergyOneSec[2] > 0) {
        fireBeatDetectedHighEvent(sampleAvgAudioEnergy)
      }
      numberOfSamplesInOneSec++
      if (System.currentTimeMillis() - systemTimeStartSec > 1000) {
        currentAvgEnergyOneSec[0] = (runningSoundAvg[0]
            / numberOfSamplesInOneSec)
        currentAvgEnergyOneSec[1] = (runningSoundAvg[1]
            / numberOfSamplesInOneSec)
        currentAvgEnergyOneSec[2] = (runningSoundAvg[2]
            / numberOfSamplesInOneSec)
        numberOfSamplesInOneSec = 0
        runningSoundAvg[0] = 0.0
        runningSoundAvg[1] = 0.0
        runningSoundAvg[2] = 0.0
        systemTimeStartSec = System.currentTimeMillis()
      }
    }
  }

  init {
    visualizer.enabled = false

    visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]

    visualizer.setDataCaptureListener(
      captureListener,
      Visualizer.getMaxCaptureRate() / 2, false, true
    )

    visualizer.enabled = true

    systemTimeStartSec = System.currentTimeMillis()
  }

  private fun fireBeatDetectedLowEvent(power: Double) {
    log { "on low $power" }
  }

  private fun fireBeatDetectedMidEvent(power: Double) {
    log { "on mid $power" }
  }

  private fun fireBeatDetectedHighEvent(power: Double) {
    log { "on high $power" }
  }

  companion object {
    private const val LOW_FREQUENCY = 300
    private const val MID_FREQUENCY = 2500
    private const val HIGH_FREQUENCY = 10000
  }
}