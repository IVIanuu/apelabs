/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalFoundationApi::class)

package com.ivianuu.apelabs.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import com.google.accompanist.flowlayout.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.apelabs.domain.*
import com.ivianuu.essentials.*
import com.ivianuu.essentials.backup.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.data.*
import com.ivianuu.essentials.resource.*
import com.ivianuu.essentials.ui.animation.*
import com.ivianuu.essentials.ui.animation.AnimatedContent
import com.ivianuu.essentials.ui.common.*
import com.ivianuu.essentials.ui.dialog.*
import com.ivianuu.essentials.ui.insets.*
import com.ivianuu.essentials.ui.material.*
import com.ivianuu.essentials.ui.navigation.*
import com.ivianuu.essentials.ui.prefs.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*
import kotlin.math.*
import kotlin.random.*

@Provide @Scoped<AppScope> class HomeScreen : RootScreen, NavigationBarScreen {
  override val title: String
    get() = "Home"
  override val icon: @Composable () -> Unit
    get() = { Icon(Icons.Default.Home, null) }
  override val index: Int
    get() = 0
}

@Provide fun homeUi(
  @Provide appContext: AppContext,
  colorRepository: ColorRepository,
  contentUsageRepository: ContentUsageRepository,
  groupConfigRepository: GroupConfigRepository,
  lightRepository: LightRepository,
  navigationBarEvents: Flow<NavigationBarEvent>,
  navigator: Navigator,
  programRepository: ProgramRepository,
  sceneRepository: SceneRepository,
  wappRepository: WappRepository,
  pref: DataStore<ApeLabsPrefs>
) = Ui<HomeScreen> {
  val prefs = pref.data.scopedState(ApeLabsPrefs(selectedGroups = GROUPS.toSet()))

  val selectedGroupConfigs = groupConfigRepository.selectedGroupConfigs
    .scopedState(emptyList())

  val groupConfig = selectedGroupConfigs.merge()
  suspend fun updateConfig(block: GroupConfig.() -> GroupConfig) {
    groupConfigRepository.updateGroupConfigs(
      selectedGroupConfigs
        .map { it.block() },
      false
    )
  }

  val colorPickerId = Program.colorPickerId(prefs.selectedGroups.toList())

  val colorPickerColor = programRepository.program(colorPickerId)
    .scopedState(null)
    ?.items
    ?.singleOrNull()
    ?.color
    ?: ApeColor(id = colorPickerId, white = 1f)

  val toggleGroupSelection = action { group: Int, longClick: Boolean ->
    pref.updateData {
      copy(
        selectedGroups = if (!longClick) setOf(group)
        else selectedGroups.toMutableSet().apply {
          if (group in this) remove(group)
          else add(group)
        }
      )
    }
  }
  val toggleAllGroupSelections = action {
    pref.updateData {
      copy(
        selectedGroups = if (GROUPS.all { it in selectedGroups }) emptySet()
        else GROUPS.toSet()
      )
    }
  }
  val shuffleGroups = action {
    val shuffledGroupConfigs = selectedGroupConfigs.shuffled()
    groupConfigRepository.updateGroupConfigs(
      selectedGroupConfigs.mapIndexed { index, config ->
        shuffledGroupConfigs[index].copy(id = config.id)
      },
      false
    )
  }
  val updateBrightness = action { value: Float ->
    updateConfig { copy(brightness = value) }
  }
  val updateSpeed = action { value: Float ->
    updateConfig { copy(speed = value) }
  }
  val shuffleSpeed = action {
    selectedGroupConfigs.forEach {
      groupConfigRepository.updateGroupConfig(
        it.copy(
          speed = Random(System.currentTimeMillis())
            .nextInt(0, 100) / 100f
        ),
        false
      )
    }
  }
  val updateMode = action { value: GroupConfig.Mode ->
    updateConfig { copy(mode = value) }
  }
  val updateBlackout = action { value: Boolean ->
    updateConfig { copy(blackout = value) }
  }
  val wappState = wappRepository.wappState.scopedResourceState()

  val wapps = wappRepository.wapps.scopedResourceState()
  val lights = lightRepository.lights.scopedResourceState()
  val contentUsages = contentUsageRepository.contentUsages.scopedState(emptyMap())
  val userContentResource = combine(
    colorRepository.userColors,
    programRepository.userPrograms,
    sceneRepository.userScenes
  ) { colors, programs, scenes -> UserContent(colors, programs, scenes) }
    .scopedResourceState()
  val updateColor = action { color: ApeColor ->
    color
      .asProgram(colorPickerId)
      .let {
        programRepository.updateProgram(it)
        updateConfig { copy(program = it) }
      }
    contentUsageRepository.contentUsed(color.id)
  }
  val openColor = action { color: ApeColor -> navigator.push(ColorScreen(color)) }
  val addColor = action {
    navigator.push(TextInputScreen(label = "Name.."))
      ?.let { id ->
        navigator.push(ColorScreen(colorRepository.createColor(id)))
        contentUsageRepository.contentUsed(id)
      }
  }
  val deleteColor = action { color: ApeColor -> colorRepository.deleteColor(color.id) }
  val saveColor = action {
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
  }
  val updateColorPickerColor = action { composeColor: Color ->
    composeColor
      .toApeColor(colorPickerId)
      .asProgram(colorPickerId)
      .let {
        programRepository.updateProgram(it)
        updateConfig { copy(program = it) }
      }
  }
  val updateProgram = action { program: Program ->
    updateConfig { copy(program = program) }
    contentUsageRepository.contentUsed(program.id)
  }
  val openProgram = action { program: Program -> navigator.push(ProgramScreen(program.id)) }
  val addProgram = action {
    navigator.push(TextInputScreen(label = "Name.."))
      ?.let { id ->
        navigator.push(ProgramScreen(programRepository.createProgram(id).id))
        contentUsageRepository.contentUsed(id)
      }
  }
  val deleteProgram = action { program: Program -> programRepository.deleteProgram(program.id) }
  val applyScene = action { scene: Scene ->
    groupConfigRepository.updateGroupConfigs(
      scene.groupConfigs
        .filterValues { it != null }
        .map { it.value!!.copy(id = it.key.toString()) },
      false
    )
    contentUsageRepository.contentUsed(scene.id)
  }
  val openScene = action { scene: Scene -> navigator.push(SceneScreen(scene.id)) }
  val addScene = action {
    navigator.push(TextInputScreen(label = "Name.."))
      ?.let { navigator.push(SceneScreen(sceneRepository.createScene(it).id)) }
  }
  val deleteScene = action { scene: Scene -> sceneRepository.deleteScene(scene.id) }
  val saveScene = action {
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
  }
  val openBackupRestore = action { navigator.push(BackupAndRestoreScreen()) }

  ScreenScaffold(
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
                    if (wapps.map { it.isNotEmpty() }.getOrElse { false } &&
                      !wappState.map { it.isConnected }.getOrElse { false }) 1f
                    else 0f
                  ).value
                )
            )

            Icon(
              Icons.Default.Bluetooth,
              tint = animateColorAsState(
                when {
                  wappState.map { it.isConnected }.getOrElse { false } -> Color(0xFF0082FC)
                  else -> LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
                }
              ).value,
              contentDescription = null
            )
          }

          DropdownMenuButton {
            DropdownMenuItem(onClick = saveColor) { Text("Save color") }
            DropdownMenuItem(onClick = saveScene) { Text("Save scene") }
            DropdownMenuItem(onClick = openBackupRestore) { Text("Backup and restore") }
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
              selected = GROUPS.all { it in prefs.selectedGroups },
              enabled = true,
              onClick = toggleAllGroupSelections,
              onLongClick = null
            ) {
              Text("ALL")
            }

            GROUPS.forEach { group ->
              LongClickChip(
                selected = group in prefs.selectedGroups,
                enabled = lights.getOrNull()?.any { it.group == group } == true,
                onClick = { toggleGroupSelection(group, false) },
                onLongClick = { toggleGroupSelection(group, true) }
              ) {
                Text(group.toString())
              }
            }

            IconButton(onClick = shuffleGroups) { Icon(Icons.Default.Shuffle, null) }
          }

          Spacer(Modifier.height(LocalInsets.current.bottom))
        }
      }
    }
  ) {
    ResourceBox(userContentResource) { userContent ->
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
        if (prefs.selectedGroups.isEmpty()) {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
              modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
              text = "Select a group to edit"
            )
          }
        } else {
          item(span = { GridItemSpan(maxLineSpan) }) {
            AnimatedContent(
              prefs.selectedGroups,
              transitionSpec = { crossFade() }
            ) {
              Column {
                val programName = when {
                  groupConfig.program.id == Program.RAINBOW.id -> "Rainbow"
                  groupConfig.program.id.isUUID -> groupConfig.program.items.singleOrNull()
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
                    updateColorPickerColor(it)
                  }
                }

                LaunchedEffect(groupConfig.program) {
                  if (groupConfig.program.id != Program.colorPickerId(prefs.selectedGroups.toList()))
                    controller.clear()
                }

                SliderListItem(
                  value = groupConfig.brightness,
                  onValueChange = updateBrightness,
                  stepPolicy = incrementingStepPolicy(0.05f),
                  title = { Text("Brightness") },
                  valueText = { Text("${(it * 100f).roundToInt()}") }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                  SliderListItem(
                    modifier = Modifier.weight(1f),
                    value = groupConfig.speed,
                    onValueChange = updateSpeed,
                    stepPolicy = incrementingStepPolicy(0.05f),
                    title = { Text("Speed") },
                    valueText = { Text("${(it * 100f).roundToInt()}") }
                  )

                  IconButton(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = shuffleSpeed
                  ) { Icon(Icons.Default.Shuffle, null) }
                }

                SingleChoiceToggleButtonGroupListItem(
                  selected = groupConfig.mode,
                  values = GroupConfig.Mode.entries,
                  onSelectionChanged = updateMode,
                  title = { Text("Mode") }
                )

                SwitchListItem(
                  value = groupConfig.blackout,
                  onValueChange = updateBlackout,
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
          addAll(BuiltInColors.map { it.id to it })
          addAll(userContent.scenes.map { it.id to it })
        }
          .sortedBy { it.first.lowercase() }
          .sortedByDescending { contentUsages[it.first] ?: -1f }
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
                        color = { updateColor(it) },
                        program = { updateProgram(it) },
                        scene = { applyScene(it) }
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
                    DropdownMenuButton {
                      DropdownMenuItem(onClick = {
                        item.fold(
                          color = { openColor(it) },
                          program = { openProgram(it) },
                          scene = { openScene(it) }
                        )
                      }) { Text("Open") }
                      DropdownMenuItem(onClick = {
                        item.fold(
                          color = { deleteColor(it) },
                          program = { deleteProgram(it) },
                          scene = { deleteScene(it) }
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
            Button(onClick = addColor) { Text("ADD COLOR") }
            Button(onClick = addProgram) { Text("ADD PROGRAM") }
            Button(onClick = addScene) { Text("ADD SCENE") }
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

data class UserContent(
  val colors: List<ApeColor>,
  val programs: List<Program>,
  val scenes: List<Scene>
)
