package com.ivianuu.apelabs.data

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.time.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable data class Program(val items: List<Item> = listOf(Item())) {
  init {
    check(items.size in ITEM_RANGE)
  }

  @Serializable data class Item(
    val color: ApeColor = ApeColor(white = 1f),
    val fadeTime: Duration = 1.seconds,
    val holdTime: Duration = 1.seconds
  )

  companion object {
    val ITEM_RANGE = 1..4
    val RAINBOW = Program(
      items = listOf(
        Item(Color.Red.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Yellow.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Green.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Blue.toApeColor(), Duration.INFINITE, Duration.INFINITE)
      )
    )
  }
}

context(ApeLabsPrefsContext) val programs: Flow<Map<String, Program>>
  get() = pref.data
    .map { it.programs }
    .distinctUntilChanged()

context(ApeLabsPrefsContext) fun program(id: String): Flow<Program?> = programs
  .map { it[id] }
  .distinctUntilChanged()

context(ApeLabsPrefsContext) suspend fun updateProgram(id: String, program: Program) {
  pref.updateData {
    copy(
      programs = programs.toMutableMap()
        .apply { put(id, program) }
    )
  }
}

context(ApeLabsPrefsContext) suspend fun deleteProgram(id: String) {
  pref.updateData {
    copy(
      programs = programs.toMutableMap()
        .apply { remove(id) }
    )
  }
}
