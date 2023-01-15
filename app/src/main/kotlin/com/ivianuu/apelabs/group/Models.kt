/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.group

import com.ivianuu.apelabs.color.ApeColor
import com.ivianuu.apelabs.program.Program
import com.ivianuu.apelabs.util.randomId
import com.ivianuu.essentials.db.AbstractEntityDescriptor
import com.ivianuu.essentials.db.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable data class GroupConfig(
  val program: Program = Program.RAINBOW,
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false,
  val blackout: Boolean = false
)

@Serializable data class GroupConfigEntity(
  val program: String,
  val brightness: Float,
  val speed: Float,
  val musicMode: Boolean,
  val blackout: Boolean
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
}.also {
  println("merged $it for $this")
}

val GROUPS = (1..4).toList()
