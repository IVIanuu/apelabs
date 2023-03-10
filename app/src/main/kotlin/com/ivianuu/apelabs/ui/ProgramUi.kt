package com.ivianuu.apelabs.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.randomId
import com.ivianuu.apelabs.domain.GroupConfigRepository
import com.ivianuu.apelabs.domain.PreviewRepository
import com.ivianuu.apelabs.domain.ProgramRepository
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.compose.bind
import com.ivianuu.essentials.resource.Idle
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.flowAsResource
import com.ivianuu.essentials.resource.get
import com.ivianuu.essentials.resource.getOrNull
import com.ivianuu.essentials.resource.map
import com.ivianuu.essentials.time.hours
import com.ivianuu.essentials.time.milliseconds
import com.ivianuu.essentials.time.minutes
import com.ivianuu.essentials.time.seconds
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Slider
import com.ivianuu.essentials.ui.material.TopAppBar
import com.ivianuu.essentials.ui.navigation.Key
import com.ivianuu.essentials.ui.navigation.KeyUiContext
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.ModelKeyUi
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.prefs.SwitchListItem
import com.ivianuu.essentials.ui.resource.ResourceBox
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

data class ProgramKey(val id: String) : Key<Unit>

@Provide val programUi = ModelKeyUi<ProgramKey, ProgramModel> {
  Scaffold(
    topBar = { TopAppBar(title = { Text(id) }) },
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = {
      if (canAddItem)
        ExtendedFloatingActionButton(
          onClick = addItem,
          text = {
            Text("ADD ITEM")
          }
        )
    }
  ) {
    ResourceBox(program) { value ->
      VerticalList {
        value.items.forEachIndexed { itemIndex, item ->
          item {
            Row(
              modifier = Modifier
                .padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ColorListIcon(
                modifier = Modifier
                  .size(40.dp)
                  .clickable { updateColor(itemIndex) },
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
                  onValueChange = { updateFade(itemIndex, it) },
                  title = "Fade"
                )

                DurationSlider(
                  value = item.holdTime,
                  onValueChange = { updateHold(itemIndex, it) },
                  title = "Hold"
                )
              }

              if (itemIndex != 0)
                IconButton(onClick = { deleteItem(itemIndex) }) { Icon(Icons.Default.Close) }
              else
                Spacer(Modifier.size(40.dp))
            }
          }
        }

        item {
          SwitchListItem(
            value = previewsEnabled,
            onValueChange = updatePreviewsEnabled,
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

data class ProgramModel(
  val id: String,
  val program: Resource<Program>,
  val addItem: () -> Unit,
  val updateColor: (Int) -> Unit,
  val updateFade: (Int, Duration) -> Unit,
  val updateHold: (Int, Duration) -> Unit,
  val deleteItem: (Int) -> Unit,
  val previewsEnabled: Boolean,
  val updatePreviewsEnabled: (Boolean) -> Unit
) {
  val canAddItem: Boolean
    get() = program.getOrNull()?.items?.size?.let { it < Program.ITEM_RANGE.last } == true
}

@Provide fun programModel(
  ctx: KeyUiContext<ProgramKey>,
  groupConfigRepository: GroupConfigRepository,
  programRepository: ProgramRepository,
  previewRepository: PreviewRepository
) = Model {
  val id = ctx.key.id

  val program by remember {
    programRepository.program(id)
      .flowAsResource()
      .map { it.map { it!! } }
  }
    .collectAsState(Idle)

  LaunchedEffect(true) {
    previewRepository.providePreviews { selectedGroups, update ->
      snapshotFlow { program }
        .map { it.getOrNull() }
        .flatMapLatest { program ->
          if (program == null) flowOf(emptyList())
          else groupConfigRepository.groupConfigs
            .map { configs ->
              configs
                .filter { it.id.toIntOrNull() in selectedGroups }
                .map { config -> config.copy(program = program) }
            }
        }
        .collect(update)
    }
  }

  suspend fun updateProgram(block: Program.() -> Program) {
    programRepository.updateProgram(program.get().block())
  }

  suspend fun updateItem(itemIndex: Int, block: Program.Item.() -> Program.Item) {
    val item = program.get().items[itemIndex].block()
    updateProgram {
      copy(
        items = items.toMutableList()
          .apply { set(itemIndex, item) }
      )
    }
  }

  ProgramModel(
    id = id,
    program = program,
    addItem = action { updateProgram { copy(items = items + Program.Item(color = ApeColor())) } },
    updateColor = action { itemIndex ->
      ctx.navigator.push(ColorKey(program.get().items.get(itemIndex).color.copy(id = randomId())))
        ?.let { updateItem(itemIndex) { copy(color = it) } }
    },
    updateFade = action { itemIndex, fade ->
      updateItem(itemIndex) { copy(fadeTime = fade) }
    },
    updateHold = action { itemIndex, hold ->
      updateItem(itemIndex) { copy(holdTime = hold) }
    },
    deleteItem = action { itemIndex ->
      updateProgram { copy(items = items.toMutableList().apply { removeAt(itemIndex) }) }
    },
    previewsEnabled = previewRepository.previewsEnabled.bind(),
    updatePreviewsEnabled = action { value -> previewRepository.updatePreviewsEnabled(value) }
  )
}
