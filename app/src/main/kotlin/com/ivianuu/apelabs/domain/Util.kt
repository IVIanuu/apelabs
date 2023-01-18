package com.ivianuu.apelabs.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEmpty

inline fun <reified A, reified B> Flow<List<A>>.mapEntities(
  crossinline transform: (A) -> Flow<B>
): Flow<List<B>> = flatMapLatest { a ->
  combine(a.map(transform)) { it.toList() }
    .onEmpty { emit(emptyList()) }
}.distinctUntilChanged()

inline fun <reified A, reified B> Flow<A?>.mapEntity(
  crossinline transform: (A) -> Flow<B>
): Flow<B?> = flatMapLatest { it?.let(transform) ?: flowOf(null) }
  .distinctUntilChanged()
