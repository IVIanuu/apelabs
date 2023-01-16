/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.BuiltInColors
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.Scene
import com.ivianuu.apelabs.domain.WappRepository
import com.ivianuu.apelabs.data.WappState
import com.ivianuu.apelabs.data.asProgram
import com.ivianuu.apelabs.data.deleteProgram
import com.ivianuu.apelabs.data.merge
import com.ivianuu.apelabs.domain.LightRepository
import com.ivianuu.apelabs.data.deleteScene
import com.ivianuu.apelabs.data.programs
import com.ivianuu.apelabs.data.scenes
import com.ivianuu.apelabs.data.updateGroupConfigs
import com.ivianuu.apelabs.data.updateProgram
import com.ivianuu.apelabs.data.updateScene
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.compose.bind
import com.ivianuu.essentials.compose.bindResource
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.getOrElse
import com.ivianuu.essentials.time.seconds
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.dialog.ListKey
import com.ivianuu.essentials.ui.dialog.TextInputKey
import com.ivianuu.essentials.ui.material.ListItem
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.material.TopAppBar
import com.ivianuu.essentials.ui.material.guessingContentColorFor
import com.ivianuu.essentials.ui.material.incrementingStepPolicy
import com.ivianuu.essentials.ui.navigation.KeyUiContext
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.ModelKeyUi
import com.ivianuu.essentials.ui.navigation.RootKey
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.popup.PopupMenuButton
import com.ivianuu.essentials.ui.popup.PopupMenuItem
import com.ivianuu.essentials.ui.prefs.SliderListItem
import com.ivianuu.essentials.ui.prefs.SwitchListItem
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Provide object HomeKey : RootKey

