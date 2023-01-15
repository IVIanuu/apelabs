/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.data

import com.ivianuu.apelabs.color.ApeColor
import com.ivianuu.apelabs.program.Program
import com.ivianuu.apelabs.util.randomId
import kotlinx.serialization.Serializable

@Serializable data class GroupConfig(
  val program: Program = Program(randomId(), listOf(Program.Item(randomId(), ApeColor()))),
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false,
  val blackout: Boolean = false
)

fun List<GroupConfig>.merge(): GroupConfig = when {
  isEmpty() -> GroupConfig()
  size == 1 -> single()
  else -> GroupConfig(
    program = when {
      all { a -> all { a.program == it.program } } -> first().program
      else -> Program("Color", listOf(Program.Item("Color", ApeColor())))
    },
    brightness = map { it.brightness }.average().toFloat(),
    speed = map { it.speed }.average().toFloat(),
    musicMode = all { it.musicMode },
    blackout = all { it.blackout }
  )
}

val GROUPS = (1..4).toList()
