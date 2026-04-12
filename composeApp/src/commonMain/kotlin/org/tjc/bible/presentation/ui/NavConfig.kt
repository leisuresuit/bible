package org.tjc.bible.presentation.ui

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data object Bible : NavKey

@Serializable
data object Search : NavKey

val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Bible::class, Bible.serializer())
            subclass(Search::class, Search.serializer())
        }
    }
}
