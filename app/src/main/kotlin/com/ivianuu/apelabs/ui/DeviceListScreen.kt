package com.ivianuu.apelabs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivianuu.apelabs.R
import com.ivianuu.apelabs.data.GROUPS
import com.ivianuu.apelabs.data.Light
import com.ivianuu.apelabs.data.Wapp
import com.ivianuu.apelabs.data.WappState
import com.ivianuu.apelabs.domain.LightRepository
import com.ivianuu.apelabs.domain.WappRepository
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.resource.Resource
import com.ivianuu.essentials.resource.collectAsResourceState
import com.ivianuu.essentials.resource.getOrElse
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.dialog.ListScreen
import com.ivianuu.essentials.ui.material.AppBar
import com.ivianuu.essentials.ui.material.ListItem
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.Navigator
import com.ivianuu.essentials.ui.navigation.Ui
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Provide class DeviceListScreen(private val lightRepository: LightRepository) : NavigationBarScreen {
  override val title: String
    get() = "Devices"

  override val icon: @Composable () -> Unit
    get() = {
      val deviceCount by remember {
        lightRepository.lights
          .map { it.size }
      }.collectAsState(0)

      BadgedBox(
        badge = {
          AnimatedVisibility(visible = deviceCount > 0) {
            Badge(backgroundColor = MaterialTheme.colors.secondary) {
              Text(deviceCount.toString())
            }
          }
        }
      ) {
        Icon(
          R.drawable.ic_settings_remote,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }

  override val index: Int
    get() = 1
}

@Provide fun deviceListUi(
  navigationBarEvents: Flow<NavigationBarEvent>
) = Ui<DeviceListScreen, DeviceListModel> { model ->
  Scaffold(
    topBar = {
      AppBar(
        leading = null,
        actions = {
          IconButton(onClick = model.refreshLights) {
            Icon(Icons.Default.Refresh)
          }
        }
      ) {
        Text("Devices")
      }
    }
  ) {
    VerticalList {
      val wappState = model.wappState.getOrElse { WappState() }
      val lights = model.lights.getOrElse { emptyList() }

      item {
        ListItem(
          title = { Text("Wapp: ${wappState.id}") },
          subtitle = {
            Text(
              if (wappState.battery != null) {
                if (wappState.battery < 0f) {
                  "Charging"
                } else {
                  "Battery: ${(wappState.battery * 100).toInt()}%"
                }
              } else "Unknown"
            )
          }
        )
      }

      lights
        .groupBy { it.group }
        .mapValues { it.value.sortedBy { it.id } }
        .toList()
        .sortedBy { it.first }
        .forEach { (group, groupLights) ->
          item {
            Subheader { Text("Group $group") }
          }

          groupLights.forEach { light ->
            item {
              ListItem(
                modifier = Modifier
                  .background(
                    animateColorAsState(
                      if (light.id in model.selectedLights) LocalContentColor.current.copy(alpha = 0.12f)
                      else MaterialTheme.colors.surface
                    ).value
                  )
                  .clickable { model.toggleLightSelection(light) },
                title = {
                  Text(
                    "${
                      light.type?.name?.lowercase()?.capitalize() ?: "Light"
                    } ${light.id}"
                  )
                },
                subtitle = {
                  Text(
                    if (light.battery != null) {
                      if (light.battery < 0f) {
                        "Charging"
                      } else {
                        "Battery: ${(light.battery * 100).toInt()}%"
                      }
                    } else "Unknown"
                  )
                }
              )
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
            enabled = model.selectedLights.isNotEmpty(),
            onClick = model.regroupLights
          ) { Text("REGROUP") }
        }
      }
    }
  }
}

data class DeviceListModel(
  val wapps: Resource<List<Wapp>>,
  val wappState: Resource<WappState>,
  val lights: Resource<List<Light>>,
  val refreshLights: () -> Unit,
  val selectedLights: Set<Int>,
  val toggleLightSelection: (Light) -> Unit,
  val regroupLights: () -> Unit,
)

@Provide fun deviceListModel(
  lightRepository: LightRepository,
  navigator: Navigator,
  wappRepository: WappRepository
) = Model {
  val lights by lightRepository.lights.collectAsResourceState()
  var selectedLights by remember { mutableStateOf(emptySet<Int>()) }

  LaunchedEffect(selectedLights) {
    lightRepository.flashLights(selectedLights.toList())
  }

  DeviceListModel(
    wappState = wappRepository.wappState.collectAsResourceState().value,
    wapps = wappRepository.wapps.collectAsResourceState().value,
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
  )
}
