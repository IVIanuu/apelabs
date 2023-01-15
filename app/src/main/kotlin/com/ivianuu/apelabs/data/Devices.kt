package com.ivianuu.apelabs.data

import android.bluetooth.BluetoothDevice

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

fun String.toApeLabsLightId() = split(":").let { it[0].toByte() to it[1].toByte() }
