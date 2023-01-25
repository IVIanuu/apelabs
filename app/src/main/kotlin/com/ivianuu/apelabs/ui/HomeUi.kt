/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalMaterialApi::class)

package com.ivianuu.apelabs.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.ivianuu.apelabs.R
import com.ivianuu.apelabs.data.ApeColor
import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.Scene
import com.ivianuu.apelabs.data.WappState
import com.ivianuu.apelabs.data.asProgram
import com.ivianuu.apelabs.data.isUUID
import com.ivianuu.apelabs.data.merge
import com.ivianuu.apelabs.data.toApeColor
import com.ivianuu.apelabs.domain.ColorRepository
import com.ivianuu.apelabs.domain.GroupConfigRepository
import com.ivianuu.apelabs.domain.LightRepository
import com.ivianuu.apelabs.domain.ProgramRepository
import com.ivianuu.apelabs.domain.SceneRepository
import com.ivianuu.apelabs.domain.WappRepository
import com.ivianuu.essentials.ResourceProvider
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.compose.bindResource
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
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

context(ResourceProvider) @Provide val homeUi
  get() = ModelKeyUi<HomeKey, HomeModel> {
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
          val controller = remember { ColorPickerController() }

          ImageColorPicker(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp)
              .padding(horizontal = 16.dp),
            controller = controller
          )

          LaunchedEffect(controller.selectedColor) {
            controller.selectedColor?.let {
              updateColorPickerColor(it)
            }
          }

          LaunchedEffect(groupConfig.program) {
            if (groupConfig.program.id != Program.colorPickerId(selectedGroups.toList()))
              controller.clear()
          }
        }

        item {
          val programName = when {
            groupConfig.program.id == Program.RAINBOW.id -> "Rainbow"
            groupConfig.program.id.isUUID ->
              groupConfig.program.items.singleOrNull()
                ?.color
                ?.takeUnless { it.id.isUUID }
                ?.id
                ?: "Color"
            else -> groupConfig.program.id
          }

          ListItem(
            leading = {
              ColorListIcon(
                modifier = Modifier.size(40.dp),
                program = groupConfig.program
              )
            },
            title = { Text("Program") },
            subtitle = { Text(programName) }
          )
        }

        item {
          SliderListItem(
            value = groupConfig.brightness,
            onValueChangeFinished = updateBrightness,
            stepPolicy = incrementingStepPolicy(0.05f),
            title = { Text("Brightness") },
            valueText = { Text("${(it * 100f).roundToInt()}") }
          )
        }

        item {
          SliderListItem(
            value = groupConfig.speed,
            onValueChangeFinished = updateSpeed,
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

      item {
        Subheader { Text("Programs") }
      }

      item {
        Row {
          ListItem(
            modifier = Modifier
              .weight(0.5f)
              .clickable(onClick = updateColor),
            title = { Text("Color") },
            leading = {
              ColorListIcon(
                modifier = Modifier.size(40.dp),
                colors = listOf(colorPickerColor)
              )
            },
            contentPadding = PaddingValues(
              start = 16.dp,
              end = 8.dp
            ),
          )

          ListItem(
            modifier = Modifier
              .weight(0.5f)
              .clickable { updateProgram(Program.RAINBOW) },
            title = { Text("Rainbow") },
            leading = {
              ColorListIcon(
                modifier = Modifier.size(40.dp),
                program = Program.RAINBOW
              )
            },
            contentPadding = PaddingValues(
              start = 8.dp,
              end = 16.dp
            )
          )
        }
      }

      val programs = programs.getOrElse { emptyList() }
        programs
          .sortedBy { it.id }
          .chunked(2)
          .forEach { row ->
            item {
              Row {
                row.forEachIndexed { index, program ->
                  ListItem(
                    modifier = Modifier
                      .weight(0.5f)
                      .clickable { updateProgram(program) },
                    title = { Text(program.id) },
                    leading = {
                      ColorListIcon(
                        modifier = Modifier.size(40.dp),
                        program = program
                      )
                    },
                    trailing = {
                      PopupMenuButton {
                        PopupMenuItem(onSelected = { openProgram(program) }) { Text("Open") }
                        PopupMenuItem(onSelected = { deleteProgram(program) }) { Text("Delete") }
                      }
                    },
                    contentPadding = PaddingValues(
                      start = if (index == 0 || row.size == 1) 16.dp else 8.dp,
                      end = if (index == 1 || row.size == 1) 16.dp else 8.dp,
                    ),
                    textPadding = PaddingValues(start = 16.dp)
                  )
                }
              }
            }
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

      val scenes = scenes.getOrElse { emptyList() }
      scenes
        .sortedBy { it.id }
        .forEach { scene ->
          item {
            ListItem(
              modifier = Modifier.clickable { applyScene(scene) },
              title = { Text(scene.id) },
              trailing = {
                PopupMenuButton {
                  PopupMenuItem(onSelected = { openScene(scene) }) { Text("Open") }
                  PopupMenuItem(onSelected = { deleteScene(scene) }) { Text("Delete") }
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

        val wappState = wappState.getOrElse { WappState() }
        val lights = lights.getOrElse { emptyList() }

        if (wappState.isConnected || lights.isNotEmpty()) {
          item {
            Subheader { Text("Devices") }
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

                    if (wappState.id != null)
                      append(" ${wappState.id}")

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
  val selectedLights: Set<Int>,
  val toggleLightSelection: (Light) -> Unit,
  val regroupLights: () -> Unit,
  val colorPickerColor: ApeColor,
  val programs: Resource<List<Program>>,
  val updateColorPickerColor: (Color) -> Unit,
  val updateColor: () -> Unit,
  val updateProgram: (Program) -> Unit,
  val openProgram: (Program) -> Unit,
  val addProgram: () -> Unit,
  val deleteProgram: (Program) -> Unit,
  val scenes: Resource<List<Scene>>,
  val applyScene: (Scene) -> Unit,
  val openScene: (Scene) -> Unit,
  val addScene: () -> Unit,
  val deleteScene: (Scene) -> Unit
)

context(ApeLabsPrefsContext, ColorRepository, GroupConfigRepository, LightRepository,
KeyUiContext<HomeKey>, ProgramRepository, SceneRepository, WappRepository)
    @Provide fun homeModel() = Model {
  val prefs by pref.data.collectAsState(ApeLabsPrefs())

  val selectedGroupConfigs by remember { selectedGroupConfigs }
    .collectAsState(emptyList())

  val groupConfig = selectedGroupConfigs
    .merge()

  suspend fun updateConfig(block: GroupConfig.() -> GroupConfig) {
    updateGroupConfigs(
      selectedGroupConfigs
        .map { it.block() },
      false
    )
  }

  val lights = lights.bindResource()
  var selectedLights by remember { mutableStateOf(emptySet<Int>()) }

  LaunchedEffect(selectedLights) {
    selectedLights.parForEach { lightId ->
      while (coroutineContext.isActive) {
        flashLight(lightId)
        delay(2.seconds)
      }
    }
  }

  val colorPickerId = Program.colorPickerId(prefs.selectedGroups.toList())

  val colorPickerColor = remember { program(colorPickerId) }
    .collectAsState(null)
    .value
    ?.items
    ?.singleOrNull()
    ?.color
    ?: ApeColor(id = colorPickerId, white = 1f)

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
          selectedLights.forEach { regroupLight(it, group) }
          selectedLights = emptySet()
        }
    },
    programs = userPrograms.bindResource(),
    colorPickerColor = colorPickerColor,
    updateColorPickerColor = action { composeColor ->
      composeColor
        .toApeColor(colorPickerId)
        .let {
          if (it.red >= 0.95f && it.green >= 0.95f && it.blue >= 0.95f)
            ApeColor(id = it.id, white = 1f)
          else it
        }
        .asProgram(colorPickerId)
        .let {
          updateProgram(it)
          updateConfig { copy(program = it) }
        }
    },
    updateColor = action {
      navigator.push(ColorKey(colorPickerColor.copy(id = colorPickerId)))
        ?.asProgram(colorPickerId)
        ?.let {
          updateProgram(it)
          updateConfig { copy(program = it) }
        }
    },
    updateProgram = action { program -> updateConfig { copy(program = program) } },
    openProgram = action { program -> navigator.push(ProgramKey(program.id)) },
    addProgram = action {
      navigator.push(TextInputKey(label = "Name.."))
        ?.let { navigator.push(ProgramKey(createProgram(it).id)) }
    },
    deleteProgram = action { program -> deleteProgram(program.id) },
    scenes = userScenes.bindResource(),
    applyScene = action { scene ->
      updateGroupConfigs(
        scene.groupConfigs
          .filterValues { it != null }
          .map { it.value!!.copy(id = it.key.toString()) },
        false
      )
    },
    openScene = action { scene -> navigator.push(SceneKey(scene.id)) },
    addScene = action {
      navigator.push(TextInputKey(label = "Name.."))
        ?.let { navigator.push(SceneKey(createScene(it).id)) }
    },
    deleteScene = action { scene -> deleteScene(scene.id) }
  )
}
