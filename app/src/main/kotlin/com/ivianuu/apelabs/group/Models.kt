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
  val id: String,
  val program: Program = Program.RAINBOW,
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false,
  val blackout: Boolean = false
)

@Serializable data class GroupConfigEntity(
  @PrimaryKey val id: String,
  val program: String,
  val brightness: Float,
  val speed: Float,
  val musicMode: Boolean,
  val blackout: Boolean
) {
  companion object : AbstractEntityDescriptor<GroupConfigEntity>("group_configs")
}

fun List<GroupConfig>.merge(id: String): GroupConfig = when {
  isEmpty() -> GroupConfig(id)
  size == 1 -> single()
  else -> GroupConfig(
    id = id,
    program = when {
      all { a -> all { a.program == it.program } } -> first().program
      else -> Program.RAINBOW
    },
    brightness = map { it.brightness }.average().toFloat(),
    speed = map { it.speed }.average().toFloat(),
    musicMode = all { it.musicMode },
    blackout = all { it.blackout }
  )
}

val GROUPS = (1..4).toList()
