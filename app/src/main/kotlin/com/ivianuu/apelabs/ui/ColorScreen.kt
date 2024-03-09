@file:OptIn(ExperimentalMaterialApi::class)

package com.ivianuu.apelabs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import com.google.accompanist.flowlayout.FlowRow
import com.ivianuu.apelabs.data.*
import com.ivianuu.apelabs.domain.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.ui.common.*
import com.ivianuu.essentials.ui.dialog.*
import com.ivianuu.essentials.ui.material.*
import com.ivianuu.essentials.ui.material.Switch
import com.ivianuu.essentials.ui.navigation.*
import com.ivianuu.injekt.*

data class ColorScreen(val initial: ApeColor) : DialogScreen<ApeColor>

@Provide fun colorUi(
  screen: ColorScreen,
  navigator: Navigator,
  colorRepository: ColorRepository,
  groupConfigRepository: GroupConfigRepository,
  previewRepository: PreviewRepository
) = Ui<ColorScreen> {
  var id by remember { mutableStateOf(screen.initial.id) }
  var red by remember { mutableStateOf(screen.initial.red) }
  var green by remember { mutableStateOf(screen.initial.green) }
  var blue by remember { mutableStateOf(screen.initial.blue) }
  var white by remember { mutableStateOf(screen.initial.white) }

  fun currentColor() = ApeColor(id, red, green, blue, white)

  previewRepository.Previews(currentColor()) { selectedGroups ->
    groupConfigRepository.groupConfigs
      .state(emptyList())
      .filter { it.id.toIntOrNull() in selectedGroups }
      .map { config -> config.copy(program = currentColor().asProgram()) }
  }

  Dialog(
    applyContentPadding = false,
    content = {
      val userColors = colorRepository.userColors.scopedState(emptyList())

      VerticalList(modifier = Modifier.padding(horizontal = 8.dp)) {
        item {
          ColorListIcon(
            colors = listOf(currentColor()),
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp)
          )
        }

        @Composable fun ColorSlider(
          value: Float,
          onValueChange: (Float) -> Unit,
          color: Color
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
              modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
              value = value,
              onValueChange = onValueChange,
              colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
              )
            )

            Text(
              modifier = Modifier.width(32.dp),
              text = (value * 255f).toInt().toString()
            )
          }
        }

        item {
          ColorSlider(
            value = red,
            onValueChange = {
              id = screen.initial.id
              red = it
            },
            color = Color.Red
          )
        }

        item {
          ColorSlider(
            value = green,
            onValueChange = {
              id = screen.initial.id
              green = it
            },
            color = Color.Green
          )
        }

        item {
          ColorSlider(
            value = blue,
            onValueChange = {
              id = screen.initial.id
              blue = it
            },
            color = Color.Blue
          )
        }

        item {
          ColorSlider(
            value = white,
            onValueChange = {
              id = screen.initial.id
              white = it
            },
            color = Color.White
          )
        }

        @Composable fun ColorList(
          colors: List<ApeColor>,
          deletable: Boolean,
          title: String
        ) {
          Subheader { Text(title) }

          FlowRow {
            colors
              .toList()
              .sortedBy { it.id.lowercase() }
              .forEach { color ->
                Chip(
                  modifier = Modifier
                    .padding(start = 16.dp),
                  onClick = {
                    id = color.id
                    red = color.red
                    green = color.green
                    blue = color.blue
                    white = color.white
                  },
                  colors = ChipDefaults.chipColors(
                    backgroundColor = color.toComposeColor(),
                    contentColor = guessingContentColorFor(color.toComposeColor())
                  )
                ) {
                  Text(color.id)

                  if (deletable) {
                    Spacer(Modifier.padding(start = 8.dp))

                    Box(modifier = Modifier.requiredSize(18.dp)) {
                      DropdownMenuButton {
                        DropdownMenuItem(
                          onClick = scopedAction { colorRepository.deleteColor(color.id) }
                        ) { Text("Delete") }
                      }
                    }
                  }
                }
              }
          }
        }

        if (userColors.isNotEmpty()) {
          item {
            ColorList(
              userColors,
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
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Preview")

        Spacer(Modifier.width(8.dp))

        Switch(
          checked = previewRepository.previewsEnabled,
          onCheckedChange = { previewRepository.previewsEnabled = it },
        )
      }

      OutlinedButton(
        onClick = action {
          navigator.push(TextInputScreen(label = "Name.."))
            ?.let {
              id = it
              colorRepository.updateColor(currentColor())
            }
        }
      ) { Text("SAVE") }

      Button(onClick = action { navigator.pop(screen, currentColor()) }) {
        Text("OK")
      }
    }
  )
}
