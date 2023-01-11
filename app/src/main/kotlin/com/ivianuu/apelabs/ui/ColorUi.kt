@file:OptIn(ExperimentalMaterialApi::class)

package com.ivianuu.apelabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.ivianuu.apelabs.data.LightColor
import com.ivianuu.apelabs.data.toColor
import com.ivianuu.apelabs.domain.BuiltInColors
import com.ivianuu.apelabs.domain.ColorRepository
import com.ivianuu.essentials.state.action
import com.ivianuu.essentials.state.bind
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.dialog.Dialog
import com.ivianuu.essentials.ui.dialog.DialogScaffold
import com.ivianuu.essentials.ui.dialog.TextInputKey
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.material.guessingContentColorFor
import com.ivianuu.essentials.ui.navigation.KeyUiContext
import com.ivianuu.essentials.ui.navigation.PopupKey
import com.ivianuu.essentials.ui.navigation.SimpleKeyUi
import com.ivianuu.essentials.ui.navigation.pop
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.injekt.Provide

data class ColorKey(
  val initial: LightColor = LightColor()
) : PopupKey<LightColor>

@Provide fun colorUi(
  colorRepository: ColorRepository,
  ctx: KeyUiContext<ColorKey>
) = SimpleKeyUi<ColorKey> {
  DialogScaffold {
    var red by remember { mutableStateOf(ctx.key.initial.red) }
    var green by remember { mutableStateOf(ctx.key.initial.green) }
    var blue by remember { mutableStateOf(ctx.key.initial.blue) }
    var white by remember { mutableStateOf(ctx.key.initial.white) }

    fun currentColor() = LightColor(red, green, blue, white)

    Dialog(
      content = {
        val customColors = colorRepository.colors.bind(emptyMap())

        VerticalList {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(currentColor().toColor())
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
          }

          @Composable fun ColorList(
            colors: Map<String, LightColor>,
            deletable: Boolean,
            title: String
          ) {
            Subheader { Text(title) }

            FlowRow {
              colors
                .toList()
                .sortedBy { it.first.lowercase() }
                .forEach { (name, color) ->
                  Chip(
                    modifier = Modifier
                      .padding(start = 8.dp),
                    onClick = {
                      red = color.red
                      green = color.green
                      blue = color.blue
                      white = color.white
                    },
                    colors = ChipDefaults.chipColors(
                      backgroundColor = color.toColor(),
                      contentColor = guessingContentColorFor(color.toColor())
                    )
                  ) {
                    Text(name)

                    if (deletable) {
                      Spacer(Modifier.padding(start = 8.dp))

                      IconButton(
                        modifier = Modifier
                          .size(18.dp),
                        onClick = action {
                          colorRepository.deleteColor(name)
                        }
                      ) {
                        Icon(Icons.Default.Close)
                      }
                    }
                  }
                }
            }
          }

          if (customColors.isNotEmpty()) {
            item {
              ColorList(
                customColors,
                true,
                "Custom"
              )
            }
          }

          item {
            ColorList(
              BuiltInColors,
              false,
              "Built in"
            )
          }
        }
      },
      buttons = {
        OutlinedButton(
          onClick = action {
            ctx.navigator.push(TextInputKey(label = "Name.."))
              ?.let { colorRepository.saveColor(it, currentColor()) }
          }
        ) { Text("SAVE") }

        Button(onClick = action { ctx.navigator.pop(ctx.key, currentColor()) }) {
          Text("OK")
        }
      }
    )
  }
}
