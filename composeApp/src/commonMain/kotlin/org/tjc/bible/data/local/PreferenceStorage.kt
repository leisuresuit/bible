package org.tjc.bible.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.HistoryItem
import org.tjc.bible.presentation.bible.DisplayMode
import org.tjc.bible.presentation.ui.deviceLanguage

class PreferenceStorage(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LAST_BOOK = stringPreferencesKey("last_book")
        val LAST_CHAPTER = intPreferencesKey("last_chapter")
        val LAST_VERSE = intPreferencesKey("last_verse")
        val HISTORY = stringPreferencesKey("history")
        val SELECTED_VERSIONS = stringPreferencesKey("selected_versions")
        val SHOW_WORDS_OF_JESUS = booleanPreferencesKey("show_words_of_jesus")
        val SEARCH_SORT_VISIBLE = booleanPreferencesKey("search_sort_visible")
        val VERSION_LANGUAGE_FILTER_VISIBLE = booleanPreferencesKey("version_language_filter_visible")
        val SELECTED_LANGUAGES = stringPreferencesKey("selected_languages")
    }

    val theme: Flow<AppTheme> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val name = prefs[Keys.THEME] ?: AppTheme.SYSTEM.name
            try { AppTheme.valueOf(name) } catch (e: Exception) { AppTheme.SYSTEM }
        }

    val displayMode: Flow<DisplayMode> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val name = prefs[Keys.DISPLAY_MODE] ?: DisplayMode.SINGLE_CHAPTER.name
            try { DisplayMode.valueOf(name) } catch (e: Exception) { DisplayMode.SINGLE_CHAPTER }
        }

    val isDynamicColor: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[Keys.DYNAMIC_COLOR] ?: true
        }

    val showWordsOfJesus: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[Keys.SHOW_WORDS_OF_JESUS] ?: true
        }

    val isSearchSortVisible: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[Keys.SEARCH_SORT_VISIBLE] ?: true
        }

    val isVersionLanguageFilterVisible: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[Keys.VERSION_LANGUAGE_FILTER_VISIBLE] ?: true
        }

    val selectedLanguages: Flow<Set<String>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val json = prefs[Keys.SELECTED_LANGUAGES]
            if (json == null) {
                val defaultLang = if (deviceLanguage.startsWith("zh")) "zh" else "en"
                return@map setOf(defaultLang)
            }
            try {
                Json.decodeFromString<Set<String>>(json)
            } catch (e: Exception) {
                setOf("en")
            }
        }

    val lastPassage: Flow<Triple<Book, Int, Int>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val bookName = prefs[Keys.LAST_BOOK] ?: "Genesis"
            val chapter = prefs[Keys.LAST_CHAPTER] ?: 1
            val verse = prefs[Keys.LAST_VERSE] ?: 1
            val book = Book.entries.find { it.name == bookName } ?: Book.Genesis
            Triple(book, chapter, verse)
        }

    val history: Flow<List<HistoryItem>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val json = prefs[Keys.HISTORY] ?: return@map emptyList()
            try {
                Json.decodeFromString<List<HistoryItem>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    val selectedVersionIds: Flow<List<String>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val json = prefs[Keys.SELECTED_VERSIONS] ?: return@map emptyList()
            try {
                Json.decodeFromString<List<String>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setDisplayMode(mode: DisplayMode) {
        dataStore.edit { it[Keys.DISPLAY_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLastPassage(book: Book, chapter: Int, verse: Int) {
        dataStore.edit {
            it[Keys.LAST_BOOK] = book.name
            it[Keys.LAST_CHAPTER] = chapter
            it[Keys.LAST_VERSE] = verse
        }
    }

    suspend fun saveHistory(history: List<HistoryItem>) {
        dataStore.edit {
            it[Keys.HISTORY] = Json.encodeToString(history)
        }
    }

    suspend fun saveSelectedVersions(versions: List<BibleVersion>) {
        val ids = versions.map { it.id }
        dataStore.edit {
            it[Keys.SELECTED_VERSIONS] = Json.encodeToString(ids)
        }
    }

    suspend fun setShowWordsOfJesus(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_WORDS_OF_JESUS] = enabled }
    }

    suspend fun setSearchSortVisible(visible: Boolean) {
        dataStore.edit { it[Keys.SEARCH_SORT_VISIBLE] = visible }
    }

    suspend fun setVersionLanguageFilterVisible(visible: Boolean) {
        dataStore.edit { it[Keys.VERSION_LANGUAGE_FILTER_VISIBLE] = visible }
    }

    suspend fun setSelectedLanguages(languages: Set<String>) {
        dataStore.edit {
            it[Keys.SELECTED_LANGUAGES] = Json.encodeToString(languages)
        }
    }
}
