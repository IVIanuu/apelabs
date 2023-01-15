/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.data

import com.ivianuu.apelabs.color.ApeColor
import com.ivianuu.essentials.cast
import com.ivianuu.essentials.time.seconds
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable data class GroupConfig(
  val program: Program = Program.SingleColor(ApeColor()),
  val brightness: Float = 1f,
  val speed: Float = 0f,
  val musicMode: Boolean = false,
  val blackout: Boolean = false
)

@Serializable sealed interface Program {
  @Serializable data class SingleColor(val color: ApeColor = ApeColor()) : Program
  @Serializable data class MultiColor(val items: List<Item> = listOf(Item())) : Program {
    init {
      check(items.size in ITEM_RANGE)
    }

    @Serializable data class Item(
      val color: ApeColor = ApeColor(),
      val fade: Duration = 1.seconds,
      val hold: Duration = 1.seconds
    )

    companion object {
      val ITEM_RANGE = 1..4
    }
  }

  @Serializable object Rainbow : Program
}

fun List<GroupConfig>.merge(): GroupConfig = when {
  isEmpty() -> GroupConfig()
  size == 1 -> single()
  else -> GroupConfig(
    program = when {
      all { it.program is Program.SingleColor } -> {
        val colors = map { it.program.cast<Program.SingleColor>().color }
        Program.SingleColor(
          color = ApeColor(
            red = colors.map { it.red }.average().toFloat(),
            green = colors.map { it.green }.average().toFloat(),
            blue = colors.map { it.blue }.average().toFloat(),
            white = colors.map { it.white }.average().toFloat()
          )
        )
      }
      all { a -> all { a.program == it.program } } -> first().program
      else -> Program.SingleColor(ApeColor())
    },
    brightness = map { it.brightness }.average().toFloat(),
    speed = map { it.speed }.average().toFloat(),
    musicMode = all { it.musicMode },
    blackout = all { it.blackout }
  )
}

val GROUPS = (1..4).toList()
