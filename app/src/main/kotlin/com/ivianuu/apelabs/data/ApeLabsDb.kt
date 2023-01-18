package com.ivianuu.apelabs.data

import com.ivianuu.essentials.AppContext
import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.db.AndroidDb
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.Schema
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.IOContext
import com.ivianuu.injekt.inject

context(AppContext) @Provide fun apeLabsDb(ioContext: IOContext): @Scoped<AppScope> Db = AndroidDb(
  context = inject(),
  name = "ape_labs.db",
  schema = Schema(
    version = 1,
    entities = listOf(ApeColor, ProgramEntity),
    migrations = listOf()
  ),
  coroutineContext = ioContext
)
