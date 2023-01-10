package com.ivianuu.apelabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
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
import com.ivianuu.apelabs.data.LightColor
import com.ivianuu.essentials.state.action
import com.ivianuu.essentials.ui.dialog.Dialog
import com.ivianuu.essentials.ui.dialog.DialogScaffold
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
              .background(Color(red, green, blue))
          )

          Slider(
            value = red,
            onValueChange = { red = it },
            colors = SliderDefaults.colors(
              thumbColor = Color.Red
            )
          )

          Slider(
            value = green,
            onValueChange = { green = it },
            colors = SliderDefaults.colors(
              thumbColor = Color.Green
            )
          )

          Slider(
            value = blue,
            onValueChange = { blue = it },
            colors = SliderDefaults.colors(
              thumbColor = Color.Blue
            )
          )

          Slider(
            value = white,
            onValueChange = { white = it },
            colors = SliderDefaults.colors(
              thumbColor = Color.White
            )
          )
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
