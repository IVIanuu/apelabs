/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalMaterialApi::class)

package com.ivianuu.apelabs.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import com.ivianuu.apelabs.data.Wapp
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
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.Resources
import com.ivianuu.essentials.ScopeManager
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.app.AppVisibleScope
import com.ivianuu.essentials.backup.BackupAndRestoreScreen
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.flowInScope
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.collectAsResourceState
import com.ivianuu.essentials.resource.getOrElse
import com.ivianuu.essentials.resource.getOrNull
import com.ivianuu.essentials.resource.map
import com.ivianuu.essentials.ui.animation.AnimatedContent
import com.ivianuu.essentials.ui.animation.crossFade
import com.ivianuu.essentials.ui.dialog.TextInputScreen
import com.ivianuu.essentials.ui.insets.LocalInsets
import com.ivianuu.essentials.ui.insets.localVerticalInsetsPadding
import com.ivianuu.essentials.ui.material.AppBar
import com.ivianuu.essentials.ui.material.ListItem
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.material.guessingContentColorFor
import com.ivianuu.essentials.ui.material.incrementingStepPolicy
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.Navigator
import com.ivianuu.essentials.ui.navigation.RootScreen
import com.ivianuu.essentials.ui.navigation.Ui
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.popup.PopupMenuButton
import com.ivianuu.essentials.ui.popup.PopupMenuItem
import com.ivianuu.essentials.ui.prefs.SingleChoiceToggleButtonGroupListItem
import com.ivianuu.essentials.ui.prefs.SliderListItem
import com.ivianuu.essentials.ui.prefs.SwitchListItem
import com.ivianuu.essentials.ui.resource.ResourceBox
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import kotlin.random.Random

@Provide @Scoped<AppScope> class HomeScreen : RootScreen, NavigationBarScreen {
  override val title: String
    get() = "Home"
  override val icon: @Composable () -> Unit
    get() = { Icon(Icons.Default.Home) }
  override val index: Int
    get() = 0

}

