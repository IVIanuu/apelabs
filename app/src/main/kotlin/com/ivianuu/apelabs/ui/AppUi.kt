package com.ivianuu.apelabs.ui

import androidx.activity.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import com.ivianuu.essentials.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.coroutines.*
import com.ivianuu.essentials.ui.*
import com.ivianuu.essentials.ui.animation.*
import com.ivianuu.essentials.ui.app.*
import com.ivianuu.essentials.ui.material.*
import com.ivianuu.essentials.ui.navigation.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*

@Provide fun appUi(
  activity: ComponentActivity,
  events: MutableSharedFlow<NavigationBarEvent>,
  navigationBarScreens: List<NavigationBarScreen>,
  @Provide navigator: Navigator
) = AppUi {
  val sortedNavigationBarScreens = navigationBarScreens.sortedBy { it.index }

  val currentNavigationBarScreen = navigator.backStack
    .last { it is NavigationBarScreen }

  ScreenScaffold(
    bottomBar = {
      NavigationBar(backgroundColor = MaterialTheme.colors.primary) {
        sortedNavigationBarScreens.forEach { navigationBarScreen ->
          NavigationBarItem(
            selected = navigationBarScreen::class == currentNavigationBarScreen::class,
            onClick = action {
              if (currentNavigationBarScreen::class == navigationBarScreen::class &&
                navigator.backStack.last()::class == navigationBarScreen::class)
                events.emit(NavigationBarEvent.Reselected(navigationBarScreen.index))
              else
                navigator.setBackStack(
                  navigator.backStack.toMutableList().apply {
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
