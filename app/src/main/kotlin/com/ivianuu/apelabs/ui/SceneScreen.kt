package com.ivianuu.apelabs.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.apelabs.domain.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.resource.*
import com.ivianuu.essentials.ui.common.*
import com.ivianuu.essentials.ui.dialog.*
import com.ivianuu.essentials.ui.material.*
import com.ivianuu.essentials.ui.navigation.*
import com.ivianuu.essentials.ui.prefs.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

data class SceneScreen(val id: String) : Screen<Unit>

@Provide fun sceneUi(
  screen: SceneScreen,
  navigator: Navigator,
  programRepository: ProgramRepository,
  previewRepository: PreviewRepository,
  sceneRepository: SceneRepository
) = Ui<SceneScreen> {
  val id = screen.id

  val scene = sceneRepository.scene(id)
    .map { it!! }
    .scopedResourceState()

  previewRepository.Previews(scene) {
    scene.getOrNull()
      ?.groupConfigs
      ?.mapValues { it.value?.copy(id = it.key.toString()) }
      ?.values
      ?.filterNotNull()
      ?: emptyList()
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

  ScreenScaffold(topBar = { AppBar { Text(screen.id) } }) {
    ResourceBox(scene) { value ->
      VerticalList {
        items(value.groupConfigs.toList()) { (group, config) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clickable(onClick = scopedAction {
                  navigator.push(
                    ListScreen<Pair<String, Program?>>(
                      buildList {
                        add("Color" to null)
                        addAll(
                          programRepository.userPrograms.first()
                            .sortedBy { it.id.lowercase() }
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
                }),
              contentAlignment = Alignment.Center
            ) {
              if (config != null)
                ColorListIcon(
                  modifier = Modifier.size(40.dp),
                  program = config.program
                )
              else
                Icon(Icons.Default.Add, null)
            }

            Column(modifier = Modifier.weight(1f)) {
              Subheader { Text("Group $group") }

              if (config != null) {
                SliderListItem(
                  value = config.brightness,
                  onValueChangeFinished = scopedAction { value ->
                    updateGroupConfig(group) { copy(brightness = value) }
                  },
                  stepPolicy = incrementingStepPolicy(0.05f),
                  title = { Text("Brightness") },
                  valueText = { Text("${(it * 100f).roundToInt()}") }
                )

                SliderListItem(
                  value = config.speed,
                  onValueChangeFinished = scopedAction { value ->
                    updateGroupConfig(group) { copy(speed = value) }
                  },
                  stepPolicy = incrementingStepPolicy(0.05f),
                  title = { Text("Speed") },
                  valueText = { Text("${(it * 100f).roundToInt()}") }
                )

                SingleChoiceToggleButtonGroupListItem(
                  selected = config.mode,
                  values = GroupConfig.Mode.entries,
                  onSelectionChanged = scopedAction { value ->
                    updateGroupConfig(group) { copy(mode = value) }
                  },
                  title = { Text("Mode") }
                )
              } else {
                Text("unchanged")
              }
            }

            if (config != null)
              IconButton(
                modifier = Modifier.align(Alignment.Top),
                onClick = scopedAction {
                  updateScene {
                    copy(groupConfigs = groupConfigs.toMutableMap().apply {
                      put(group, null)
                    })
                  }
                }
              ) {
                Icon(Icons.Default.Close, null)
              }
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
