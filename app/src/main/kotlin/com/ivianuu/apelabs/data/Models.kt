/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.data

import android.bluetooth.BluetoothDevice
import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.android.prefs.DataStoreModule
import com.ivianuu.essentials.cast
import com.ivianuu.essentials.time.seconds
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable
import kotlin.time.Duration

data class Wapp(val address: String, val name: String)

fun BluetoothDevice.toWapp() = Wapp(address, alias ?: name)

fun BluetoothDevice.isWapp() = (alias ?: name).let {
  it?.startsWith("APE-") == true
}

fun BluetoothDevice.debugName() = "[${alias ?: name} ~ $address]"

fun Wapp.debugName() = "[$name ~ $address]"

data class Light(val id: String, val group: Int)

fun lightIdOf(id1: Byte, id2: Byte) = "$id1:$id2"

fun String.toApeLabsId() = split(":").let { it[0].toByte() to it[1].toByte() }

@Serializable data class GroupConfig(
  val program: ProgramConfig = ProgramConfig.SingleColor(LightColor()),
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false
)

@Serializable sealed interface ProgramConfig {
  @Serializable data class SingleColor(val color: LightColor) : ProgramConfig
  @Serializable data class MultiColor(val items: List<Item>) : ProgramConfig {
    @Serializable data class Item(
      val color: LightColor,
      val fade: Duration = 1.seconds,
      val hold: Duration = 1.seconds
    )
  }

  @Serializable object Rainbow : ProgramConfig
}

@Serializable data class LightColor(
  val red: Float = 0f,
  val green: Float = 0f,
  val blue: Float = 0f,
  val white: Float = 1f
)

fun LightColor.toColor() = Color(red, green, blue)

fun Color.toLightColor(white: Float = 0f) = LightColor(red, green, blue, white)

fun List<GroupConfig>.merge(): GroupConfig = when {
  isEmpty() -> GroupConfig()
  size == 1 -> single()
  else -> GroupConfig(
    program = when {
      all { it.program is ProgramConfig.SingleColor } -> {
        val colors = map { it.program.cast<ProgramConfig.SingleColor>().color }
        ProgramConfig.SingleColor(
          color = LightColor(
            red = colors.map { it.red }.average().toFloat(),
            green = colors.map { it.green }.average().toFloat(),
            blue = colors.map { it.blue }.average().toFloat(),
            white = colors.map { it.white }.average().toFloat()
          )
        )
      }
      all { a -> all { a.program == it.program } } -> first().program
      else -> ProgramConfig.SingleColor(LightColor())
    },
    brightness = map { it.brightness }.average().toFloat(),
    speed = map { it.speed }.average().toFloat(),
    musicMode = all { it.musicMode }
  )
}

@Serializable data class ApeLabsPrefs(
  val selectedGroups: Set<Int> = emptySet(),
  val groupConfigs: Map<Int, GroupConfig> = emptyMap()
) {
  companion object {
    @Provide val prefModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}

val GROUPS = (1..4).toList()
