package com.ivianuu.apelabs

import com.ivianuu.essentials.AppContext
import com.ivianuu.essentials.db.AndroidDb
import com.ivianuu.essentials.db.Db
import com.ivianuu.essentials.db.Schema
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.IOCoroutineContext

interface DbFactory {
  operator fun invoke(
    name: String,
    schema: Schema
  ): Db
}

@Provide class DbFactoryImpl(
  private val appContext: AppContext,
  private val ioCoroutineContext: IOCoroutineContext
) : DbFactory {
  override fun invoke(name: String, schema: Schema) = AndroidDb(
    name = name,
    schema = schema,
    coroutineContext = ioCoroutineContext,
    context = appContext
  )
}
