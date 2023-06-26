package com.ivianuu.apelabs.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.Scene
import com.ivianuu.apelabs.data.asProgram
import com.ivianuu.apelabs.data.randomId
import com.ivianuu.apelabs.domain.PreviewRepository
import com.ivianuu.apelabs.domain.ProgramRepository
import com.ivianuu.apelabs.domain.SceneRepository
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.collectAsResourceState
import com.ivianuu.essentials.resource.flowAsResource
import com.ivianuu.essentials.resource.get
import com.ivianuu.essentials.resource.getOrNull
import com.ivianuu.essentials.resource.map
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.dialog.ListScreen
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.material.TopAppBar
import com.ivianuu.essentials.ui.material.incrementingStepPolicy
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.Navigator
import com.ivianuu.essentials.ui.navigation.Screen
import com.ivianuu.essentials.ui.navigation.Ui
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.prefs.SliderListItem
import com.ivianuu.essentials.ui.prefs.SwitchListItem
import com.ivianuu.essentials.ui.resource.ResourceBox
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

data class SceneScreen(val id: String) : Screen<Unit>

@Provide val sceneUi = Ui<SceneScreen, SceneModel> { model ->
  Scaffold(
    topBar = {
      TopAppBar(title = { Text(model.id) })
    }
  ) {
    ResourceBox(model.scene) { value ->
      VerticalList {
        value.groupConfigs.forEach { (group, config) ->
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clickable { model.updateProgram(group, config) },
                contentAlignment = Alignment.Center
              ) {
                if (config != null) {
                  ColorListIcon(
                    modifier = Modifier
                      .size(40.dp),
                    program = config.program
                  )
                } else {
                  Icon(Icons.Default.Add)
                }
              }

              Column(modifier = Modifier.weight(1f)) {
                Subheader { Text("Group $group") }

                if (config != null) {
                  SliderListItem(
                    value = config.brightness,
                    onValueChangeFinished = { model.updateBrightness(group, it) },
                    stepPolicy = incrementingStepPolicy(0.05f),
                    title = { Text("Brightness") },
                    valueText = { Text("${(it * 100f).roundToInt()}") }
                  )

                  SliderListItem(
                    value = config.speed,
                    onValueChangeFinished = { model.updateSpeed(group, it) },
                    stepPolicy = incrementingStepPolicy(0.05f),
                    title = { Text("Speed") },
                    valueText = { Text("${(it * 100f).roundToInt()}") }
                  )

                  SwitchListItem(
                    value = config.musicMode,
                    onValueChange = { model.updateMusicMode(group, it) },
                    title = { Text("Music mode") }
                  )
                } else {
                  Text("unchanged")
                }
              }

              if (config != null)
                IconButton(onClick = { model.deleteGroupConfig(group) }) {
                  Icon(Icons.Default.Close)
                }
            }
          }
        }

        item {
          SwitchListItem(
            value = model.previewsEnabled,
            onValueChange = model.updatePreviewsEnabled,
            title = { Text("Preview") }
          )
        }
      }
    }
  }
}

data class SceneModel(
  val id: String,
  val scene: Resource<Scene>,
  val updateProgram: (Int, GroupConfig?) -> Unit,
  val updateBrightness: (Int, Float) -> Unit,
  val updateSpeed: (Int, Float) -> Unit,
  val updateMusicMode: (Int, Boolean) -> Unit,
  val deleteGroupConfig: (Int) -> Unit,
  val previewsEnabled: Boolean,
  val updatePreviewsEnabled: (Boolean) -> Unit
)

@Provide fun sceneModel(
  screen: SceneScreen,
  navigator: Navigator,
  programRepository: ProgramRepository,
  previewRepository: PreviewRepository,
  sceneRepository: SceneRepository
) = Model {
  val id = screen.id

  val scene by remember {
    sceneRepository.scene(id)
      .map { it!! }
  }.collectAsResourceState()

  LaunchedEffect(true) {
    previewRepository.providePreviews { _, update ->
      snapshotFlow { scene }
        .map {
          scene.getOrNull()
            ?.groupConfigs
            ?.mapValues { it.value?.copy(id = it.key.toString()) }
            ?.values
            ?.filterNotNull()
            ?: emptyList()
        }
        .collect(update)
    }
  }

  suspend fun updateScene(block: Scene.() -> Scene) {
    sceneRepository.updateScene(scene.get().block())
  }

  suspend fun updateGroupConfig(
    group: Int,
    block: GroupConfig.() -> GroupConfig
  ) {
    val groupConfig = (scene.get().groupConfigs.get(group) ?: GroupConfig()).block()
    updateScene {
      copy(
        groupConfigs = groupConfigs.toMutableMap()
          .apply { put(group, groupConfig) }
      )
    }
  }

  SceneModel(
    id = id,
    scene = scene,
    updateProgram = action { group, config ->
      navigator.push(
        ListScreen<Pair<String, Program?>>(
          buildList<Pair<String, Program?>> {
            add("Color" to null)
            addAll(
              programRepository.userPrograms.first()
                .sortedBy { it.id.toLowerCase() }
                .map { it.id to it }
            )
            add("Rainbow" to null)
          }
        ) { it.first }
      )
        ?.let { (id, program) ->
          val finalProgram = when (id) {
            "Color" -> navigator.push(
              ColorScreen(
                config?.program?.items?.singleOrNull()?.color?.copy(id = randomId()) ?: ApeColor()
              )
            )?.asProgram() ?: return@let

            "Rainbow" -> Program.RAINBOW
            else -> program!!
          }

          updateGroupConfig(group) { copy(program = finalProgram) }
        }
    },
    updateBrightness = action { group, brightness ->
      updateGroupConfig(group) { copy(brightness = brightness) }
    },
    updateSpeed = action { group, speed ->
      updateGroupConfig(group) { copy(speed = speed) }
    },
    updateMusicMode = action { group, musicMode ->
      updateGroupConfig(group) { copy(musicMode = musicMode) }
    },
    deleteGroupConfig = action { group ->
      updateScene {
        copy(groupConfigs = groupConfigs.toMutableMap().apply {
          put(group, null)
        })
      }
    },
    previewsEnabled = previewRepository.previewsEnabled.collectAsState().value,
    updatePreviewsEnabled = action { value -> previewRepository.updatePreviewsEnabled(value) }
  )
}
