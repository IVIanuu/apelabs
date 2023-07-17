package com.ivianuu.apelabs.data

import com.ivianuu.essentials.AppScope
import com.ivianuu.essentials.Initial
import com.ivianuu.essentials.Scoped
import com.ivianuu.essentials.app.ScopeWorker
import com.ivianuu.essentials.coroutines.ScopedCoroutineScope
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.data.DataStoreModule
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.TaggedEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.time.Duration

@Serializable data class ApeLabsPrefs(
  val selectedGroups: Set<Int> = emptySet(),
  val programUsages: Map<String, List<Duration>> = emptyMap(),
) {
  @Provide companion object {
    @Provide val dataStoreModule = DataStoreModule("apelabs_prefs") { ApeLabsPrefs() }
  }
}
