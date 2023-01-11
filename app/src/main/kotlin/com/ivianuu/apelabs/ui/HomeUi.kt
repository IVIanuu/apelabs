/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ivianuu.apelabs.data.ApeLabsPrefs
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.GroupConfig
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.LightColor
import com.ivianuu.apelabs.data.ProgramConfig
import com.ivianuu.apelabs.data.merge
import com.ivianuu.apelabs.data.toColor
import com.ivianuu.apelabs.domain.LightRepository
import com.ivianuu.essentials.coroutines.parForEach
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.getOrElse
import com.ivianuu.essentials.safeAs
import com.ivianuu.essentials.state.action
import com.ivianuu.essentials.state.bind
import com.ivianuu.essentials.state.bindResource
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.dialog.ListKey
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.material.TopAppBar
import com.ivianuu.essentials.ui.material.guessingContentColorFor
import com.ivianuu.essentials.ui.material.incrementingStepPolicy
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.ModelKeyUi
import com.ivianuu.essentials.ui.navigation.Navigator
import com.ivianuu.essentials.ui.navigation.RootKey
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.prefs.ColorListItem
import com.ivianuu.essentials.ui.prefs.ScaledPercentageUnitText
import com.ivianuu.essentials.ui.prefs.SliderListItem
import com.ivianuu.essentials.ui.prefs.SwitchListItem
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

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
          Text("Select a group to edit")
        }
      } else {
        item {
          ColorListItem(
            value = groupConfig.program.safeAs<ProgramConfig.SingleColor>()
              ?.color
              ?.toColor()
              ?: Color.Black,
            onValueChangeRequest = updateProgramColor,
            title = { Text("Color") }
          )
        }

        item {
          SwitchListItem(
            value = rainbowProgram,
            onValueChange = updateRainbowProgram,
            title = { Text("Rainbow program") }
          )
        }

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
      }

      val lights = lights.getOrElse { emptyList() }

      if (lights.isNotEmpty()) {
        item {
          Subheader { Text("Lights") }
        }

        item {
          FlowRow(
            modifier = Modifier
              .padding(16.dp),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
            crossAxisAlignment = FlowCrossAxisAlignment.Center
          ) {
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
                    Text("Light ${light.id}")
                  }
                }
              }
          }
        }

        if (selectedLights.isNotEmpty()) {
          item {
            Button(onClick = regroupLights) { Text("Regroup") }
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
  val rainbowProgram: Boolean,
  val updateRainbowProgram: (Boolean) -> Unit,
  val updateProgramColor: () -> Unit,
  val updateBrightness: (Float) -> Unit,
  val updateSpeed: (Float) -> Unit,
  val updateMusicMode: (Boolean) -> Unit,
  val lights: Resource<List<Light>>,
  val selectedLights: Set<String>,
  val toggleLightSelection: (Light) -> Unit,
  val flashLight: suspend (Light) -> Unit,
  val regroupLights: () -> Unit,
)

@Provide fun homeModel(
  lightRepository: LightRepository,
  navigator: Navigator,
  pref: DataStore<ApeLabsPrefs>
) = Model {
  val prefs = pref.data.bind(ApeLabsPrefs())

  val groupConfig = prefs.selectedGroups
    .map { prefs.groupConfigs[it] ?: GroupConfig() }
    .merge()

  suspend fun updateConfig(block: GroupConfig.() -> GroupConfig) {
    pref.updateData {
      copy(
        groupConfigs = buildMap {
          putAll(groupConfigs)
          selectedGroups.forEach {
            put(it, block(this[it] ?: GroupConfig()))
          }
        }
      )
    }
  }

  val lights = lightRepository.lights.bindResource()
  var selectedLights by remember { mutableStateOf(emptySet<String>()) }

  LaunchedEffect(selectedLights) {
    selectedLights.parForEach { lightId ->
      while (coroutineContext.isActive) {
        lightRepository.flashLight(lightId)
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
    rainbowProgram = groupConfig.program is ProgramConfig.Rainbow,
    updateRainbowProgram = action { value ->
      if (value) updateConfig { copy(program = ProgramConfig.Rainbow) }
      else updateConfig { copy(program = ProgramConfig.SingleColor(LightColor())) }
    },
    updateProgramColor = action {
      navigator.push(
        ColorKey(
          groupConfig.program.safeAs<ProgramConfig.SingleColor>()?.color ?: LightColor()
        )
      )
        ?.let { updateConfig { copy(program = ProgramConfig.SingleColor(it)) } }
    },
    updateBrightness = action { value ->
      updateConfig { copy(brightness = value) }
    },
    updateSpeed = action { value ->
      updateConfig { copy(speed = value) }
    },
    updateMusicMode = action { value ->
      updateConfig { copy(musicMode = value) }
    },
    lights = lights,
    selectedLights = selectedLights,
    toggleLightSelection = action { light ->
      selectedLights = selectedLights.toMutableSet().apply {
        if (light.id in this) remove(light.id)
        else add(light.id)
      }
    },
    regroupLights = action {
      navigator.push(
        ListKey(
          items = GROUPS
            .map { ListKey.Item(it, it.toString()) }
        )
      )?.let { group ->
        selectedLights.parForEach {
          lightRepository.regroupLight(it, group)
        }

        selectedLights = emptySet()
      }
    },
    flashLight = { light ->
      while (coroutineContext.isActive) {
        lightRepository.flashLight(light.id)
      }
    }
  )
}