@Provide val homeUi = ModelKeyUi<HomeKey, HomeModel> {
  Scaffold(
    topBar = { TopAppBar(title = { Text("Ape labs") }) }
  ) {
    VerticalList {
      item {
        FlowRow(
          modifier = Modifier.padding(8.dp),
          mainAxisSpacing = 8.dp,
          crossAxisSpacing = 8.dp
        ) {
          LongClickChip(
            selected = groups.all { it in selectedGroups },
            onClick = toggleAllGroupSelections,
            onLongClick = null
          ) {
            Text("ALL")
          }

          groups.forEach { group ->
            LongClickChip(
              selected = group in selectedGroups,
              onClick = { toggleGroupSelection(group, false) },
              onLongClick = { toggleGroupSelection(group, true) }
            ) {
              Text(group.toString())
            }
          }
        }
      }

      if (selectedGroups.isEmpty()) {
        item {
          Text(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            text = "Select a group to edit"
          )
        }
      } else {
        item {
          SliderListItem(
            value = groupConfig.brightness,
            onValueChange = updateBrightness,
            stepPolicy = incrementingStepPolicy(0.05f),
            title = { Text("Brightness") },
            valueText = { Text("${(it * 100f).roundToInt()}") }
          )
        }

        item {
          SliderListItem(
            value = groupConfig.speed,
            onValueChange = updateSpeed,
            stepPolicy = incrementingStepPolicy(0.05f),
            title = { Text("Speed") },
            valueText = { Text("${(it * 100f).roundToInt()}") }
          )
        }

        item {
          SwitchListItem(
            value = groupConfig.musicMode,
            onValueChange = updateMusicMode,
            title = { Text("Music mode") }
          )
        }

        item {
          SwitchListItem(
            value = groupConfig.blackout,
            onValueChange = updateBlackout,
            title = { Text("Blackout") }
          )
        }
      }

      val wappState = wappState.getOrElse { WappState() }
      val lights = lights.getOrElse { emptyList() }

      if (wappState.isConnected || lights.isNotEmpty()) {
        item {
          Subheader { Text("Devices") }
        }

        item {
          FlowRow(
            modifier = Modifier
              .padding(16.dp),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
            crossAxisAlignment = FlowCrossAxisAlignment.Center
          ) {
            LongClickChip(
              selected = false,
              onClick = {},
              onLongClick = null
            ) {
              Text(
                buildString {
                  append("Wapp")
                  if (wappState.battery != null) {
                    if (wappState.battery < 0f) {
                      append(", charging")
                    } else {
                      append(", bat ${(wappState.battery * 100).toInt()}%")
                    }
                  }
                }
              )
            }

            lights
              .groupBy { it.group }
              .mapValues { it.value.sortedBy { it.id } }
              .toList()
              .sortedBy { it.first }
              .forEach { (group, groupLights) ->
                Text("#$group")
                groupLights.forEach { light ->
                  LongClickChip(
                    selected = light.id in selectedLights,
                    onClick = { toggleLightSelection(light) },
                    onLongClick = { toggleLightSelection(light) }
                  ) {
                    Text(
                      buildString {
                        append(
                          "${
                            light.type?.name?.toLowerCase()?.capitalize() ?: "Light"
                          } ${light.id}"
                        )
                        if (light.battery != null) {
                          if (light.battery < 0f) {
                            append(", charging")
                          } else {
                            append(", bat ${(light.battery * 100).toInt()}%")
                          }
                        }
                      }
                    )
                  }
                }
              }
          }
        }

        if (selectedLights.isNotEmpty()) {
          item {
            Button(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
              onClick = regroupLights
            ) { Text("REGROUP") }
          }
        }
      }

      item {
        Subheader { Text("Programs") }
      }

      item {
        ListItem(
          modifier = Modifier.clickable(onClick = updateColor),
          title = { Text("Color") },
          leading = {
            ColorListIcon(
              modifier = Modifier.size(40.dp),
              colors = remember { listOf(BuiltInColors.toList().shuffled().first().second) }
            )
          }
        )
      }

      val programs = programs.getOrElse { emptyMap() }
      programs
        .toList()
        .sortedBy { it.first }
        .forEach { (id, program) ->
          item {
            ListItem(
              modifier = Modifier.clickable { updateProgram(program) },
              title = { Text(id) },
              leading = {
                ColorListIcon(
                  modifier = Modifier.size(40.dp),
                  program = program
                )
              },
              trailing = {
                PopupMenuButton {
                  PopupMenuItem(onSelected = { openProgram(id) }) { Text("Open") }
                  PopupMenuItem(onSelected = { deleteProgram(id) }) { Text("Delete") }
                }
              }
            )
          }
        }

      item {
        ListItem(
          modifier = Modifier.clickable { updateProgram(Program.RAINBOW) },
          title = { Text("Rainbow") },
          leading = {
            ColorListIcon(
              modifier = Modifier.size(40.dp),
              program = Program.RAINBOW
            )
          }
        )
      }

      item {
        Button(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          onClick = addProgram
        ) { Text("ADD PROGRAM") }
      }

      item {
        Subheader { Text("Scenes") }
      }

      val scenes = scenes.getOrElse { emptyMap() }
      scenes
        .toList()
        .sortedBy { it.first }
        .forEach { (id, scene) ->
          item {
            ListItem(
              modifier = Modifier.clickable { applyScene(scene) },
              title = { Text(id) },
              trailing = {
                PopupMenuButton {
                  PopupMenuItem(onSelected = { openScene(id) }) { Text("Open") }
                  PopupMenuItem(onSelected = { deleteScene(id) }) { Text("Delete") }
                }
              }
            )
          }
        }

      item {
        Button(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          onClick = addScene
        ) { Text("ADD SCENE") }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun LongClickChip(
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  content: @Composable () -> Unit
) {
  val backgroundColor = if (selected) MaterialTheme.colors.secondary
  else LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
  Surface(
    modifier = Modifier
      .height(32.dp)
      .defaultMinSize(minWidth = 56.dp),
    shape = RoundedCornerShape(50),
    color = backgroundColor,
    contentColor = guessingContentColorFor(backgroundColor)
  ) {
    Box(
      modifier = Modifier
        .combinedClickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = LocalIndication.current,
          onClick = onClick,
          onLongClick = onLongClick
        )
        .padding(horizontal = 8.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.button,
        content = content
      )
    }
  }
}

data class HomeModel(
  val groups: List<Int>,
  val selectedGroups: Set<Int>,
  val toggleGroupSelection: (Int, Boolean) -> Unit,
  val toggleAllGroupSelections: () -> Unit,
  val groupConfig: GroupConfig,
  val updateBrightness: (Float) -> Unit,
  val updateSpeed: (Float) -> Unit,
  val updateMusicMode: (Boolean) -> Unit,
  val updateBlackout: (Boolean) -> Unit,
  val wappState: Resource<WappState>,
  val lights: Resource<List<Light>>,
  val selectedLights: Set<String>,
  val toggleLightSelection: (Light) -> Unit,
  val regroupLights: () -> Unit,
  val programs: Resource<Map<String, Program>>,
  val updateColor: () -> Unit,
  val updateProgram: (Program) -> Unit,
  val openProgram: (String) -> Unit,
  val addProgram: () -> Unit,
  val deleteProgram: (String) -> Unit,
  val scenes: Resource<Map<String, Scene>>,
  val applyScene: (Scene) -> Unit,
  val openScene: (String) -> Unit,
  val addScene: () -> Unit,
  val deleteScene: (String) -> Unit
)

context(ApeLabsPrefsContext, LightRepository, Logger, KeyUiContext<HomeKey>, WappRepository)
    @Provide fun homeModel() = Model {
  val prefs = pref.data.bind(ApeLabsPrefs())

  val groupConfig = prefs.selectedGroups
    .map { prefs.groupConfigs[it]!! }
    .merge()

  suspend fun updateConfig(block: GroupConfig.() -> GroupConfig) {
    updateGroupConfigs(
      prefs.selectedGroups
        .associateWith { prefs.groupConfigs[it]!!.block() }
    )
  }

  val lights = lights.bindResource()
  var selectedLights by remember { mutableStateOf(emptySet<String>()) }

  LaunchedEffect(selectedLights) {
    selectedLights.parForEach { lightId ->
      while (coroutineContext.isActive) {
        flashLight(lightId)
        delay(1.seconds)
      }
    }
  }

  HomeModel(
    groups = GROUPS,
    selectedGroups = prefs.selectedGroups,
    toggleGroupSelection = action { group, longClick ->
      pref.updateData {
        copy(
          selectedGroups = if (!longClick) setOf(group)
          else selectedGroups.toMutableSet().apply {
            if (group in this) remove(group)
            else add(group)
          }
        )
      }
    },
    toggleAllGroupSelections = action {
      pref.updateData {
        copy(
          selectedGroups = if (GROUPS.all { it in selectedGroups }) emptySet()
          else GROUPS.toSet()
        )
      }
    },
    groupConfig = groupConfig,
    updateBrightness = action { value ->
      updateConfig { copy(brightness = value) }
    },
    updateSpeed = action { value ->
      updateConfig { copy(speed = value) }
    },
    updateMusicMode = action { value ->
      updateConfig { copy(musicMode = value) }
    },
    updateBlackout = action { value ->
      updateConfig { copy(blackout = value) }
    },
    wappState = wappState.bindResource(),
    lights = lights,
    selectedLights = selectedLights,
    toggleLightSelection = action { light ->
      selectedLights = selectedLights.toMutableSet().apply {
        if (light.id in this) remove(light.id)
        else add(light.id)
      }
    },
    regroupLights = action {
      navigator.push(ListKey(items = GROUPS) { toString() })
        ?.let { group ->
          selectedLights.parForEach { regroupLight(it, group) }
          selectedLights = emptySet()
        }
    },
    programs = programs.bindResource(),
    updateColor = action {
      navigator.push(ColorKey(groupConfig.program.items.singleOrNull()?.color ?: ApeColor()))
        ?.let {
          val program = it.asProgram()
          updateConfig { copy(program = program) }
        }
    },
    updateProgram = action { program -> updateConfig { copy(program = program) } },
    openProgram = action { programId -> navigator.push(ProgramKey(programId)) },
    addProgram = action {
      navigator.push(TextInputKey(label = "Name.."))
        ?.let {
          updateProgram(it, Program())
          navigator.push(ProgramKey(it))
        }
    },
    deleteProgram = action { programId -> deleteProgram(programId) },
    scenes = scenes.bindResource(),
    applyScene = action { scene ->
      updateGroupConfigs(
        scene.groupConfigs
          .filterValues { it != null }
          .mapValues { it.value!! }
      )
    },
    openScene = action { sceneId -> navigator.push(SceneKey(sceneId)) },
    addScene = action {
      navigator.push(TextInputKey(label = "Name.."))
        ?.let {
          updateScene(it, Scene())
          navigator.push(SceneKey(it))
        }
    },
    deleteScene = action { sceneId -> deleteScene(sceneId) }
  )
}
