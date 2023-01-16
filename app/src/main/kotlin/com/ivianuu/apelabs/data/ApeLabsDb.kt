package com.ivianuu.apelabs.data

import com.ivianuu.apelabs.color.NamedColor
import com.ivianuu.apelabs.group.GroupConfigEntity
import com.ivianuu.apelabs.program.ProgramEntity
import com.ivianuu.apelabs.scene.SceneEntity
import com.ivianuu.essentials.AppContext
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.db.AndroidDb
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.Schema
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext

context(AppContext) @Provide fun apeLabsDb(ioContext: IOContext): @Scoped<AppScope> Db = AndroidDb(
  context = inject(),
  name = "ape_labs.db",
  schema = Schema(
    version = 1,
    entities = listOf(
      NamedColor,
      GroupConfigEntity,
      ProgramEntity,
      ProgramEntity.Item,
      SceneEntity
    ),
    migrations = listOf()
  ),
  coroutineContext = ioContext
)
