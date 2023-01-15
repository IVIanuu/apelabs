package com.ivianuu.apelabs.scene

import com.ivianuu.apelabs.program.ProgramRepository
import com.ivianuu.essentials.db.Db
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.Flow

context(Db, ProgramRepository) @Provide class SceneRepository {

  private fun SceneEntity.toScene(): Flow<Scene> = TODO()

}
