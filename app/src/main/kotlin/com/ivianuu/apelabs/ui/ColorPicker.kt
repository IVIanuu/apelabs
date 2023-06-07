package com.ivianuu.apelabs.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import com.ivianuu.apelabs.R
import com.ivianuu.essentials.Resources
import com.ivianuu.essentials.ui.image.toImageBitmap
import com.ivianuu.injekt.Inject

@Composable fun ImageColorPicker(
  modifier: Modifier,
  controller: ColorPickerController
) {
  Box(
    modifier = modifier
      .onSizeChanged { controller.updateSize(it) }
      .pointerInput(true) {
        while (true) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent().changes.first()
              event.consume()
              controller.updatePosition(event.position)
              if (event.changedToUpIgnoreConsumed())
                break
            }
          }
        }
      }
      .drawWithContent {
        drawContent()
        if (controller.position != null) {
          drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = controller.position!!
          )
        }
      },
    propagateMinConstraints = true
  ) {
    Image(bitmap = controller.palette, contentScale = ContentScale.FillBounds)
  }
}

class ColorPickerController(@Inject private val resources: Resources) {
  var palette by mutableStateOf(resources<ImageBitmap>(R.drawable.color_picker))

  var position by mutableStateOf<Offset?>(null)
    private set
  var selectedColor by mutableStateOf<Color?>(null)
    private set

  var size by mutableStateOf(IntSize(0, 0))
    private set

  fun clear() {
    position = null
    selectedColor = null
  }

  fun updateSize(size: IntSize) {
    if (this.size != size) {
      this.size = size
      palette = resources<ImageBitmap>(R.drawable.color_picker)
        .asAndroidBitmap()
        .scale(size.width, size.height)
        .toImageBitmap()
    }
  }

  fun updatePosition(position: Offset) {
    val finalPosition = position.copy(
      x = position.x.coerceIn(0f, size.width.toFloat() - 1f),
      y = position.y.coerceIn(0f, size.height.toFloat() - 1f)
    )
    if (this.position != finalPosition) {
      this.position = finalPosition
      selectedColor =
        Color(palette.asAndroidBitmap().getPixel(finalPosition.x.toInt(), finalPosition.y.toInt()))
    }
  }
}
