package org.tjc.bible.presentation.bible

import co.touchlab.skie.configuration.annotations.FlowInterop
import androidx.navigation3.runtime.NavKey
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.HistoryItem
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.SearchSort
import org.tjc.bible.domain.model.Verse

enum class DisplayMode {
    SINGLE_CHAPTER, CONTIGUOUS
}

data class BibleState(
    val versions: List<BibleVersion> = emptyList(),
    val selectedVersions: List<BibleVersion> = emptyList(),
    val currentBook: Book? = null,
    val currentChapter: Int = 1,
    val currentVerse: Int = 1,
    val verses: List<Verse> = emptyList(),
    val chaptersVerses: Map<Int, List<Verse>> = emptyMap(),
    val history: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchResults: List<SearchResult> = emptyList(),
    val isSearchingMore: Boolean = false,
    val hasMoreSearchResults: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val isDynamicColor: Boolean = true,
    val showWordsOfJesus: Boolean = true,
    val searchQuery: String = "",
    val searchSort: SearchSort = SearchSort.RELEVANCE,
    val isSearchSortVisible: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.SINGLE_CHAPTER,
    val allBooks: List<Book> = Book.entries,
    val selectionEventId: Long = 0L,
    val activeSheet: ActiveSheet? = null
)

enum class Operation {
    LOAD_VERSIONS, LOAD_VERSES, SEARCH
}

sealed class ActiveSheet {
    object VersionSelection : ActiveSheet()
    data class PassageSelection(val initialPage: Int = 0) : ActiveSheet()
    object Settings : ActiveSheet()
    object History : ActiveSheet()
    object Search : ActiveSheet()
}

sealed class BibleIntent {
    data class SelectVersions(val versions: List<BibleVersion>) : BibleIntent()
    data class ToggleParallelVersion(val version: BibleVersion) : BibleIntent()
    data class SelectBook(val book: Book) : BibleIntent()
    data class SelectChapter(val chapter: Int) : BibleIntent()
    data class SelectVerse(val verse: Int) : BibleIntent()
    data class SelectPassage(val book: Book, val chapter: Int, val verse: Int) : BibleIntent()
    data class UpdateVisiblePassage(val book: Book, val chapter: Int, val verse: Int? = null) : BibleIntent()
    data class LoadChapterVerses(val book: Book, val chapter: Int, val globalIndex: Int) : BibleIntent()
    data class UpdateSearchQuery(val query: String) : BibleIntent()
    data class UpdateSearchSort(val sort: SearchSort) : BibleIntent()
    object ToggleSearchSortVisibility : BibleIntent()
    object ClearHistory : BibleIntent()
    object LoadMoreSearchResults : BibleIntent()
    data class UpdateTheme(val theme: AppTheme) : BibleIntent()
    data class UpdateDynamicColor(val enabled: Boolean) : BibleIntent()
    data class UpdateShowWordsOfJesus(val enabled: Boolean) : BibleIntent()
    data class ShowSheet(val sheet: ActiveSheet?) : BibleIntent()
    data class NavigateToHistoryItem(val item: HistoryItem) : BibleIntent()
    data class UpdateDisplayMode(val mode: DisplayMode) : BibleIntent()
    object NextChapter : BibleIntent()
    object PreviousChapter : BibleIntent()
    data class RetryOperation(val operation: Operation) : BibleIntent()
}

internal sealed class BibleAction {
    data class ShowSheet(val sheet: ActiveSheet?) : BibleAction()
    data class DataLoaded(
        val versions: List<BibleVersion>,
        val selectedVersions: List<BibleVersion>,
        val currentBook: Book?,
        val currentChapter: Int,
        val currentVerse: Int
    ) : BibleAction()
    data class VersesLoaded(val verses: List<Verse>) : BibleAction()
    data class ChapterVersesLoaded(val globalIndex: Int, val verses: List<Verse>) : BibleAction()
    data class Loading(val isLoading: Boolean) : BibleAction()
    data class ThemeChanged(val theme: AppTheme) : BibleAction()
    data class DynamicColorChanged(val enabled: Boolean) : BibleAction()
    data class ShowWordsOfJesusChanged(val enabled: Boolean) : BibleAction()
    data class DisplayModeChanged(val mode: DisplayMode) : BibleAction()
    data class BookSelected(val book: Book) : BibleAction()
    data class ChapterSelected(val chapter: Int) : BibleAction()
    data class VerseSelected(val verse: Int) : BibleAction()
    data class PassageSelected(val book: Book, val chapter: Int, val verse: Int, val eventId: Long) : BibleAction()
    data class VisiblePassageChanged(val book: Book, val chapter: Int, val verse: Int) : BibleAction()
    data class VersionsChanged(val selected: List<BibleVersion>) : BibleAction()
    data class NavigateChapter(val delta: Int) : BibleAction()
    data class HistoryLoaded(val history: List<HistoryItem>) : BibleAction()
    object SearchSortVisibilityToggled : BibleAction()
    data class SearchQueryChanged(val query: String) : BibleAction()
    data class SearchSortChanged(val sort: SearchSort) : BibleAction()
    data class SearchResultsLoaded(val results: List<SearchResult>, val hasMore: Boolean) : BibleAction()
    data class SearchMoreResultsLoaded(val results: List<SearchResult>, val hasMore: Boolean) : BibleAction()
    data class SearchingMore(val isSearching: Boolean) : BibleAction()
    data class HistoryItemNavigated(val item: HistoryItem, val eventId: Long) : BibleAction()
}

sealed class BibleEffect {
    data class Navigate(val destination: NavKey) : BibleEffect()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : BibleEffect()
}
