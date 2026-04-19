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

@Serializable
data class PassageSelection(val initialPage: Int = 0) : NavKey

@Serializable
data object VersionSelection : NavKey

@Serializable
data object History : NavKey

@Serializable
data object Settings : NavKey

val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Bible::class, Bible.serializer())
            subclass(Search::class, Search.serializer())
            subclass(PassageSelection::class, PassageSelection.serializer())
            subclass(VersionSelection::class, VersionSelection.serializer())
            subclass(History::class, History.serializer())
            subclass(Settings::class, Settings.serializer())
        }
    }
}
