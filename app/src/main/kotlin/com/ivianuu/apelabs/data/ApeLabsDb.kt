package com.ivianuu.apelabs.data

import com.ivianuu.essentials.AppContext
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.db.AndroidDb
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.EntityDescriptor
import com.ivianuu.essentials.db.Migration
import com.ivianuu.essentials.db.Schema
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.inject

context(AppContext) @Provide fun apeLabsDb(ioContext: IOContext): @Scoped<AppScope> Db = AndroidDb(
  context = inject(),
  name = "ape_labs.db",
  schema = Schema(
    version = 3,
    entities = listOf(
      EntityDescriptor<ApeColor>("colors"),
      EntityDescriptor<ProgramEntity>("programs"),
      EntityDescriptor<GroupConfigEntity>("group_configs"),
      EntityDescriptor<SceneEntity>("scenes")
    ),
    migrations = listOf(
      Migration(1, 2) { db, _, _ ->
        db.execute("ALTER TABLE 'programs' ADD COLUMN 'strobe' INTEGER NOT NULL DEFAULT 0", null)
      },
      Migration(2, 3) { db, _, _ ->
        db.execute(
          "ALTER TABLE 'group_configs' ADD COLUMN 'strobe' INTEGER NOT NULL DEFAULT 0",
          null
        )
      }
    )
  ),
  coroutineContext = ioContext
)
