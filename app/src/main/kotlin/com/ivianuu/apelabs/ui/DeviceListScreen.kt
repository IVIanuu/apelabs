package com.ivianuu.apelabs.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.apelabs.domain.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.ui.common.*
import com.ivianuu.essentials.ui.dialog.*
import com.ivianuu.essentials.ui.material.*
import com.ivianuu.essentials.ui.navigation.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*

@Provide class DeviceListScreen(private val lightRepository: LightRepository) : NavigationBarScreen {
  override val title: String
    get() = "Devices"

  override val icon: @Composable () -> Unit = {
    val deviceCount = lightRepository.lights.map { it.size }.state(0)

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
        Icons.Default.SettingsRemote,
        modifier = Modifier.align(Alignment.Center),
        contentDescription = null
      )
    }
  }

  override val index: Int
    get() = 1
}

@Provide fun deviceListUi(
  lightRepository: LightRepository,
  navigator: Navigator,
  wappRepository: WappRepository
) = Ui<DeviceListScreen> {
  val wappState = wappRepository.wappState.scopedState(WappState())
  val lights = lightRepository.lights.scopedState(emptyList())

  var selectedLights by rememberScoped { mutableStateOf(emptySet<Int>()) }

  LaunchedScopedEffect(selectedLights) {
    lightRepository.flashLights(selectedLights.toList())
  }

  ScreenScaffold(
    topBar = {
      AppBar(
        leading = null,
        actions = {
          IconButton(onClick = scopedAction { lightRepository.refreshLights() }) {
            Icon(Icons.Default.Refresh, null)
          }
        }
      ) {
        Text("Devices")
      }
    }
  ) {
    VerticalList {
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
                      if (light.id in selectedLights) LocalContentColor.current.copy(alpha = 0.12f)
                      else MaterialTheme.colors.surface
                    ).value
                  )
                  .clickable {
                    selectedLights = selectedLights.toMutableSet().apply {
                      if (light.id in this) remove(light.id)
                      else add(light.id)
                    }
                  },
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
            enabled = selectedLights.isNotEmpty(),
            onClick = scopedAction {
              navigator.push(ListScreen(items = GROUPS) { it.toString() })
                ?.let { group ->
                  lightRepository.regroupLights(
                    selectedLights.toList()
                      .also { selectedLights = emptySet() }, group
                  )
                }
            }
          ) { Text("REGROUP") }
        }
      }
    }
  }
}
