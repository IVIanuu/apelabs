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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.ivianuu.apelabs.color.ApeColor
import com.ivianuu.apelabs.color.BuiltInColors
import com.ivianuu.apelabs.color.ColorKey
import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.group.GROUPS
import com.ivianuu.apelabs.group.GroupConfig
import com.ivianuu.apelabs.device.Light
import com.ivianuu.apelabs.device.WappState
import com.ivianuu.apelabs.group.merge
import com.ivianuu.apelabs.device.LightRepository
import com.ivianuu.apelabs.program.ProgramRepository
import com.ivianuu.apelabs.device.WappRepository
import com.ivianuu.apelabs.program.Program
import com.ivianuu.apelabs.color.ColorListIcon
import com.ivianuu.apelabs.color.ColorRepository
import com.ivianuu.apelabs.color.toApeColor
import com.ivianuu.apelabs.group.GroupConfigRepository
import com.ivianuu.apelabs.program.ProgramKey
import com.ivianuu.apelabs.scene.SceneRepository
import com.ivianuu.apelabs.util.randomId
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.compose.bind
import com.ivianuu.essentials.compose.bindResource
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.db.Db
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
import com.ivianuu.essentials.ui.prefs.ScaledPercentageUnitText
import com.ivianuu.essentials.ui.prefs.SliderListItem
import com.ivianuu.essentials.ui.prefs.SwitchListItem
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

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
            valueText = { ScaledPercentageUnitText(it) }
          )
        }

        item {
          SliderListItem(
            value = groupConfig.speed,
            onValueChange = updateSpeed,
            stepPolicy = incrementingStepPolicy(0.05f),
            title = { Text("Speed") },
            valueText = { ScaledPercentageUnitText(it) }
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
          modifier = Modifier.clickable { updateProgram(Program.COLOR_PICKER) },
          title = { Text(Program.COLOR_PICKER_ID) },
          leading = {
            ColorListIcon(
              modifier = Modifier.size(40.dp),
              colors = remember { listOf(BuiltInColors.shuffled().first()) }
            )
          }
        )
      }

      val programs = programs.getOrElse { emptyList() }
      programs
        .sortedBy { it.id }
        .forEach { program ->
          item {
            ListItem(
              modifier = Modifier.clickable { updateProgram(program) },
              title = { Text(program.id) },
              leading = {
                ColorListIcon(
                  modifier = Modifier.size(40.dp),
                  colors = program.items.map { it.color }
                )
              },
              trailing = {
                PopupMenuButton {
                  PopupMenuItem(onSelected = { openProgram(program) }) { Text("Open") }
                  PopupMenuItem(onSelected = { deleteProgram(program) }) { Text("Delete") }
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
              colors = listOf(
                Color.Red.toApeColor("R"),
                Color.Yellow.toApeColor("Y"),
                Color.Green.toApeColor("G"),
                Color.Blue.toApeColor("B")
              )
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
  val programs: Resource<List<Program>>,
  val updateProgram: (Program) -> Unit,
  val openProgram: (Program) -> Unit,
  val addProgram: () -> Unit,
  val deleteProgram: (Program) -> Unit
)

context(ApeLabsPrefsContext, ColorRepository, Db, GroupConfigRepository, LightRepository,
Logger, KeyUiContext<HomeKey>, ProgramRepository, SceneRepository, WappRepository)
    @Provide fun homeModel() = Model {
  val prefs = pref.data.bind(ApeLabsPrefs())

  val groupConfig = combine(
    prefs.selectedGroups
      .map { group ->
        groupConfig(group.toString())
          .map { it ?: GroupConfig(group.toString()) }
      }
  ) { it.toList().merge("Merged") }
    .bind(GroupConfig("Merged"), prefs.selectedGroups)

  suspend fun updateConfig(block: GroupConfig.() -> GroupConfig) {
    transaction {
      prefs.selectedGroups
        .map { (groupConfig(it.toString()).first() ?: GroupConfig(it.toString())).block() }
        .forEach { updateGroupConfig(it) }
    }
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
    updateProgram = action { program ->
      if (program === Program.COLOR_PICKER) {
        navigator.push(ColorKey(color(Program.COLOR_PICKER_ID).first() ?: ApeColor.BLACK))
          ?.let {
            updateColor(it.copy(id = Program.COLOR_PICKER_ID))
            updateConfig { copy(program = program) }
          }
      } else {
        updateConfig { copy(program = program) }
      }
    },
    openProgram = action { program -> navigator.push(ProgramKey(program.id)) },
    addProgram = action {
      navigator.push(TextInputKey(label = "Name.."))
        ?.let {
          createProgram(it)
          navigator.push(ProgramKey(it))
        }
    },
    deleteProgram = action { program -> deleteProgram(program.id) }
  )
}
