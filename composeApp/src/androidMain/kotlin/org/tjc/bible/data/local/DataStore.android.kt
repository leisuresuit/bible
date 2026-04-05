package org.tjc.bible.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

actual fun createDataStore(context: Any?): DataStore<Preferences> {
    require(context is Context) { "Context is required for Android DataStore" }
    return PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(DATA_STORE_FILE_NAME) }
    )
}
