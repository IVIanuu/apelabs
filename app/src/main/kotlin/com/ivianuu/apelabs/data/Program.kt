package com.ivianuu.apelabs.data

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.db.AbstractEntityDescriptor
import com.ivianuu.essentials.db.PrimaryKey
import com.ivianuu.essentials.time.seconds
import kotlinx.serialization.Serializable
import kotlin.time.Duration

data class Program(
  val id: String = randomId(),
  val items: List<Item> = listOf(Item())
) {
  init {
    check(items.size in ITEM_RANGE)
  }

  data class Item(
    val color: ApeColor = ApeColor(white = 1f),
    val fadeTime: Duration = 1.seconds,
    val holdTime: Duration = 1.seconds
  )

  companion object {
    val ITEM_RANGE = 1..4
    const val COLOR_PICKER_ID = "895b4f7f-bc25-4d5a-bf5a-0eb7050601ce"
    val RAINBOW = Program(
      id = "50217819-189d-45a0-8ef4-5589df5ca466",
      items = listOf(
        Item(Color.Red.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Yellow.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Green.toApeColor(), Duration.INFINITE, Duration.INFINITE),
        Item(Color.Blue.toApeColor(), Duration.INFINITE, Duration.INFINITE)
      )
    )
  }
}

@Serializable data class ProgramEntity(
  @PrimaryKey val id: String,
  val items: List<Item>
) {
  @Serializable data class Item(
    val color: String,
    val fadeTime: Duration,
    val holdTime: Duration
  )

  companion object : AbstractEntityDescriptor<ProgramEntity>("programs")
}
