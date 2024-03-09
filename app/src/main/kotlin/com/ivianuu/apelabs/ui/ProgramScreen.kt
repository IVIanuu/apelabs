package com.ivianuu.apelabs.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.apelabs.domain.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.resource.*
import com.ivianuu.essentials.ui.common.*
import com.ivianuu.essentials.ui.material.*
import com.ivianuu.essentials.ui.navigation.*
import com.ivianuu.essentials.ui.prefs.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*
import kotlin.time.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class ProgramScreen(val id: String) : Screen<Unit>

@Provide fun programUi(
  screen: ProgramScreen,
  navigator: Navigator,
  groupConfigRepository: GroupConfigRepository,
  programRepository: ProgramRepository,
  previewRepository: PreviewRepository
) = Ui<ProgramScreen> {
  val id = screen.id

  val programResource = programRepository.program(id)
    .map { it!! }
    .scopedResourceState()

  previewRepository.Previews(programResource) { selectedGroups ->
    programResource.getOrNull()
      ?.let { program ->
        groupConfigRepository.groupConfigs
          .state(emptyList())
          .filter { it.id.toIntOrNull() in selectedGroups }
          .map { config -> config.copy(program = program) }
      } ?: emptyList()
  }

  suspend fun updateProgram(block: Program.() -> Program) {
    programRepository.updateProgram(programResource.get().block())
  }

  suspend fun updateItem(itemIndex: Int, block: Program.Item.() -> Program.Item) {
    val item = programResource.get().items[itemIndex].block()
    updateProgram {
      copy(
        items = items.toMutableList()
          .apply { set(itemIndex, item) }
      )
    }
  }

  ScreenScaffold(
    topBar = { AppBar { Text(screen.id) } },
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = {
      if (programResource.getOrNull()?.items?.size?.let { it < Program.ITEM_RANGE.last } == true)
        ExtendedFloatingActionButton(
          onClick = scopedAction {
            updateProgram { copy(items = items + Program.Item(color = ApeColor())) }
          },
          text = {
            Text("ADD ITEM")
          }
        )
    }
  ) {
    ResourceBox(programResource) { program ->
      VerticalList {
        itemsIndexed(program.items) { itemIndex, item ->
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ColorListIcon(
              modifier = Modifier
                .size(40.dp)
                .clickable(onClick = scopedAction {
                  navigator.push(ColorScreen(program.items[itemIndex].color.copy(id = randomId())))
                    ?.let {
                      updateItem(itemIndex) { copy(color = it) }
                    }
                }),
              colors = listOf(item.color)
            )

            Column(
              modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
            ) {
              @Composable fun DurationSlider(
                value: Duration,
                onValueChange: (Duration) -> Unit,
                title: String
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(title)

                  var internalValue by remember { mutableStateOf(DurationToFloat[value]!!) }

                  Slider(
                    modifier = Modifier.weight(1f)
                      .padding(horizontal = 4.dp),
                    value = internalValue,
                    onValueChange = { internalValue = it },
                    onValueChangeFinished = {
                      onValueChange(FloatToDuration[internalValue.toInt().toFloat()]!!)
                    },
                    valueRange = FloatToDuration.keys.toList()
                      .let { it.first()..it.last() }
                  )

                  Text(
                    modifier = Modifier.width(40.dp),
                    text = FloatToDuration[internalValue.toInt().toFloat()]!!.toString()
                  )
                }
              }

              DurationSlider(
                value = item.fadeTime,
                onValueChange = scopedAction { value ->
                  updateItem(itemIndex) { copy(fadeTime = value) }
                },
                title = "Fade"
              )

              DurationSlider(
                value = item.holdTime,
                onValueChange = scopedAction { value ->
                  updateItem(itemIndex) { copy(holdTime = value) }
                },
                title = "Hold"
              )
            }

            if (itemIndex != 0)
              IconButton(onClick = scopedAction {
                updateProgram {
                  copy(items = items.toMutableList().apply { removeAt(itemIndex) })
                }
              }) { Icon(Icons.Default.Close, null) }
            else
              Spacer(Modifier.size(48.dp))
          }
        }

        item {
          SwitchListItem(
            value = previewRepository.previewsEnabled,
            onValueChange = { previewRepository.previewsEnabled = it },
            title = { Text("Preview") }
          )
        }
      }
    }
  }
}

private val FloatToDuration = listOf(
  Duration.ZERO,
  250.milliseconds,
  500.milliseconds,
  1.seconds,
  2.seconds,
  3.seconds,
  4.seconds,
  5.seconds,
  10.seconds,
  20.seconds,
  30.seconds,
  45.seconds,
  1.minutes,
  2.minutes,
  3.minutes,
  4.minutes,
  5.minutes,
  10.minutes,
  20.minutes,
  30.minutes,
  45.minutes,
  1.hours
)
  .mapIndexed { index, duration -> index.toFloat() to duration }
  .toMap()

private val DurationToFloat = FloatToDuration
  .map { it.value to it.key }
  .toMap()
