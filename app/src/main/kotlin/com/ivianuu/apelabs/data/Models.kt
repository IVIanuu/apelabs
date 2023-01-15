/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.data

import android.bluetooth.BluetoothDevice
import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.android.prefs.DataStoreModule
import com.ivianuu.essentials.cast
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.time.seconds
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable
import kotlin.time.Duration

data class Wapp(val address: String, val name: String)

data class WappState(
  val isConnected: Boolean = false,
  val battery: Float? = null
)

fun BluetoothDevice.toWapp() = Wapp(address, alias ?: name)

fun BluetoothDevice.isWapp() = (alias ?: name).let {
  it?.startsWith("APE-") == true
}

fun BluetoothDevice.debugName() = "[${alias ?: name} ~ $address]"

fun Wapp.debugName() = "[$name ~ $address]"

data class Light(
  val id: String,
  val group: Int,
  val battery: Float? = null,
  val type: Type?
) {
  enum class Type(val id: Int) {
    COIN(-96)
  }
}

fun lightIdOf(id1: Byte, id2: Byte) = "$id1:$id2"

fun String.toApeLabsId() = split(":").let { it[0].toByte() to it[1].toByte() }

@Serializable data class GroupConfig(
  val program: Program = Program.SingleColor(LightColor()),
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false,
  val blackout: Boolean = false
)

@Serializable sealed interface Program {
  @Serializable data class SingleColor(val color: LightColor = LightColor()) : Program
  @Serializable data class MultiColor(val items: List<Item> = listOf(Item())) : Program {
    init {
      check(items.size in ITEM_RANGE)
    }

    @Serializable data class Item(
      val color: LightColor = LightColor(),
      val fade: Duration = 1.seconds,
      val hold: Duration = 1.seconds
    )

    companion object {
      val ITEM_RANGE = 1..4
    }
  }

  @Serializable object Rainbow : Program
}

@Serializable data class LightColor(
  val red: Float = 0f,
  val green: Float = 0f,
  val blue: Float = 0f,
  val white: Float = 0f
)

fun LightColor.toColor() = Color(red, green, blue)
  .overlay(Color.White.copy(alpha = white))

private fun Color.overlay(overlay: Color): Color {
  val alphaSum = alpha + overlay.alpha
  return Color(
    (red * alpha + overlay.red * overlay.alpha) / alphaSum,
    (green * alpha + overlay.green * overlay.alpha) / alphaSum,
    (blue * alpha + overlay.blue * overlay.alpha) / alphaSum,
    alphaSum.coerceIn(0f, 1f),
  )
}

fun List<GroupConfig>.merge(): GroupConfig = when {
  isEmpty() -> GroupConfig()
  size == 1 -> single()
  else -> GroupConfig(
    program = when {
      all { it.program is Program.SingleColor } -> {
        val colors = map { it.program.cast<Program.SingleColor>().color }
        Program.SingleColor(
          color = LightColor(
            red = colors.map { it.red }.average().toFloat(),
            green = colors.map { it.green }.average().toFloat(),
            blue = colors.map { it.blue }.average().toFloat(),
            white = colors.map { it.white }.average().toFloat()
          )
        )
      }
      all { a -> all { a.program == it.program } } -> first().program
      else -> Program.SingleColor(LightColor())
    },
    brightness = map { it.brightness }.average().toFloat(),
    speed = map { it.speed }.average().toFloat(),
    musicMode = all { it.musicMode },
    blackout = all { it.blackout }
  )
}

@Serializable data class ApeLabsPrefs(
  val selectedGroups: Set<Int> = emptySet(),
  val groupConfigs: Map<Int, GroupConfig> = emptyMap(),
  val colors: Map<String, LightColor> = emptyMap(),
  val programs: Map<String, Program.MultiColor> = emptyMap()
) {
  companion object {
    @Provide val prefModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}

@Provide @JvmInline value class ApeLabsPrefsContext(val pref: DataStore<ApeLabsPrefs>)

val GROUPS = (1..4).toList()
