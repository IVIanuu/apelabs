package com.ivianuu.apelabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.toColor
import com.ivianuu.apelabs.domain.Preview
import com.ivianuu.apelabs.domain.ProgramRepository
import com.ivianuu.essentials.coroutines.onCancel
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.get
import com.ivianuu.essentials.resource.getOrNull
import com.ivianuu.essentials.resource.map
import com.ivianuu.essentials.state.action
import com.ivianuu.essentials.state.bindResource
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.TopAppBar
import com.ivianuu.essentials.ui.navigation.Key
import com.ivianuu.essentials.ui.navigation.KeyUiContext
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.ModelKeyUi
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.resource.ResourceBox
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
        value.items.forEach { item ->
          item {
            Row(
              modifier = Modifier
                .padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier.size(40.dp)
                  .background(item.color.toColor())
                  .clickable { updateColor(item) }
              )

              Column(
                modifier = Modifier.weight(1f)
              ) {

              }

              IconButton(onClick = { deleteItem(item) }) { Icon(Icons.Default.Close) }
            }
          }
        }
      }
    }
  }
}

data class ProgramModel(
  val id: String,
  val program: Resource<Program.MultiColor>,
  val addItem: () -> Unit,
  val updateColor: (Program.MultiColor.Item) -> Unit,
  val updateFade: (Program.MultiColor.Item, Duration) -> Unit,
  val updateHold: (Program.MultiColor.Item, Duration) -> Unit,
  val deleteItem: (Program.MultiColor.Item) -> Unit
) {
  val canAddItem: Boolean
    get() = program.getOrNull()?.items?.size?.let { it < Program.MultiColor.MAX_ITEMS } == true
}

@Provide fun programModel(
  repository: ProgramRepository,
  previewProgram: MutableStateFlow<@Preview Program?>,
  ctx: KeyUiContext<ProgramKey>
) = Model {
  val id = ctx.key.id

  val program = repository.program(id)
    .bindResource()
    .map { it!! }

  LaunchedEffect(program) {
    // debounce
    delay(100.milliseconds)
    previewProgram.value = program.getOrNull()
  }

  LaunchedEffect(true) {
    onCancel {
      previewProgram.value = null
    }
  }

  suspend fun updateProgram(block: Program.MultiColor.() -> Program.MultiColor) {
    repository.updateProgram(id, program.get().block())
  }

  suspend fun updateItem(
    item: Program.MultiColor.Item,
    block: Program.MultiColor.Item.() -> Program.MultiColor.Item
  ) {
    updateProgram {
      val index = items.indexOf(item)
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
    updateColor = action { item ->
      ctx.navigator.push(ColorKey(item.color))
        ?.let {
          updateItem(item) { copy(color = it) }
        }
    },
    updateFade = action { item, fade ->
      updateItem(item) { copy(fade = fade) }
    },
    updateHold = action { item, hold ->
      updateItem(item) { copy(hold = hold) }
    },
    deleteItem = action { item ->
      updateProgram {
        copy(items = items - item)
      }
    }
  )
}
