package com.ivianuu.apelabs.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.toColor

@Composable fun Program(program: Program, modifier: Modifier = Modifier) {
  val colors = remember(program) {
    when (program) {
      is Program.MultiColor -> program.items.map { it.color.toColor() }
      Program.Rainbow -> listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Blue
      )
      is Program.SingleColor -> listOf(program.color.toColor())
    }
  }

  Canvas(
    Modifier
      .size(40.dp)
      .then(modifier)
  ) {
    val itemSize = Size(size.width / colors.size, size.height)
    var x = 0f
    println("draw colors $colors for $program $itemSize $size")
    colors.forEach { color ->
      drawRect(
        color = color,
        topLeft = Offset(x, 0f),
        size = itemSize
      )
      x += itemSize.width
    }
  }
}
