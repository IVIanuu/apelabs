@file:OptIn(ExperimentalMaterialApi::class)

package com.ivianuu.apelabs.color

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.ivianuu.apelabs.domain.PreviewRepository
import com.ivianuu.apelabs.program.Program
import com.ivianuu.apelabs.program.asProgram
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.compose.bind
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.dialog.Dialog
import com.ivianuu.essentials.ui.dialog.DialogScaffold
import com.ivianuu.essentials.ui.dialog.TextInputKey
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.material.Switch
import com.ivianuu.essentials.ui.material.guessingContentColorFor
import com.ivianuu.essentials.ui.navigation.KeyUiContext
import com.ivianuu.essentials.ui.navigation.PopupKey
import com.ivianuu.essentials.ui.navigation.SimpleKeyUi
import com.ivianuu.essentials.ui.navigation.pop
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.popup.PopupMenuButton
import com.ivianuu.essentials.ui.popup.PopupMenuItem
import com.ivianuu.injekt.Provide

data class ColorKey(val initial: ApeColor) : PopupKey<ApeColor>

context(ColorRepository, PreviewRepository, KeyUiContext<ColorKey>)
    @Provide fun colorUi() = SimpleKeyUi<ColorKey> {
  DialogScaffold {
    var red by remember { mutableStateOf(key.initial.red) }
    var green by remember { mutableStateOf(key.initial.green) }
    var blue by remember { mutableStateOf(key.initial.blue) }
    var white by remember { mutableStateOf(key.initial.white) }

    fun currentColor() = ApeColor(key.initial.id, red, green, blue, white)

    LaunchedEffect(true) {
      providePreviews { update ->
        snapshotFlow { currentColor() }
          .collect { update(it.asProgram(Program.COLOR_PICKER_ID)) }
      }
    }

    Dialog(
      content = {
        val customColors = colors.bind(emptyList())

        VerticalList {
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
            title: String,
            color: Color
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(title)

              Slider(
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 4.dp),
                value = value,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                  thumbColor = color,
                  activeTrackColor = color
                )
              )

              Text(
                modifier = Modifier.width(32.dp),
                text = (value * 255).toInt().toString()
              )
            }
          }

          item {
            ColorSlider(
              value = red,
              onValueChange = { red = it },
              title = "R",
              color = Color.Red
            )
          }

          item {
            ColorSlider(
              value = green,
              onValueChange = { green = it },
              title = "G",
              color = Color.Green
            )
          }

          item {
            ColorSlider(
              value = blue,
              onValueChange = { blue = it },
              title = "B",
              color = Color.Blue
            )
          }

          item {
            ColorSlider(
              value = white,
              onValueChange = { white = it },
              title = "W",
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
                    Text(color.id!!)

                    if (deletable) {
                      Spacer(Modifier.padding(start = 8.dp))

                      Box(modifier = Modifier.requiredSize(18.dp)) {
                        PopupMenuButton {
                          PopupMenuItem(
                            onSelected = action { deleteColor(color.id!!) }
                          ) { Text("Delete") }
                        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Preview")

          Spacer(Modifier.width(8.dp))

          Switch(
            checked = previewsEnabled.bind(),
            onCheckedChange = action { value -> updatePreviewsEnabled(value) },
          )
        }

        OutlinedButton(
          onClick = action {
            navigator.push(TextInputKey(label = "Name.."))
              ?.let { updateColor(currentColor().copy(id = it)) }
          }
        ) { Text("SAVE") }

        Button(onClick = action { navigator.pop(key, currentColor()) }) {
          Text("OK")
        }
      }
    )
  }
}
