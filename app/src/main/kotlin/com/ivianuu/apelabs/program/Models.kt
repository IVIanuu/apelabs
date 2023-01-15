package com.ivianuu.apelabs.program

import com.ivianuu.apelabs.color.ApeColor
import com.ivianuu.apelabs.util.randomId
import com.ivianuu.essentials.db.AbstractEntityDescriptor
import com.ivianuu.essentials.db.AutoIncrement
import com.ivianuu.essentials.db.PrimaryKey
import com.ivianuu.essentials.time.seconds
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable data class Program(val id: String, val items: List<Item>) {
  init {
    check(items.size in ITEM_RANGE || id == "Rainbow")
  }

  @Serializable data class Item(
    val id: String,
    val color: ApeColor,
    val fade: Duration = 1.seconds,
    val hold: Duration = 1.seconds
  )

  companion object {
    val ITEM_RANGE = 1..4
    val RAINBOW = Program("Rainbow", emptyList())
  }
}

fun ApeColor.asProgram(id: String = "Color") =
  Program(id = id, items = listOf(Program.Item(id = randomId(), color = this)))

@Serializable data class ProgramEntity(
  @PrimaryKey val id: String,
  val items: List<String>
) {
  @Serializable data class Item(
    @PrimaryKey val id: String,
    val color: String,
    val fade: Long,
    val hold: Long
  ) {
    companion object : AbstractEntityDescriptor<Item>("program_items")
  }

  companion object : AbstractEntityDescriptor<ProgramEntity>("programs")
}
