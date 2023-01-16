package com.ivianuu.apelabs.program

import androidx.compose.ui.graphics.Color
import com.ivianuu.apelabs.color.ApeColor
import com.ivianuu.apelabs.color.toApeColor
import com.ivianuu.essentials.time.seconds
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable data class Program(val items: List<Item> = listOf(Item())) {
  init {
    check(items.size in ITEM_RANGE)
  }

  @Serializable data class Item(
    val color: ApeColor = ApeColor(white = 1f),
    val fadeTime: Duration = 1.seconds,
    val holdTime: Duration = 1.seconds
  )

  companion object {
    val ITEM_RANGE = 1..4
    val RAINBOW = Program(
      items = listOf(
        Item(Color.Red.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Yellow.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Green.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Blue.toApeColor(), Duration.INFINITE, Duration.INFINITE)
      )
    )
  }
}

fun ApeColor.asProgram() = Program(items = listOf(Program.Item(this)))
