package com.ivianuu.apelabs.color

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.ivianuu.apelabs.program.Program

@Composable fun ColorListIcon(modifier: Modifier, colors: List<ApeColor>) {
  val composeColors = colors.map { it.toComposeColor() }

  Canvas(modifier) {
    val itemSize = Size(
      width = size.width / composeColors.size - ColorPadding.toPx(),
      height = size.height
    )
    var x = (ColorPadding.toPx() * composeColors.size - 1) / 2
    composeColors.forEach { color ->
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

@Composable fun ColorListIcon(modifier: Modifier, program: Program) {
  ColorListIcon(
    modifier = modifier,
    colors = program.items.map { it.color }
  )
}

private val ColorPadding = 1.dp
