package com.ivianuu.apelabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.toColor
import com.ivianuu.apelabs.domain.PreviewRepository
import com.ivianuu.apelabs.domain.ProgramRepository
import com.ivianuu.essentials.resource.Idle
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.flowAsResource
import com.ivianuu.essentials.resource.get
import com.ivianuu.essentials.resource.getOrNull
import com.ivianuu.essentials.resource.map
import com.ivianuu.essentials.state.action
import com.ivianuu.essentials.time.minutes
import com.ivianuu.essentials.time.seconds
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Slider
import com.ivianuu.essentials.ui.material.TopAppBar
import com.ivianuu.essentials.ui.material.incrementingStepPolicy
import com.ivianuu.essentials.ui.navigation.Key
import com.ivianuu.essentials.ui.navigation.KeyUiContext
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.ModelKeyUi
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.resource.ResourceBox
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ProgramKey(val id: String) : Key<Unit>

@Provide fun programUi() = ModelKeyUi<ProgramKey, ProgramModel> {
  Scaffold(
    topBar = {
      TopAppBar(title = { Text(id) })
    },
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
        value.items.forEachIndexed { index, item ->
          item {
            Row(
              modifier = Modifier
                .padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier.size(40.dp)
                  .background(item.color.toColor())
                  .clickable { updateColor(index) }
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
                        .padding(horizontal = 8.dp),
                      value = internalValue,
                      onValueChange = { internalValue = it },
                      onValueChangeFinished = {
                        onValueChange(FloatToDuration[internalValue.toInt().toFloat()]!!)
                      },
                      valueRange = FloatToDuration.keys.toList()
                        .let { it.first()..it.last() },
                      stepPolicy = incrementingStepPolicy(1f)
                    )

                    Text(
                      modifier = Modifier.width(40.dp),
                      text = FloatToDuration[internalValue.toInt().toFloat()]!!.toString()
                    )
                  }
                }

                DurationSlider(
                  value = item.fade,
                  onValueChange = { updateFade(index, it) },
                  title = "Fade"
                )

                DurationSlider(
                  value = item.hold,
                  onValueChange = { updateHold(index, it) },
                  title = "Hold"
                )
              }

              IconButton(onClick = { deleteItem(index) }) { Icon(Icons.Default.Close) }
            }
          }
        }
      }
    }
  }
}

private val FloatToDuration = mapOf(
  0f to Duration.ZERO,
  1f to 250.milliseconds,
  2f to 500.milliseconds,
  3f to 1.seconds,
  4f to 2.seconds,
  5f to 3.seconds,
  6f to 4.seconds,
  7f to 5.seconds,
  8f to 10.seconds,
  9f to 20.seconds,
  10f to 30.seconds,
  11f to 45.seconds,
  12f to 1.minutes
)

private val DurationToFloat = FloatToDuration
  .map { it.value to it.key }
  .toMap()

data class ProgramModel(
  val id: String,
  val program: Resource<Program.MultiColor>,
  val addItem: () -> Unit,
  val updateColor: (Int) -> Unit,
  val updateFade: (Int, Duration) -> Unit,
  val updateHold: (Int, Duration) -> Unit,
  val deleteItem: (Int) -> Unit
) {
  val canAddItem: Boolean
    get() = program.getOrNull()?.items?.size?.let { it < Program.MultiColor.MAX_ITEMS } == true
}

@Provide fun programModel(
  programRepository: ProgramRepository,
  previewRepository: PreviewRepository,
  ctx: KeyUiContext<ProgramKey>
) = Model {
  val id = ctx.key.id

  val program by programRepository.program(id)
    .flowAsResource()
    .map { it.map { it!! } }
    .collectAsState(Idle)

  LaunchedEffect(true) {
    previewRepository.providePreviews {
      snapshotFlow { program }
        .map { it.getOrNull() }
        .collect(it)
    }
  }

  suspend fun updateProgram(block: Program.MultiColor.() -> Program.MultiColor) {
    programRepository.updateProgram(id, program.get().block())
  }

  suspend fun updateItem(
    index: Int,
    block: Program.MultiColor.Item.() -> Program.MultiColor.Item
  ) {
    updateProgram {
      copy(
        items = items.toMutableList().apply {
          set(index, get(index).block())
        }
      )
    }
  }

  ProgramModel(
    id = id,
    program = program,
    addItem = action {
      updateProgram { copy(items = items + Program.MultiColor.Item()) }
    },
    updateColor = action { index ->
      ctx.navigator.push(ColorKey(program.get().items[index].color))
        ?.let {
          updateItem(index) { copy(color = it) }
        }
    },
    updateFade = action { index, fade ->
      updateItem(index) { copy(fade = fade) }
    },
    updateHold = action { index, hold ->
      updateItem(index) { copy(hold = hold) }
    },
    deleteItem = action { index ->
      updateProgram {
        copy(items = items.toMutableList().apply { removeAt(index) })
      }
    }
  )
}
