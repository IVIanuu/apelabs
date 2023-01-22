package com.ivianuu.apelabs.data

import android.bluetooth.BluetoothDevice

data class Wapp(
  val id: Int,
  val address: String,
  val name: String
)

data class WappState(
  val id: Int? = null,
  val isConnected: Boolean = false,
  val battery: Float? = null
)

fun BluetoothDevice.toWapp() = Wapp(
  (alias ?: name).replaceBeforeLast("-", "")
    .drop(1)
    .toInt(),
  address,
  alias ?: name
)

fun BluetoothDevice.isWapp() = (alias ?: name).let {
  it?.startsWith("APE-") == true
}

fun BluetoothDevice.debugName() = "[${alias ?: name} ~ $address]"

fun Wapp.debugName() = "[$name ~ $address]"

data class Light(
  val id: Int,
  val group: Int,
  val battery: Float? = null,
  val type: Type?
) {
  enum class Type(val id: Int) {
    COIN(-96), LIGHTCAN(48)
  }
}
