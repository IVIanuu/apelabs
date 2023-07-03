/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalMaterialApi::class)

package com.ivianuu.apelabs.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
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
import com.ivianuu.apelabs.data.BuiltInColors
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.Program
import com.ivianuu.apelabs.data.Scene
import com.ivianuu.apelabs.data.WappState
import com.ivianuu.apelabs.data.asProgram
import com.ivianuu.apelabs.data.isUUID
import com.ivianuu.apelabs.data.merge
import com.ivianuu.apelabs.data.randomId
import com.ivianuu.apelabs.data.toApeColor
import com.ivianuu.apelabs.domain.ColorRepository
import com.ivianuu.apelabs.domain.ContentUsageRepository
import com.ivianuu.apelabs.domain.GroupConfigRepository
import com.ivianuu.apelabs.domain.LightRepository
import com.ivianuu.apelabs.domain.ProgramRepository
import com.ivianuu.apelabs.domain.SceneRepository
import com.ivianuu.apelabs.domain.WappRepository
import com.ivianuu.essentials.Resources
import com.ivianuu.essentials.app.AppForegroundState
import com.ivianuu.essentials.backup.BackupAndRestoreScreen
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.coroutines.infiniteEmptyFlow
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.collectAsResourceState
import com.ivianuu.essentials.resource.getOrElse
import com.ivianuu.essentials.resource.map
import com.ivianuu.essentials.time.milliseconds
import com.ivianuu.essentials.ui.dialog.ListScreen
import com.ivianuu.essentials.ui.dialog.TextInputScreen
import com.ivianuu.essentials.ui.material.ListItem
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.material.TopAppBar
import com.ivianuu.essentials.ui.material.guessingContentColorFor
import com.ivianuu.essentials.ui.material.incrementingStepPolicy
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.Navigator
import com.ivianuu.essentials.ui.navigation.RootScreen
import com.ivianuu.essentials.ui.navigation.Ui
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.popup.PopupMenuButton
import com.ivianuu.essentials.ui.popup.PopupMenuItem
import com.ivianuu.essentials.ui.prefs.SliderListItem
import com.ivianuu.essentials.ui.prefs.SwitchListItem
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.math.roundToInt

@Provide class HomeScreen : RootScreen

