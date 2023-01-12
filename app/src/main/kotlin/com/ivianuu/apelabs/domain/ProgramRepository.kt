package com.ivianuu.apelabs.domain

import com.ivianuu.apelabs.data.ApeLabsPrefsContext
import com.ivianuu.apelabs.data.Program
import com.ivianuu.essentials.AppScope
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

context(ApeLabsPrefsContext)
@Provide @Scoped<AppScope> class ProgramRepository {
  val programs: Flow<Map<String, Program.MultiColor>>
    get() = pref.data
      .map { it.programs }
      .distinctUntilChanged()

  fun program(id: String) = programs
    .map { it[id] }
    .distinctUntilChanged()

  suspend fun updateProgram(id: String, program: Program.MultiColor) {
    pref.updateData {
      copy(
        programs = programs.toMutableMap()
          .apply { put(id, program) }
      )
    }
  }

  suspend fun deleteProgram(id: String) {
    pref.updateData {
      copy(
        programs = programs
          .filterKeys { it != id }
      )
    }
  }
}
