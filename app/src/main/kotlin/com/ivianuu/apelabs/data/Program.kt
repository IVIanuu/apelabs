package com.ivianuu.apelabs.data

import androidx.compose.ui.graphics.*
import com.ivianuu.apelabs.domain.*
import com.ivianuu.essentials.db.*
import kotlinx.serialization.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

data class Program(
  val id: String = randomId(),
  val items: List<Item> = listOf(Item())
) {
  init {
    check(items.size in ITEM_RANGE) {
      "Items not in range $items"
    }
  }

  data class Item(
    val color: ApeColor = ApeColor(white = 1f),
    val fadeTime: Duration = 1.seconds,
    val holdTime: Duration = 1.seconds
  )

  companion object {
    val ITEM_RANGE = 1..4
    private const val COLOR_PICKER_ID = "895b4f7f-bc25-4d5a-bf5a-0eb7050601ce"

    fun colorPickerId(groups: List<Int>): String {
      val suffix = groups.toGroupByte().toString()
      return COLOR_PICKER_ID.dropLast(suffix.length) + suffix
    }

    val RAINBOW = Program(
      id = "50217819-189d-45a0-8ef4-5589df5ca466",
      items = listOf(
        Item(Color.Red.toApeColor()),
        Item(Color.Yellow.toApeColor()),
        Item(Color.Green.toApeColor()),
        Item(Color.Blue.toApeColor())
      )
    )
  }
}

@Serializable @Entity data class ProgramEntity(
  @PrimaryKey val id: String,
  val items: List<Item>
) {
  @Serializable data class Item(
    val color: String,
    val fadeTime: Duration,
    val holdTime: Duration
  )
}
