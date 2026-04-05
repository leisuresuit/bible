package org.tjc.bible.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

expect fun createDataStore(context: Any? = null): DataStore<Preferences>

fun createDataStoreWithPath(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

internal const val DATA_STORE_FILE_NAME = "bible.preferences_pb"