@Provide fun homeUi(resources: Resources) = Ui<HomeScreen, HomeModel> { model ->
  Scaffold(
    topBar = {
      Column {
        TopAppBar(
          title = { Text("Ape labs") },
          actions = {
            IconButton(onClick = {}) {
              Icon(
                painterResId = R.drawable.ic_bluetooth,
                tint = animateColorAsState(
                  if (model.wappState.map { it.isConnected }.getOrElse { false }) Color(0xFF0082FC)
                  else LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
                ).value
              )
            }

            PopupMenuButton {
              PopupMenuItem(onSelected = model.saveColor) { Text("Save color") }
              PopupMenuItem(onSelected = model.saveScene) { Text("Save scene") }
              PopupMenuItem(onSelected = model.openBackupRestore) { Text("Backup and restore") }
            }
          }
        )
      }
    },
    bottomBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .clickable { },
        elevation = 8.dp
      ) {
        Box(contentAlignment = Alignment.CenterStart) {
          FlowRow(
            modifier = Modifier.padding(16.dp),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp
          ) {
            LongClickChip(
              selected = model.groups.all { it in model.selectedGroups },
              onClick = model.toggleAllGroupSelections,
              onLongClick = null
            ) {
              Text("ALL")
            }

            model.groups.forEach { group ->
              LongClickChip(
                selected = group in model.selectedGroups,
                onClick = { model.toggleGroupSelection(group, false) },
                onLongClick = { model.toggleGroupSelection(group, true) }
              ) {
                Text(group.toString())
              }
            }
          }
        }
      }
    }
  ) {
    LazyVerticalGrid(
      columns = GridCells.Fixed(2)
    ) {
      if (model.selectedGroups.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Text(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            text = "Select a group to edit"
          )
        }
      } else {
        item(span = { GridItemSpan(maxLineSpan) }) {
          val programName = when {
            model.groupConfig.program.id == Program.RAINBOW.id -> "Rainbow"
            model.groupConfig.program.id.isUUID ->
              model.groupConfig.program.items.singleOrNull()
                ?.color
                ?.takeUnless { it.id.isUUID }
                ?.id
                ?: "Color"

            else -> model.groupConfig.program.id
          }

          ListItem(
            leading = {
              ColorListIcon(
                modifier = Modifier.size(40.dp),
                program = model.groupConfig.program
              )
            },
            title = { Text("Program") },
            subtitle = { Text(programName) }
          )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
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
              model.updateColorPickerColor(it)
            }
          }

          LaunchedEffect(model.groupConfig.program) {
            if (model.groupConfig.program.id != Program.colorPickerId(model.selectedGroups.toList()))
              controller.clear()
          }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
          SliderListItem(
            value = model.groupConfig.brightness,
            onValueChange = model.updateBrightness,
            stepPolicy = incrementingStepPolicy(0.05f),
            title = { Text("Brightness") },
            valueText = { Text("${(it * 100f).roundToInt()}") }
          )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
          SliderListItem(
            value = model.groupConfig.speed,
            onValueChange = model.updateSpeed,
            stepPolicy = incrementingStepPolicy(0.05f),
            title = { Text("Speed") },
            valueText = { Text("${(it * 100f).roundToInt()}") }
          )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
          SwitchListItem(
            value = model.groupConfig.musicMode,
            onValueChange = model.updateMusicMode,
            title = { Text("Music mode") }
          )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
          SwitchListItem(
            value = model.groupConfig.blackout,
            onValueChange = model.updateBlackout,
            title = { Text("Blackout") }
          )
        }
      }

      item(span = { GridItemSpan(maxLineSpan) }) {
        Subheader { Text("Programs / colors") }
      }

      val userColors = model.userColors.getOrElse { emptyList() }
      val userPrograms = model.userPrograms.getOrElse { emptyList() }

      fun Any.isCustom() = this in userColors || this in userPrograms

      buildList {
        addAll(userColors.map { it.id to it })
        addAll(userPrograms.map { it.id to it })
        add(Program.RAINBOW.id to Program.RAINBOW)
        addAll(model.builtInColors.map { it.id to it })
      }
        .sortedBy { it.first.lowercase() }
        .sortedByDescending { model.contentUsages[it.first] ?: -1f }
        .chunked(2)
        .forEach { row ->
          row.forEachIndexed { index, (id, item) ->
            item(key = item, span = {
              GridItemSpan(if (row.size == 1) maxLineSpan else 1)
            }) {
              fun <T> Any.map(
                color: (ApeColor) -> T,
                program: (Program) -> T
              ) = when (this) {
                is ApeColor -> color(this)
                is Program -> program(this)
                else -> throw AssertionError()
              }

              ListItem(
                modifier = Modifier
                  .animateItemPlacement()
                  .clickable {
                    item.map(
                      color = { model.updateColor(it) },
                      program = { model.updateProgram(it) }
                    )
                  },
                title = { Text(if (item === Program.RAINBOW) "Rainbow" else id) },
                leading = {
                  ColorListIcon(
                    modifier = Modifier.size(40.dp),
                    colors = item.map(
                      color = { listOf(it) },
                      program = { it.items.map { it.color } }
                    )
                  )
                },
                trailing = if (!item.isCustom()) null else ({
                  PopupMenuButton {
                    PopupMenuItem(onSelected = {
                      item.map(
                        color = { model.openColor(it) },
                        program = { model.openProgram(it) }
                      )
                    }) { Text("Open") }
                    PopupMenuItem(onSelected = {
                      item.map(
                        color = { model.deleteColor(it) },
                        program = { model.deleteProgram(it) }
                      )
                    }) { Text("Delete") }
                  }
                }),
                contentPadding = PaddingValues(
                  start = if (index == 0 || row.size == 1) 16.dp else 8.dp,
                  end = if (index == 1 || row.size == 1) 16.dp else 8.dp,
                ),
                textPadding = PaddingValues(start = 16.dp)
              )
            }
          }
        }

      item(span = { GridItemSpan(maxLineSpan) }) {
        Row(
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
          Button(
            modifier = Modifier.weight(0.5f),
            onClick = model.addColor
          ) { Text("ADD COLOR") }

          Button(
            modifier = Modifier.weight(0.5f),
            onClick = model.addProgram
          ) { Text("ADD PROGRAM") }
        }
      }

      item(span = { GridItemSpan(maxLineSpan) }) { Subheader { Text("Scenes") } }

      val scenes = model.scenes.getOrElse { emptyList() }
      scenes
        .sortedBy { it.id.lowercase() }
        .sortedByDescending { model.contentUsages[it.id] ?: -1f }
        .chunked(2)
        .forEach { row ->
          row.forEachIndexed { index, scene ->
            item(key = scene, span = {
              GridItemSpan(if (row.size == 1) maxLineSpan else 1)
            }) {
              ListItem(
                modifier = Modifier
                  .animateItemPlacement()
                  .clickable { model.applyScene(scene) },
                title = { Text(scene.id) },
                trailing = {
                  PopupMenuButton {
                    PopupMenuItem(onSelected = { model.openScene(scene) }) { Text("Open") }
                    PopupMenuItem(onSelected = { model.deleteScene(scene) }) { Text("Delete") }
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

      item(span = { GridItemSpan(maxLineSpan) }) {
        Button(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          onClick = model.addScene
        ) { Text("ADD SCENE") }
      }

      val wappState = model.wappState.getOrElse { WappState() }
      val lights = model.lights.getOrElse { emptyList() }

      if (wappState.isConnected || lights.isNotEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Subheader { Text("Devices ${lights.size}") }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
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
                    selected = light.id in model.selectedLights,
                    onClick = { model.toggleLightSelection(light) },
                    onLongClick = { model.toggleLightSelection(light) }
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

        item(span = { GridItemSpan(maxLineSpan) }) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
          ) {
            Button(
              modifier = Modifier.weight(1f),
              onClick = model.refreshLights
            ) { Text("REFRESH") }

            Button(
              modifier = Modifier.weight(1f),
              enabled = model.selectedLights.isNotEmpty(),
              onClick = model.regroupLights
            ) { Text("REGROUP") }
          }
        }
      }

      item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(56.dp)) }
    }
  }
}

