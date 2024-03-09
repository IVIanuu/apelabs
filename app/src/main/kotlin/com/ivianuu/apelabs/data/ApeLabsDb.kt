package com.ivianuu.apelabs.data

import com.ivianuu.essentials.*
import com.ivianuu.essentials.db.*
import com.ivianuu.injekt.*

@Provide fun apeLabsDb(factory: DbFactory): @Scoped<AppScope> Db = factory.createDb(
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
  )
)
