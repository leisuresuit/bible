package org.tjc.bible.presentation.bible

import org.tjc.bible.domain.model.Book

internal fun bibleReducer(state: BibleState, action: BibleAction): BibleState {
    return when (action) {
        is BibleAction.Loading -> state.copy(isLoading = action.isLoading)
        is BibleAction.DataLoaded -> state.copy(
            versions = action.versions,
            selectedVersions = action.selectedVersions,
            currentBook = action.currentBook,
            currentChapter = action.currentChapter,
            currentVerse = action.currentVerse,
            isLoading = false,
        )
        is BibleAction.VersesLoaded -> state.copy(
            verses = action.verses,
            chaptersVerses = state.chaptersVerses + (getGlobalIndex(state.currentBook, state.currentChapter) to action.verses)
        )
        is BibleAction.ChapterVersesLoaded -> state.copy(
            chaptersVerses = state.chaptersVerses + (action.globalIndex to action.verses)
        )
        is BibleAction.ThemeChanged -> state.copy(theme = action.theme)
        is BibleAction.DynamicColorChanged -> state.copy(isDynamicColor = action.enabled)
        is BibleAction.ShowWordsOfJesusChanged -> state.copy(showWordsOfJesus = action.enabled)
        is BibleAction.DisplayModeChanged -> state.copy(displayMode = action.mode)
        is BibleAction.HistoryLoaded -> state.copy(history = action.history)
        is BibleAction.BookSelected -> {
            state.copy(
                currentBook = action.book,
                currentChapter = 1,
                currentVerse = 1
            )
        }
        is BibleAction.ChapterSelected -> {
            state.copy(
                currentChapter = action.chapter,
                currentVerse = 1
            )
        }
        is BibleAction.VerseSelected -> state.copy(
            currentVerse = action.verse
        )
        is BibleAction.PassageSelected -> state.copy(
            currentBook = action.book,
            currentChapter = action.chapter,
            currentVerse = action.verse,
            selectionEventId = action.eventId
        )
        is BibleAction.VisiblePassageChanged -> state.copy(
            currentBook = action.book,
            currentChapter = action.chapter,
            currentVerse = action.verse
        )
        is BibleAction.VersionsChanged -> state.copy(
            selectedVersions = action.selected,
            chaptersVerses = emptyMap()
        )
        is BibleAction.NavigateChapter -> {
            val currentBook = state.currentBook
            val books = Book.entries
            val bookIndex = books.indexOf(currentBook)
            
            var nextChapter = state.currentChapter + action.delta
            var nextBook = currentBook
            
            if (nextChapter < 1) {
                if (bookIndex > 0) {
                    nextBook = books[bookIndex - 1]
                    nextChapter = nextBook.chaptersCount
                } else {
                    nextChapter = 1
                }
            } else if (nextChapter > currentBook.chaptersCount) {
                if (bookIndex < (books.size - 1)) {
                    nextBook = books[bookIndex + 1]
                    nextChapter = 1
                } else {
                    nextChapter = currentBook.chaptersCount
                }
            }
            state.copy(currentBook = nextBook, currentChapter = nextChapter)
        }
        is BibleAction.SearchQueryChanged -> state.copy(
            searchQuery = action.query,
            searchResults = if (action.query.trim().length < 3) emptyList() else state.searchResults,
            hasMoreSearchResults = true
        )
        is BibleAction.ShowSheet -> state.copy(activeSheet = action.sheet)
        is BibleAction.SearchSortChanged -> state.copy(searchSort = action.sort)
        is BibleAction.SearchSortVisibilityToggled -> state.copy(isSearchSortVisible = !state.isSearchSortVisible)
        is BibleAction.VersionLanguageFilterVisibilityToggled -> state.copy(isVersionLanguageFilterVisible = !state.isVersionLanguageFilterVisible)
        is BibleAction.SelectedLanguagesChanged -> state.copy(selectedLanguages = action.languages)
        is BibleAction.SearchResultsLoaded -> state.copy(
            searchResults = action.results,
            hasMoreSearchResults = action.hasMore,
            isLoading = false
        )
        is BibleAction.SearchMoreResultsLoaded -> {
            val combined = state.searchResults + action.results
            state.copy(
                searchResults = combined.distinctBy { it.id },
                hasMoreSearchResults = action.hasMore
            )
        }
        is BibleAction.SearchingMore -> state.copy(isSearchingMore = action.isSearching)
        is BibleAction.HistoryItemNavigated -> state.copy(
            currentBook = action.item.book,
            currentChapter = action.item.chapter,
            currentVerse = action.item.verse,
            selectionEventId = action.eventId
        )
    }
}

private fun getGlobalIndex(book: Book, chapter: Int): Int {
    var sum = 0
    for (b in Book.entries) {
        if (b == book) break
        sum += b.chaptersCount
    }
    return sum + (chapter - 1)
}