@Composable private fun LongClickChip(
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  content: @Composable () -> Unit
) {
  val targetBackgroundColor = if (selected) MaterialTheme.colors.secondary
  else LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
  val backgroundColor by animateColorAsState(targetBackgroundColor)
  val contentColor by animateColorAsState(guessingContentColorFor(targetBackgroundColor))
  Surface(
    modifier = Modifier
      .height(32.dp)
      .defaultMinSize(minWidth = 56.dp),
    shape = RoundedCornerShape(50),
    color = backgroundColor,
    contentColor = contentColor
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
  val refreshLights: () -> Unit,
  val selectedLights: Set<Int>,
  val toggleLightSelection: (Light) -> Unit,
  val regroupLights: () -> Unit,
  val userColors: Resource<List<ApeColor>>,
  val updateColor: (ApeColor) -> Unit,
  val openColor: (ApeColor) -> Unit,
  val addColor: () -> Unit,
  val deleteColor: (ApeColor) -> Unit,
  val saveColor: () -> Unit,
  val updateColorPickerColor: (Color) -> Unit,
  val contentUsages: Map<String, Float>,
  val builtInColors: List<ApeColor> = BuiltInColors,
  val userPrograms: Resource<List<Program>>,
  val updateProgram: (Program) -> Unit,
  val openProgram: (Program) -> Unit,
  val addProgram: () -> Unit,
  val deleteProgram: (Program) -> Unit,
  val scenes: Resource<List<Scene>>,
  val applyScene: (Scene) -> Unit,
  val openScene: (Scene) -> Unit,
  val addScene: () -> Unit,
  val deleteScene: (Scene) -> Unit,
  val saveScene: () -> Unit,
  val openBackupRestore: () -> Unit
)

@Provide fun homeModel(
  appForegroundState: Flow<AppForegroundState>,
  colorRepository: ColorRepository,
  contentUsageRepository: ContentUsageRepository,
  groupConfigRepository: GroupConfigRepository,
  lightRepository: LightRepository,
  navigator: Navigator,
  programRepository: ProgramRepository,
  sceneRepository: SceneRepository,
  wappRepository: WappRepository,
  pref: DataStore<ApeLabsPrefs>
) = Model {
  val prefs by pref.data.collectAsState(ApeLabsPrefs(selectedGroups = GROUPS.toSet()))

  val selectedGroupConfigs by groupConfigRepository.selectedGroupConfigs
    .collectAsState(emptyList())

  val groupConfig = selectedGroupConfigs
    .merge()

  suspend fun updateConfig(block: GroupConfig.() -> GroupConfig) {
    groupConfigRepository.updateGroupConfigs(
      selectedGroupConfigs
        .map { it.block() },
      false
    )
  }

  val lights by remember {
    appForegroundState
      .flatMapLatest {
        if (it == AppForegroundState.FOREGROUND) lightRepository.lights
        else infiniteEmptyFlow()
      }
  }.collectAsResourceState()
  var selectedLights by remember { mutableStateOf(emptySet<Int>()) }

  LaunchedEffect(selectedLights) {
    lightRepository.flashLights(selectedLights.toList())
  }

  val colorPickerId = Program.colorPickerId(prefs.selectedGroups.toList())

  val colorPickerColor = remember(colorPickerId) { programRepository.program(colorPickerId) }
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
    wappState = remember {
      appForegroundState
        .flatMapLatest {
          if (it == AppForegroundState.FOREGROUND) wappRepository.wappState
          else infiniteEmptyFlow()
        }
    }.collectAsResourceState().value,
    lights = lights,
    refreshLights = action { lightRepository.refreshLights() },
    selectedLights = selectedLights,
    toggleLightSelection = action { light ->
      selectedLights = selectedLights.toMutableSet().apply {
        if (light.id in this) remove(light.id)
        else add(light.id)
      }
    },
    regroupLights = action {
      navigator.push(ListScreen(items = GROUPS) { it.toString() })
        ?.let { group ->
          lightRepository.regroupLights(
            selectedLights.toList()
              .also { selectedLights = emptySet() }, group
          )
        }
    },
    contentUsages = contentUsageRepository.contentUsages.collectAsState(emptyMap()).value,
    userColors = colorRepository.userColors.collectAsResourceState().value,
    updateColor = action { color ->
      color
        .asProgram(colorPickerId)
        .let {
          programRepository.updateProgram(it)
          updateConfig { copy(program = it) }
        }
      contentUsageRepository.contentUsed(color.id)
    },
    openColor = action { color -> navigator.push(ColorScreen(color)) },
    addColor = action {
      navigator.push(TextInputScreen(label = "Name.."))
        ?.let { id ->
          navigator.push(ColorScreen(colorRepository.createColor(id)))
          contentUsageRepository.contentUsed(id)
        }
    },
    deleteColor = action { color -> colorRepository.deleteColor(color.id) },
    saveColor = action {
      navigator.push(TextInputScreen(label = "Name.."))
        ?.let { id ->
          val color = colorPickerColor.copy(id = id)
          colorRepository.updateColor(color)
          color
            .asProgram(colorPickerId)
            .let {
              programRepository.updateProgram(it)
              updateConfig { copy(program = it) }
            }
          contentUsageRepository.contentUsed(color.id)
        }
    },
    updateColorPickerColor = action { composeColor ->
      composeColor
        .toApeColor(colorPickerId)
        .asProgram(colorPickerId)
        .let {
          programRepository.updateProgram(it)
          updateConfig { copy(program = it) }
        }
    },
    userPrograms = programRepository.userPrograms.collectAsResourceState().value,
    updateProgram = action { program ->
      updateConfig { copy(program = program) }
      contentUsageRepository.contentUsed(program.id)
    },
    openProgram = action { program -> navigator.push(ProgramScreen(program.id)) },
    addProgram = action {
      navigator.push(TextInputScreen(label = "Name.."))
        ?.let { id ->
          navigator.push(ProgramScreen(programRepository.createProgram(id).id))
          contentUsageRepository.contentUsed(id)
        }
    },
    deleteProgram = action { program -> programRepository.deleteProgram(program.id) },
    scenes = sceneRepository.userScenes.collectAsResourceState().value,
    applyScene = action { scene ->
      groupConfigRepository.updateGroupConfigs(
        scene.groupConfigs
          .filterValues { it != null }
          .map { it.value!!.copy(id = it.key.toString()) },
        false
      )
      contentUsageRepository.contentUsed(scene.id)
    },
    openScene = action { scene -> navigator.push(SceneScreen(scene.id)) },
    addScene = action {
      navigator.push(TextInputScreen(label = "Name.."))
        ?.let { navigator.push(SceneScreen(sceneRepository.createScene(it).id)) }
    },
    deleteScene = action { scene -> sceneRepository.deleteScene(scene.id) },
    saveScene = action {
      navigator.push(TextInputScreen(label = "Name.."))
        ?.let { id ->
          sceneRepository.updateScene(
            Scene(
              id = id,
              groupConfigs = groupConfigRepository.groupConfigs
                .first()
                .map {
                  if (!it.program.id.isUUID) it
                  else it.copy(program = it.program.copy(id = randomId()))
                }
                .associate { it.id.toInt() to it.copy(id = randomId()) }
            )
          )
          contentUsageRepository.contentUsed(id)
        }
    },
    openBackupRestore = action { navigator.push(BackupAndRestoreScreen()) }
  )
}
