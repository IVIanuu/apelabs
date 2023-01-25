package com.ivianuu.apelabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ivianuu.essentials.ui.material.guessingContentColorFor

@Composable fun QuickSettingsSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  icon: Int,
  modifier: Modifier = Modifier
) {
  var internalValue by remember { mutableStateOf(value) }

  BoxWithConstraints(
    modifier = modifier,
    contentAlignment = Alignment.CenterStart
  ) {
    val thumbSize = 48.dp

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .padding(horizontal = thumbSize / 2)
        .background(
          LocalContentColor.current.copy(alpha = 0.12f),
          RoundedCornerShape(50)
        )
    )

    val thumbPosition = (maxWidth * internalValue)
      .coerceAtLeast(thumbSize)

    Box(
      modifier = Modifier
        .height(thumbSize)
        .width(thumbPosition)
        .background(
          MaterialTheme.colors.secondary,
          RoundedCornerShape(50)
        )
    )

    Icon(
      icon,
      modifier = Modifier
        .size(24.dp)
        .offset(x = thumbPosition - thumbSize / 2 - 12.dp),
      tint = guessingContentColorFor(MaterialTheme.colors.secondary)
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(thumbSize)
        .pointerInput(true) {
          while (true) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent().changes.first()
                event.consume()

                if (event.changedToUpIgnoreConsumed()) {
                  onValueChange(internalValue)
                  break
                } else {
                  val newValue = (event.position.x / maxWidth.toPx())
                    .coerceIn(0f, 1f)
                  internalValue = newValue
                  onValueChange(internalValue)
                }
              }
            }
          }
        }
    )
  }
}

@Composable fun RowScope.QuickSettingsToggle(
  icon: Int,
  title: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  val backgroundColor = if (selected) MaterialTheme.colors.secondary
  else LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
  Surface(
    modifier = Modifier
      .weight(1f)
      .height(64.dp),
    color = backgroundColor,
    contentColor = guessingContentColorFor(backgroundColor),
    shape = RoundedCornerShape(50)
  ) {
    Row(
      modifier = Modifier.clickable(onClick = onClick),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        modifier = Modifier.padding(start = 16.dp),
        painterResId = icon
      )

      Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = title,
        style = MaterialTheme.typography.subtitle1
      )
    }
  }
}
