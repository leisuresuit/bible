package org.tjc.bible.presentation.bible

import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.HistoryItem
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.Verse

enum class DisplayMode {
    SINGLE_CHAPTER, CONTIGUOUS
}

data class BibleState(
    val versions: List<BibleVersion> = emptyList(),
    val selectedVersions: List<BibleVersion> = emptyList(),
    val currentBook: Book? = null,
    val currentChapter: Int = 1,
    val currentVerse: Int? = null,
    val verses: List<Verse> = emptyList(),
    val chaptersVerses: Map<Int, List<Verse>> = emptyMap(),
    val history: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchResults: List<SearchResult> = emptyList(),
    val activeDialog: ActiveDialog? = null,
    val theme: AppTheme = AppTheme.SYSTEM,
    val isDynamicColor: Boolean = true,
    val showWordsOfJesus: Boolean = true,
    val searchQuery: String = "",
    val displayMode: DisplayMode = DisplayMode.SINGLE_CHAPTER,
    val allBooks: List<Book> = Book.entries
)

enum class Operation {
    LOAD_VERSIONS, LOAD_VERSES, SEARCH
}

sealed class ActiveDialog {
    object VersionSelection : ActiveDialog()
    data class PassageSelection(val initialPage: Int = 0) : ActiveDialog()
    object Settings : ActiveDialog()
    object History : ActiveDialog()
    data class Error(val message: String, val operation: Operation) : ActiveDialog()
}

sealed class BibleIntent {
    object LoadInitialData : BibleIntent()
    data class SelectVersions(val versions: List<BibleVersion>) : BibleIntent()
    data class ToggleParallelVersion(val version: BibleVersion) : BibleIntent()
    data class SelectBook(val book: Book) : BibleIntent()
    data class SelectChapter(val chapter: Int) : BibleIntent()
    data class SelectVerse(val verse: Int) : BibleIntent()
    data class SelectPassage(val book: Book, val chapter: Int, val verse: Int? = null) : BibleIntent()
    data class UpdateVisiblePassage(val book: Book, val chapter: Int) : BibleIntent()
    data class LoadChapterVerses(val book: Book, val chapter: Int, val globalIndex: Int) : BibleIntent()
    data class UpdateSearchQuery(val query: String) : BibleIntent()
    object ClearHistory : BibleIntent()
    data class UpdateTheme(val theme: AppTheme) : BibleIntent()
    data class UpdateDynamicColor(val enabled: Boolean) : BibleIntent()
    data class UpdateShowWordsOfJesus(val enabled: Boolean) : BibleIntent()
    data class ShowDialog(val dialog: ActiveDialog?) : BibleIntent()
    data class NavigateToHistoryItem(val item: HistoryItem) : BibleIntent()
    data class UpdateDisplayMode(val mode: DisplayMode) : BibleIntent()
    object NextChapter : BibleIntent()
    object PreviousChapter : BibleIntent()
    object DismissError : BibleIntent()
    data class RetryOperation(val operation: Operation) : BibleIntent()
}

internal sealed class BibleAction {
    data class DataLoaded(
        val versions: List<BibleVersion>,
        val selectedVersions: List<BibleVersion>,
        val currentBook: Book?,
        val currentChapter: Int
    ) : BibleAction()
    data class VersesLoaded(val verses: List<Verse>) : BibleAction()
    data class ChapterVersesLoaded(val globalIndex: Int, val verses: List<Verse>) : BibleAction()
    data class Loading(val isLoading: Boolean) : BibleAction()
    data class DialogChanged(val dialog: ActiveDialog?) : BibleAction()
    data class ThemeChanged(val theme: AppTheme) : BibleAction()
    data class DynamicColorChanged(val enabled: Boolean) : BibleAction()
    data class ShowWordsOfJesusChanged(val enabled: Boolean) : BibleAction()
    data class DisplayModeChanged(val mode: DisplayMode) : BibleAction()
    data class BookSelected(val book: Book) : BibleAction()
    data class ChapterSelected(val chapter: Int) : BibleAction()
    data class VerseSelected(val verse: Int?) : BibleAction()
    data class PassageSelected(val book: Book, val chapter: Int, val verse: Int?) : BibleAction()
    data class VisiblePassageChanged(val book: Book, val chapter: Int) : BibleAction()
    data class VersionsChanged(val selected: List<BibleVersion>) : BibleAction()
    data class NavigateChapter(val delta: Int) : BibleAction()
    data class HistoryLoaded(val history: List<HistoryItem>) : BibleAction()
    data class SearchQueryChanged(val query: String) : BibleAction()
    data class SearchResultsLoaded(val results: List<SearchResult>) : BibleAction()
    data class HistoryItemNavigated(val item: HistoryItem) : BibleAction()
    data class ErrorOccurred(val operation: Operation, val message: String) : BibleAction()
    object DismissError : BibleAction()
}
