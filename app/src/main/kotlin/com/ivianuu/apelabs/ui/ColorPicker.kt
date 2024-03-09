package com.ivianuu.apelabs.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*
import androidx.core.graphics.*
import androidx.core.graphics.drawable.*
import com.ivianuu.apelabs.R
import com.ivianuu.essentials.*
import com.ivianuu.injekt.*

@Composable fun ImageColorPicker(
  appContext: AppContext = inject,
  modifier: Modifier,
  controller: ColorPickerState,
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
            color = Color.Black.copy(alpha = 0.66f),
            radius = 13.dp.toPx(),
            center = controller.position!!
          )
          drawCircle(
            color = Color.White,
            radius = 12.dp.toPx(),
            center = controller.position!!
          )
        }
      },
    propagateMinConstraints = true
  ) {
    Image(
      bitmap = controller.palette,
      contentScale = ContentScale.FillBounds,
      contentDescription = null
    )
  }
}

class ColorPickerState(private val appContext: AppContext = inject) {
  var palette by mutableStateOf(
    appContext.resources.getDrawable(R.drawable.color_picker)
      .toBitmap()
      .asImageBitmap()
  )

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
      palette =  appContext.resources.getDrawable(R.drawable.color_picker)
        .toBitmap()
        .scale(size.width, size.height)
        .asImageBitmap()
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
