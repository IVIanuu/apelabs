package com.ivianuu.apelabs.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ivianuu.apelabs.color.toColor
import com.ivianuu.apelabs.data.Program

@Composable fun Program(modifier: Modifier, program: Program) {
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

  Canvas(modifier) {
    val itemSize = Size(
      width = size.width / colors.size - ColorPadding.toPx(),
      height = size.height
    )
    var x = (ColorPadding.toPx() * colors.size - 1) / 2
    colors.forEach { color ->
      drawRect(
        color = color,
        topLeft = Offset(x, 0f),
        size = itemSize
      )
      x += itemSize.width
      x += ColorPadding.toPx()
    }
  }
}

private val ColorPadding = 1.dp
