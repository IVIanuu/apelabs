package com.ivianuu.apelabs.ui

import androidx.compose.foundation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.unit.*
import com.ivianuu.apelabs.data.*

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
