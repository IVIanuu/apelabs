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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
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
import com.ivianuu.apelabs.domain.WappRemote
import com.ivianuu.apelabs.domain.WappRepository
import com.ivianuu.essentials.Resources
import com.ivianuu.essentials.app.AppForegroundState
import com.ivianuu.essentials.backup.BackupAndRestoreKey
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.compose.bind
import com.ivianuu.essentials.compose.bindResource
import com.ivianuu.essentials.coroutines.infiniteEmptyFlow
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.getOrElse
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.math.roundToInt

@Provide object HomeKey : RootKey

@OptIn(ExperimentalFoundationApi::class)
@Provide fun homeUi(resources: Resources) = ModelKeyUi<HomeKey, HomeModel> {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Ape labs") },
        actions = {
          PopupMenuButton {
            PopupMenuItem(onSelected = saveColor) {
              Text("Save color")
            }
            PopupMenuItem(onSelected = saveScene) {
              Text("Save scene")
            }
            PopupMenuItem(onSelected = openBackupRestore) {
              Text("Backup and restore")
            }
          }
        }
      )
    }
  ) {
    VerticalList {
      stickyHeader {
        FlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .clickable { }
            .padding(8.dp),
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
        Subheader { Text("Programs / colors") }
      }

      val userColors = userColors.getOrElse { emptyList() }
      val userPrograms = userPrograms.getOrElse { emptyList() }

      fun Any.isCustom() = this in userColors || this in userPrograms

      buildList {
        addAll(userColors.map { it.id to it })
        addAll(userPrograms.map { it.id to it })
        add(Program.RAINBOW.id to Program.RAINBOW)
        addAll(builtInColors.map { it.id to it })
      }
        .sortedBy { it.first.lowercase() }
        .sortedByDescending { contentUsages[it.first] ?: 0.0 }
        .chunked(2)
        .forEach { row ->
          item {
            Row {
              row.forEachIndexed { index, (id, item) ->
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
                    .weight(0.5f)
                    .animateItemPlacement()
                    .clickable {
                      item.map(
                        color = { updateColor(it) },
                        program = { updateProgram(it) }
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
                          color = { openColor(it) },
                          program = { openProgram(it) }
                        )
                      }) { Text("Open") }
                      PopupMenuItem(onSelected = {
                        item.map(
                          color = { deleteColor(it) },
                          program = { deleteProgram(it) }
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
        }

      item {
        Row(
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
          Button(
            modifier = Modifier.weight(0.5f),
            onClick = addColor
          ) { Text("ADD COLOR") }

          Button(
            modifier = Modifier.weight(0.5f),
            onClick = addProgram
          ) { Text("ADD PROGRAM") }
        }
      }

      item { Subheader { Text("Scenes") } }

      val scenes = scenes.getOrElse { emptyList() }
      scenes
        .sortedBy { it.id.lowercase() }
        .sortedByDescending { contentUsages[it.id] ?: 0.0 }
        .chunked(2)
        .forEach { row ->
          item {
            Row {
              row.forEachIndexed { index, scene ->
                ListItem(
                  modifier = Modifier
                    .weight(0.5f)
                    .animateItemPlacement()
                    .clickable { applyScene(scene) },
                  title = { Text(scene.id) },
                  trailing = {
                    PopupMenuButton {
                      PopupMenuItem(onSelected = { openScene(scene) }) { Text("Open") }
                      PopupMenuItem(onSelected = { deleteScene(scene) }) { Text("Delete") }
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
          onClick = addScene
        ) { Text("ADD SCENE") }
      }

      val wappState = wappState.getOrElse { WappState() }
      val lights = lights.getOrElse { emptyList() }

      if (wappState.isConnected || lights.isNotEmpty()) {
        item {
          Subheader { Text("Devices ${lights.size}") }
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

        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
          ) {
            Button(
              modifier = Modifier.weight(1f),
              onClick = refreshLights
            ) { Text("REFRESH") }

            Button(
              modifier = Modifier.weight(1f),
              enabled = selectedLights.isNotEmpty(),
              onClick = regroupLights
            ) { Text("REGROUP") }
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
  val contentUsages: Map<String, Double>,
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
  ctx: KeyUiContext<HomeKey>,
  groupConfigRepository: GroupConfigRepository,
  lightRepository: LightRepository,
  programRepository: ProgramRepository,
  sceneRepository: SceneRepository,
  wappRepository: WappRepository,
  pref: DataStore<ApeLabsPrefs>
) = Model {
  val prefs by pref.data.collectAsState(ApeLabsPrefs())

  val selectedGroupConfigs by remember { groupConfigRepository.selectedGroupConfigs }
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

  val lights = appForegroundState
    .flatMapLatest {
      if (it == AppForegroundState.FOREGROUND) lightRepository.lights
      else infiniteEmptyFlow()
    }
    .bindResource()
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
    }.bindResource(),
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
      ctx.navigator.push(ListKey(items = GROUPS) { it.toString() })
        ?.let { group ->
          lightRepository.regroupLights(
            selectedLights.toList()
              .also { selectedLights = emptySet() }, group
          )
        }
    },
    contentUsages = contentUsageRepository.contentUsages.bind(emptyMap()),
    userColors = colorRepository.userColors.bindResource(),
    updateColor = action { color ->
      color
        .asProgram(colorPickerId)
        .let {
          programRepository.updateProgram(it)
          updateConfig { copy(program = it) }
        }
      contentUsageRepository.contentUsed(color.id)
    },
    openColor = action { color -> ctx.navigator.push(ColorKey(color)) },
    addColor = action {
      ctx.navigator.push(TextInputKey(label = "Name.."))
        ?.let { id ->
          ctx.navigator.push(ColorKey(colorRepository.createColor(id)))
          contentUsageRepository.contentUsed(id)
        }
    },
    deleteColor = action { color -> colorRepository.deleteColor(color.id) },
    saveColor = action {
      ctx.navigator.push(TextInputKey(label = "Name.."))
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
    userPrograms = programRepository.userPrograms.bindResource(),
    updateProgram = action { program ->
      updateConfig { copy(program = program) }
      contentUsageRepository.contentUsed(program.id)
    },
    openProgram = action { program -> ctx.navigator.push(ProgramKey(program.id)) },
    addProgram = action {
      ctx.navigator.push(TextInputKey(label = "Name.."))
        ?.let { id ->
          ctx.navigator.push(ProgramKey(programRepository.createProgram(id).id))
          contentUsageRepository.contentUsed(id)
        }
    },
    deleteProgram = action { program -> programRepository.deleteProgram(program.id) },
    scenes = sceneRepository.userScenes.bindResource(),
    applyScene = action { scene ->
      groupConfigRepository.updateGroupConfigs(
        scene.groupConfigs
          .filterValues { it != null }
          .map { it.value!!.copy(id = it.key.toString()) },
        false
      )
      contentUsageRepository.contentUsed(scene.id)
    },
    openScene = action { scene -> ctx.navigator.push(SceneKey(scene.id)) },
    addScene = action {
      ctx.navigator.push(TextInputKey(label = "Name.."))
        ?.let { ctx.navigator.push(SceneKey(sceneRepository.createScene(it).id)) }
    },
    deleteScene = action { scene -> sceneRepository.deleteScene(scene.id) },
    saveScene = action {
      ctx.navigator.push(TextInputKey(label = "Name.."))
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
    openBackupRestore = action { ctx.navigator.push(BackupAndRestoreKey()) }
  )
}
