package org.tjc.bible

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object Bible : NavRoute
    @Serializable
    data object Search : NavRoute
}

@OptIn(ExperimentalSerializationApi::class)
val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<NavRoute>()
        }
    }
}