@Provide fun homeUi(
  navigationBarEvents: Flow<NavigationBarEvent>,
  @Inject resources: Resources
) = Ui<HomeScreen, HomeModel> { model ->
  Scaffold(
    topBar = {
      AppBar(
        title = { Text("Home") },
        leading = null,
        actions = {
          IconButton(onClick = {}) {
            CircularProgressIndicator(
              color = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
              modifier = Modifier
                .alpha(
                  animateFloatAsState(
                    if (model.wapps.map { it.isNotEmpty() }.getOrElse { false } &&
                      !model.wappState.map { it.isConnected }.getOrElse { false }) 1f
                    else 0f
                  ).value
                )
            )

            Icon(
              painterResId = R.drawable.ic_bluetooth,
              tint = animateColorAsState(
                when {
                  model.wappState.map { it.isConnected }.getOrElse { false } -> Color(0xFF0082FC)
                  else -> LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
                }
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
    },
    bottomBar = {
      Card(
        modifier = Modifier.clickable {},
        shape = RectangleShape,
        elevation = 6.dp
      ) {
        Column {
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .padding(16.dp),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
            crossAxisAlignment = FlowCrossAxisAlignment.Center
          ) {
            LongClickChip(
              selected = model.groups.all { it in model.selectedGroups },
              enabled = true,
              onClick = model.toggleAllGroupSelections,
              onLongClick = null
            ) {
              Text("ALL")
            }

            model.groups.forEach { group ->
              LongClickChip(
                selected = group in model.selectedGroups,
                enabled = model.lights.getOrNull()?.any { it.group == group } == true,
                onClick = { model.toggleGroupSelection(group, false) },
                onLongClick = { model.toggleGroupSelection(group, true) }
              ) {
                Text(group.toString())
              }
            }

            IconButton(onClick = model.shuffleGroups) { Icon(R.drawable.ic_shuffle) }
          }

          Spacer(Modifier.height(LocalInsets.current.bottom))
        }
      }
    }
  ) {
    ResourceBox(model.userContent) { userContent ->
      val lazyGridState = rememberLazyGridState()

      LaunchedEffect(true) {
        navigationBarEvents.collect {
          if (it is NavigationBarEvent.Reselected && it.index == 0)
            lazyGridState.animateScrollToItem(0)
        }
      }

      LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(2),
        contentPadding = localVerticalInsetsPadding()
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
            AnimatedContent(
              model.selectedGroups,
              transitionSpec = { crossFade() }
            ) {
              Column {
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

                val controller = remember { ColorPickerState() }

                Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                  ImageColorPicker(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(200.dp),
                    controller = controller
                  )
                }

                LaunchedEffect(controller.selectedColor) {
                  controller.selectedColor?.let {
                    model.updateColorPickerColor(it)
                  }
                }

                LaunchedEffect(model.groupConfig.program) {
                  if (model.groupConfig.program.id != Program.colorPickerId(model.selectedGroups.toList()))
                    controller.clear()
                }

                SliderListItem(
                  value = model.groupConfig.brightness,
                  onValueChange = model.updateBrightness,
                  stepPolicy = incrementingStepPolicy(0.05f),
                  title = { Text("Brightness") },
                  valueText = { Text("${(it * 100f).roundToInt()}") }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                  SliderListItem(
                    modifier = Modifier.weight(1f),
                    value = model.groupConfig.speed,
                    onValueChange = model.updateSpeed,
                    stepPolicy = incrementingStepPolicy(0.05f),
                    title = { Text("Speed") },
                    valueText = { Text("${(it * 100f).roundToInt()}") }
                  )

                  IconButton(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = model.shuffleSpeed
                  ) { Icon(R.drawable.ic_shuffle) }
                }

                SingleChoiceToggleButtonGroupListItem(
                  selected = model.groupConfig.mode,
                  values = GroupConfig.Mode.entries,
                  onSelectionChanged = model.updateMode,
                  title = { Text("Mode") }
                )

                SwitchListItem(
                  value = model.groupConfig.blackout,
                  onValueChange = model.updateBlackout,
                  title = { Text("Blackout") }
                )
              }
            }
          }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
          Subheader { Text("Content") }
        }

        fun Any.isCustom() = this in userContent.colors || this in userContent.programs ||
            this in userContent.scenes

        buildList {
          addAll(userContent.colors.map { it.id to it })
          addAll(userContent.programs.map { it.id to it })
          add(Program.RAINBOW.id to Program.RAINBOW)
          addAll(model.builtInColors.map { it.id to it })
          addAll(userContent.scenes.map { it.id to it })
        }
          .sortedBy { it.first.lowercase() }
          .sortedByDescending { model.contentUsages[it.first] ?: -1f }
          .chunked(2)
          .forEach { row ->
            row.forEachIndexed { index, (id, item) ->
              item(key = item, span = {
                GridItemSpan(if (row.size == 1) maxLineSpan else 1)
              }) {
                fun <T> Any.fold(
                  color: (ApeColor) -> T,
                  program: (Program) -> T,
                  scene: (Scene) -> T
                ) = when (this) {
                  is ApeColor -> color(this)
                  is Program -> program(this)
                  is Scene -> scene(this)
                  else -> throw AssertionError()
                }

                ListItem(
                  modifier = Modifier
                    .animateItemPlacement()
                    .clickable {
                      item.fold(
                        color = { model.updateColor(it) },
                        program = { model.updateProgram(it) },
                        scene = { model.applyScene(it) }
                      )
                    },
                  title = { Text(if (item === Program.RAINBOW) "Rainbow" else id) },
                  leading = {
                    ColorListIcon(
                      modifier = Modifier.size(40.dp),
                      colors = item.fold(
                        color = { listOf(it) },
                        program = { it.items.map { it.color } },
                        scene = { emptyList() }
                      )
                    )
                  },
                  trailing = if (!item.isCustom()) null else ({
                    PopupMenuButton {
                      PopupMenuItem(onSelected = {
                        item.fold(
                          color = { model.openColor(it) },
                          program = { model.openProgram(it) },
                          scene = { model.openScene(it) }
                        )
                      }) { Text("Open") }
                      PopupMenuItem(onSelected = {
                        item.fold(
                          color = { model.deleteColor(it) },
                          program = { model.deleteProgram(it) },
                          scene = { model.deleteScene(it) }
                        )
                      }) { Text("Delete") }
                    }
                  }),
                  textPadding = PaddingValues(start = 16.dp),
                  leadingPadding = PaddingValues(start = if (index == 0 || row.size == 1) 16.dp else 8.dp),
                  trailingPadding = PaddingValues(end = if (index == 1 || row.size == 1) 16.dp else 8.dp)
                )
              }
            }
          }

        item(span = { GridItemSpan(maxLineSpan) }) {
          FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            mainAxisSpacing = 8.dp
          ) {
            Button(onClick = model.addColor) { Text("ADD COLOR") }
            Button(onClick = model.addProgram) { Text("ADD PROGRAM") }
            Button(onClick = model.addScene) { Text("ADD SCENE") }
          }
        }
      }
    }
  }
}

