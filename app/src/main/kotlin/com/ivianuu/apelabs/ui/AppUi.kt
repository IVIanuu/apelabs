package com.ivianuu.apelabs.ui

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.compose.action
import com.ivianuu.essentials.coroutines.EventFlow
import com.ivianuu.essentials.ui.UiScope
import com.ivianuu.essentials.ui.animation.crossFade
import com.ivianuu.essentials.ui.app.AppUi
import com.ivianuu.essentials.ui.material.NavigationBar
import com.ivianuu.essentials.ui.material.NavigationBarItem
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.navigation.Navigator
import com.ivianuu.essentials.ui.navigation.NavigatorContent
import com.ivianuu.essentials.ui.navigation.Screen
import com.ivianuu.essentials.ui.navigation.ScreenConfig
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.MutableSharedFlow

@Provide fun appUi(
  activity: ComponentActivity,
  events: MutableSharedFlow<NavigationBarEvent>,
  navigationBarScreens: List<NavigationBarScreen>,
  @Inject navigator: Navigator
) = AppUi {
  val sortedNavigationBarScreens = navigationBarScreens.sortedBy { it.index }

  val currentNavigationBarScreen by rememberUpdatedState(
    navigator.backStack.collectAsState()
      .value
      .last { it is NavigationBarScreen }
  )

  Scaffold(
    bottomBar = {
      NavigationBar(backgroundColor = MaterialTheme.colors.primary) {
        sortedNavigationBarScreens.forEach { navigationBarScreen ->
          NavigationBarItem(
            selected = navigationBarScreen::class == currentNavigationBarScreen::class,
            onClick = action {
              if (currentNavigationBarScreen::class == navigationBarScreen::class &&
                navigator.backStack.value.last()::class == navigationBarScreen::class)
                events.emit(NavigationBarEvent.Reselected(navigationBarScreen.index))
              else
                navigator.setBackStack(
                  navigator.backStack.value.toMutableList().apply {
                    removeAll { it::class == navigationBarScreen::class }
                    add(navigationBarScreen)
                  }
                )
            },
            icon = navigationBarScreen.icon,
            label = { Text(navigationBarScreen.title) }
          )
        }
      }
    }
  ) {
    NavigatorContent(navigator = navigator)
  }
}

sealed interface NavigationBarScreen : Screen<Unit> {
  val title: String
  val icon: @Composable () -> Unit
  val index: Int

  @Provide companion object {
    @Provide fun <T : NavigationBarScreen> config() = ScreenConfig<T> {
      crossFade()
      containerSizeTransform(null)
    }
  }
}

@Provide val navigationBarEvents: @Scoped<UiScope> MutableSharedFlow<NavigationBarEvent>
  get() = EventFlow()

sealed interface NavigationBarEvent {
  val index: Int

  data class Reselected(override val index: Int) : NavigationBarEvent
}
