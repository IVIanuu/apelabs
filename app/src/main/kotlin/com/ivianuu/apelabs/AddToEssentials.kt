package com.ivianuu.apelabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivianuu.essentials.ui.common.UiRenderer
import com.ivianuu.essentials.ui.material.Button
import com.ivianuu.essentials.ui.material.esButtonColors
import com.ivianuu.essentials.ui.material.guessingContentColorFor
import com.ivianuu.injekt.Inject

@Composable fun <T> ToggleButtonGroup(
  values: List<T>,
  selected: T,
  onSelectionChanged: (T) -> Unit,
  @Inject renderer: UiRenderer<T>,
  title: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .height(88.dp)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
  ) {
    CompositionLocalProvider(
      LocalTextStyle provides MaterialTheme.typography.subtitle1,
      content = title
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      values.forEach { value ->
        val targetBackgroundColor = if (value == selected) MaterialTheme.colors.secondary
        else LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
        val backgroundColor by animateColorAsState(targetBackgroundColor)
        val contentColor by animateColorAsState(guessingContentColorFor(targetBackgroundColor))
        Button(
          colors = ButtonDefaults.esButtonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
          ),
          onClick = { onSelectionChanged(value) }
        ) {
          Text(renderer(value))
        }
      }
    }
  }
}
