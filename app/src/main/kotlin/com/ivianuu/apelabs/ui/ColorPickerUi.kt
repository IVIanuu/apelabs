@file:OptIn(ExperimentalMaterialApi::class)

package com.ivianuu.apelabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ivianuu.apelabs.data.DefaultColors
import com.ivianuu.apelabs.data.LightColor
import com.ivianuu.apelabs.data.toColor
import com.ivianuu.essentials.state.action
import com.ivianuu.essentials.ui.common.HorizontalList
import com.ivianuu.essentials.ui.dialog.Dialog
import com.ivianuu.essentials.ui.dialog.DialogScaffold
import com.ivianuu.essentials.ui.material.guessingContentColorFor
import com.ivianuu.essentials.ui.navigation.KeyUiContext
import com.ivianuu.essentials.ui.navigation.PopupKey
import com.ivianuu.essentials.ui.navigation.SimpleKeyUi
import com.ivianuu.essentials.ui.navigation.pop
import com.ivianuu.injekt.Provide

data class ColorPickerKey(
  val initial: LightColor = LightColor()
) : PopupKey<LightColor>

@Provide fun colorPickerUi(ctx: KeyUiContext<ColorPickerKey>) = SimpleKeyUi<ColorPickerKey> {
  DialogScaffold {
    var red by remember { mutableStateOf(ctx.key.initial.red) }
    var green by remember { mutableStateOf(ctx.key.initial.green) }
    var blue by remember { mutableStateOf(ctx.key.initial.blue) }
    var white by remember { mutableStateOf(ctx.key.initial.white) }

    Dialog(
      content = {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp)
              .background(LightColor(red, green, blue, white).toColor())
          )

          Slider(
            value = red,
            onValueChange = { red = it },
            colors = SliderDefaults.colors(
              thumbColor = Color.Red,
              activeTrackColor = Color.Red
            )
          )

          Slider(
            value = green,
            onValueChange = { green = it },
            colors = SliderDefaults.colors(
              thumbColor = Color.Green,
              activeTrackColor = Color.Green
            )
          )

          Slider(
            value = blue,
            onValueChange = { blue = it },
            colors = SliderDefaults.colors(
              thumbColor = Color.Blue,
              activeTrackColor = Color.Blue
            )
          )

          Slider(
            value = white,
            onValueChange = { white = it },
            colors = SliderDefaults.colors(
              thumbColor = Color.White,
              activeTrackColor = Color.White
            )
          )

          HorizontalList {
            DefaultColors.forEach { color ->
              item {
                Chip(
                  modifier = Modifier
                    .padding(start = 8.dp),
                  onClick = {
                    red = color.color.red
                    green = color.color.green
                    blue = color.color.blue
                    white = color.color.white
                  },
                  colors = ChipDefaults.chipColors(
                    backgroundColor = color.color.toColor(),
                    contentColor = guessingContentColorFor(color.color.toColor())
                  )
                ) {
                  Text(color.name)
                }
              }
            }
          }
        }
      },
      buttons = {
        Button(
          onClick = action {
            ctx.navigator.pop(
              ctx.key,
              LightColor(red, green, blue, white)
            )
          }
        ) { Text("OK") }
      }
    )
  }
}
