package com.ivianuu.apelabs.domain

import androidx.compose.runtime.*
import com.ivianuu.apelabs.data.*
import com.ivianuu.essentials.*
import com.ivianuu.essentials.compose.*
import com.ivianuu.essentials.data.*
import com.ivianuu.essentials.ui.*
import com.ivianuu.injekt.*
import kotlinx.coroutines.flow.*

@Provide @Scoped<UiScope> class PreviewRepository(private val pref: DataStore<ApeLabsPrefs>) {
  private val previewProviders = mutableStateListOf<@Composable (List<Int>) -> List<GroupConfig>>()

  var previewsEnabled by mutableStateOf(false)

  @Composable fun previewGroupConfigs(): List<GroupConfig> = if (!previewsEnabled) emptyList()
  else previewProviders.lastOrNull()
    ?.invoke(
      pref.data
        .map { it.selectedGroups }
        .state(emptySet())
        .toList()
    ) ?: emptyList()

  @Composable fun Previews(vararg keys: Any?, block: @Composable (List<Int>) -> List<GroupConfig>) {
    DisposableEffect(keys) {
      previewProviders += block
      onDispose { previewProviders -= block }
    }
  }
}
