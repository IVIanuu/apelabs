/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.data

import android.bluetooth.BluetoothDevice
import com.ivianuu.essentials.android.prefs.DataStoreModule
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable

data class Wapp(val address: String, val name: String)

fun BluetoothDevice.toWapp() = Wapp(address, alias ?: name)

fun BluetoothDevice.isWapp() = (alias ?: name).let {
  it?.startsWith("APE-") == true
}

fun BluetoothDevice.debugName() = "[${alias ?: name} ~ $address]"

fun Wapp.debugName() = "[$name ~ $address]"

data class Light(
  val id: String,
  val group: Int
)

fun lightIdOf(id1: Byte, id2: Byte) = "$id1:$id2"

fun String.toApeLabsId() = split(":").let { it[0].toByte() to it[1].toByte() }

@Serializable data class GroupConfig(
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false
)

fun List<GroupConfig>.merge(): GroupConfig = when {
  isEmpty() -> GroupConfig()
  size == 1 -> single()
  else -> GroupConfig(
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