@Composable fun LongClickChip(
  selected: Boolean,
  enabled: Boolean,
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
      .defaultMinSize(minWidth = 56.dp)
      .alpha(animateFloatAsState(if (enabled) 1f else ContentAlpha.disabled).value),
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
  val shuffleGroups: () -> Unit,
  val updateBrightness: (Float) -> Unit,
  val updateSpeed: (Float) -> Unit,
  val shuffleSpeed: () -> Unit,
  val updateMode: (GroupConfig.Mode) -> Unit,
  val updateBlackout: (Boolean) -> Unit,
  val wapps: Resource<List<Wapp>>,
  val wappState: Resource<WappState>,
  val lights: Resource<List<Light>>,
  val userContent: Resource<UserContent>,
  val updateColor: (ApeColor) -> Unit,
  val openColor: (ApeColor) -> Unit,
  val addColor: () -> Unit,
  val deleteColor: (ApeColor) -> Unit,
  val saveColor: () -> Unit,
  val updateColorPickerColor: (Color) -> Unit,
  val contentUsages: Map<String, Float>,
  val builtInColors: List<ApeColor> = BuiltInColors,
  val updateProgram: (Program) -> Unit,
  val openProgram: (Program) -> Unit,
  val addProgram: () -> Unit,
  val deleteProgram: (Program) -> Unit,
  val applyScene: (Scene) -> Unit,
  val openScene: (Scene) -> Unit,
  val addScene: () -> Unit,
  val deleteScene: (Scene) -> Unit,
  val saveScene: () -> Unit,
  val openBackupRestore: () -> Unit
)

data class UserContent(
  val colors: List<ApeColor>,
  val programs: List<Program>,
  val scenes: List<Scene>
)

@Provide fun homeModel(
  colorRepository: ColorRepository,
  contentUsageRepository: ContentUsageRepository,
  groupConfigRepository: GroupConfigRepository,
  lightRepository: LightRepository,
  navigator: Navigator,
  programRepository: ProgramRepository,
  sceneRepository: SceneRepository,
  scopeManager: ScopeManager,
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
    shuffleGroups = action {
      val shuffledGroupConfigs = selectedGroupConfigs.shuffled()
      groupConfigRepository.updateGroupConfigs(
        selectedGroupConfigs.mapIndexed { index, config ->
          shuffledGroupConfigs[index].copy(id = config.id)
        },
        false
      )
    },
    updateBrightness = action { value ->
      updateConfig { copy(brightness = value) }
    },
    updateSpeed = action { value ->
      updateConfig { copy(speed = value) }
    },
    shuffleSpeed = action {
      selectedGroupConfigs.forEach {
        groupConfigRepository.updateGroupConfig(
          it.copy(
            speed = Random(System.currentTimeMillis())
              .nextInt(0, 100) / 100f
          ),
          false
        )
      }
    },
    updateMode = action { value ->
      updateConfig { copy(mode = value) }
    },
    updateBlackout = action { value ->
      updateConfig { copy(blackout = value) }
    },
    wappState = remember {
      scopeManager.flowInScope<AppVisibleScope, _>(wappRepository.wappState)
    }.collectAsResourceState().value,
    wapps = wappRepository.wapps.collectAsResourceState().value,
    lights = lightRepository.lights.collectAsResourceState().value,
    contentUsages = contentUsageRepository.contentUsages.collectAsState(emptyMap()).value,
    userContent = remember {
      combine(
        colorRepository.userColors,
        programRepository.userPrograms,
        sceneRepository.userScenes
      ) { colors, programs, scenes -> UserContent(colors, programs, scenes) }
    }.collectAsResourceState().value,
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
