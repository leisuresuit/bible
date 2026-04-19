package org.tjc.bible.presentation.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.skie.configuration.annotations.FlowInterop
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tjc.bible.data.local.PreferenceStorage
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.HistoryItem
import org.tjc.bible.domain.model.SearchSort
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.usecase.GetBibleVersionsUseCase
import org.tjc.bible.domain.usecase.GetVersesUseCase
import org.tjc.bible.domain.usecase.SearchUseCase
import org.tjc.bible.presentation.ui.History
import org.tjc.bible.presentation.ui.PassageSelection
import org.tjc.bible.presentation.ui.Search
import org.tjc.bible.presentation.ui.Settings
import org.tjc.bible.presentation.ui.VersionSelection

class BibleViewModel(
    private val getBibleVersionsUseCase: GetBibleVersionsUseCase,
    private val getVersesUseCase: GetVersesUseCase,
    private val searchUseCase: SearchUseCase,
    private val preferenceStorage: PreferenceStorage
) : ViewModel() {
    private val _state = MutableStateFlow(BibleState())
    @FlowInterop.Enabled
    val state: StateFlow<BibleState> = _state.asStateFlow()

    @FlowInterop.Enabled
    private val _effects = MutableSharedFlow<BibleEffect>()
    @FlowInterop.Enabled
    val effects: SharedFlow<BibleEffect> = _effects.asSharedFlow()

    private var nextEventId = 1L

    init {
        observePreferences()
        handleLoadInitialData()
    }

    fun onIntent(intent: BibleIntent) {
        when (intent) {
            is BibleIntent.ShowSheet -> {
                dispatch(BibleAction.ShowSheet(intent.sheet))
                if (intent.sheet == null) {
                    dispatch(BibleAction.SearchQueryChanged(""))
                    dispatch(BibleAction.SearchResultsLoaded(emptyList(), hasMore = false))
                }
            }
            is BibleIntent.SelectVersions -> handleSelectVersions(intent.versions)
            is BibleIntent.ToggleParallelVersion -> handleToggleParallelVersion(intent.version)
            is BibleIntent.SelectBook -> dispatch(BibleAction.BookSelected(intent.book))
            is BibleIntent.SelectChapter -> {
                dispatch(BibleAction.ChapterSelected(intent.chapter))
                val book = _state.value.currentBook
                saveLastPassage(book, intent.chapter)
                loadVerses()
            }
            is BibleIntent.SelectVerse -> {
                dispatch(BibleAction.VerseSelected(intent.verse))
                val book = _state.value.currentBook
                addToHistory(book, _state.value.currentChapter, intent.verse)
            }
            is BibleIntent.SelectPassage -> handleSelectPassage(intent.book, intent.chapter, intent.verse)
            is BibleIntent.UpdateVisiblePassage -> handleUpdateVisiblePassage(intent.book, intent.chapter, intent.verse)
            is BibleIntent.LoadChapterVerses -> handleLoadChapterVerses(intent.book, intent.chapter, intent.globalIndex)
            is BibleIntent.UpdateTheme -> viewModelScope.launch { preferenceStorage.setTheme(intent.theme) }
            is BibleIntent.UpdateDynamicColor -> viewModelScope.launch { preferenceStorage.setDynamicColor(intent.enabled) }
            is BibleIntent.UpdateShowWordsOfJesus -> viewModelScope.launch { preferenceStorage.setShowWordsOfJesus(intent.enabled) }
            is BibleIntent.UpdateDisplayMode -> viewModelScope.launch { preferenceStorage.setDisplayMode(intent.mode) }
            is BibleIntent.NextChapter -> {
                dispatch(BibleAction.NavigateChapter(1))
                saveLastPassage()
                loadVerses()
            }
            is BibleIntent.PreviousChapter -> {
                dispatch(BibleAction.NavigateChapter(-1))
                saveLastPassage()
                loadVerses()
            }
            is BibleIntent.ClearHistory -> viewModelScope.launch { preferenceStorage.saveHistory(emptyList()) }
            is BibleIntent.NavigateToHistoryItem -> {
                val eventId = nextEventId++
                dispatch(BibleAction.HistoryItemNavigated(intent.item, eventId))
                saveLastPassage(intent.item.book, intent.item.chapter, intent.item.verse)
                addToHistory(intent.item.book, intent.item.chapter, intent.item.verse)
                loadVerses()
            }
            is BibleIntent.UpdateSearchQuery -> handleSearch(intent.query)
            is BibleIntent.UpdateSearchSort -> handleSearchSort(intent.sort)
            is BibleIntent.ToggleSearchSortVisibility -> viewModelScope.launch { preferenceStorage.setSearchSortVisible(!_state.value.isSearchSortVisible) }
            is BibleIntent.LoadMoreSearchResults -> handleLoadMoreSearchResults()
            is BibleIntent.RetryOperation -> {
                when (intent.operation) {
                    Operation.LOAD_VERSIONS -> handleLoadInitialData()
                    Operation.LOAD_VERSES -> loadVerses()
                    Operation.SEARCH -> handleSearch(_state.value.searchQuery)
                }
            }
        }
    }

    private fun handleSelectPassage(book: Book, chapter: Int, verse: Int) {
        val eventId = nextEventId++
        dispatch(BibleAction.PassageSelected(book, chapter, verse, eventId))
        saveLastPassage(book, chapter, verse)
        addToHistory(book, chapter, verse)
        loadVerses()
    }

    private fun handleUpdateVisiblePassage(book: Book, chapter: Int, verse: Int?) {
        val v = verse ?: 1
        val currentState = _state.value

        // Only update if something actually changed
        if (currentState.currentBook == book && currentState.currentChapter == chapter && currentState.currentVerse == v) {
            return
        }

        dispatch(BibleAction.VisiblePassageChanged(book, chapter, v))

        // Only load verses and save last passage/history if chapter actually changed
        // This prevents excessive DataStore writes during scrolling within a chapter
        if (currentState.currentBook != book || currentState.currentChapter != chapter) {
            loadVerses()
            saveLastPassage(book, chapter, v)
            addToHistory(book, chapter, v)
        }
    }

    private fun handleLoadChapterVerses(book: Book, chapter: Int, globalIndex: Int) {
        if (_state.value.chaptersVerses.containsKey(globalIndex)) return

        viewModelScope.launch {
            val selectedVersions = _state.value.selectedVersions
            if (selectedVersions.isEmpty()) return@launch

            fetchVerses(selectedVersions, book, chapter).fold(
                onSuccess = { verses ->
                    dispatch(BibleAction.ChapterVersesLoaded(globalIndex, verses))
                },
                onFailure = { error ->
                    viewModelScope.launch {
                        _effects.emit(BibleEffect.ShowSnackbar(
                            message = error.message ?: "Failed to load verses",
                            actionLabel = "Retry",
                            onAction = { onIntent(BibleIntent.LoadChapterVerses(book, chapter, globalIndex)) }
                        ))
                    }
                }
            )
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null
    private var loadMoreJob: kotlinx.coroutines.Job? = null

    private fun handleSearch(query: String) {
        val trimmedQuery = query.trim()
        dispatch(BibleAction.SearchQueryChanged(query))
        
        searchJob?.cancel()
        loadMoreJob?.cancel()
        
        if (trimmedQuery.length < 3) {
            dispatch(BibleAction.SearchResultsLoaded(emptyList(), hasMore = false))
            dispatch(BibleAction.Loading(false))
            return
        }
        
        performSearch(trimmedQuery, _state.value.searchSort)
    }

    private fun handleSearchSort(sort: SearchSort) {
        dispatch(BibleAction.SearchSortChanged(sort))
        val query = _state.value.searchQuery.trim()
        if (query.length >= 3) {
            searchJob?.cancel()
            loadMoreJob?.cancel()
            performSearch(query, sort)
        }
    }

    private fun performSearch(query: String, sort: SearchSort) {
        dispatch(BibleAction.Loading(true))
        searchJob = viewModelScope.launch {
            try {
                val versionId = _state.value.selectedVersions.firstOrNull()?.id ?: run {
                    dispatch(BibleAction.Loading(false))
                    return@launch
                }

                searchUseCase(versionId, query, offset = 0, sort = sort).fold(
                    onSuccess = { response ->
                        if (coroutineContext.isActive) {
                            dispatch(BibleAction.SearchResultsLoaded(response.results, hasMore = response.results.size + response.offset < response.total))
                        }
                    },
                    onFailure = { error ->
                        if (coroutineContext.isActive) {
                            _effects.emit(BibleEffect.ShowSnackbar(
                                message = error.message ?: "Search failed",
                                actionLabel = "Retry",
                                onAction = { onIntent(BibleIntent.RetryOperation(Operation.SEARCH)) }
                            ))
                        }
                    }
                )
            } finally {
                if (coroutineContext.isActive) {
                    dispatch(BibleAction.Loading(false))
                }
            }
        }
    }

    private fun handleLoadMoreSearchResults() {
        val currentState = _state.value
        val trimmedQuery = currentState.searchQuery.trim()
        if (currentState.isSearchingMore || !currentState.hasMoreSearchResults || trimmedQuery.length < 3) return

        dispatch(BibleAction.SearchingMore(true))
        val currentQuery = currentState.searchQuery
        val currentSort = currentState.searchSort
        loadMoreJob = viewModelScope.launch {
            try {
                val versionId = currentState.selectedVersions.firstOrNull()?.id ?: return@launch
                val offset = _state.value.searchResults.size

                searchUseCase(versionId, trimmedQuery, offset = offset, sort = currentSort).fold(
                    onSuccess = { response ->
                        if (_state.value.searchQuery == currentQuery && _state.value.searchSort == currentSort && coroutineContext.isActive) {
                            dispatch(BibleAction.SearchMoreResultsLoaded(response.results, hasMore = response.results.isNotEmpty() && response.results.size + response.offset < response.total))
                        }
                    },
                    onFailure = { error ->
                        if (coroutineContext.isActive) {
                            _effects.emit(BibleEffect.ShowSnackbar(
                                message = error.message ?: "Failed to load more results"
                            ))
                        }
                    }
                )
            } finally {
                if (coroutineContext.isActive) {
                    dispatch(BibleAction.SearchingMore(false))
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferenceStorage.theme.collect { dispatch(BibleAction.ThemeChanged(it)) }
        }
        viewModelScope.launch {
            preferenceStorage.displayMode.collect { dispatch(BibleAction.DisplayModeChanged(it)) }
        }
        viewModelScope.launch {
            preferenceStorage.isDynamicColor.collect { dispatch(BibleAction.DynamicColorChanged(it)) }
        }
        viewModelScope.launch {
            preferenceStorage.showWordsOfJesus.collect { dispatch(BibleAction.ShowWordsOfJesusChanged(it)) }
        }
        viewModelScope.launch {
            preferenceStorage.history.collect { dispatch(BibleAction.HistoryLoaded(it)) }
        }
        viewModelScope.launch {
            preferenceStorage.isSearchSortVisible.collect { sortVisible ->
                if (sortVisible != _state.value.isSearchSortVisible) {
                    dispatch(BibleAction.SearchSortVisibilityToggled)
                }
            }
        }
    }

    private fun dispatch(action: BibleAction) {
        _state.update { currentState -> reduce(currentState, action) }
    }

    private fun reduce(state: BibleState, action: BibleAction): BibleState {
        return when (action) {
            is BibleAction.Loading -> state.copy(isLoading = action.isLoading)
            is BibleAction.DataLoaded -> state.copy(
                versions = action.versions,
                selectedVersions = action.selectedVersions,
                currentBook = action.currentBook,
                currentChapter = action.currentChapter,
                currentVerse = action.currentVerse,
                isLoading = false
            )
            is BibleAction.VersesLoaded -> state.copy(
                verses = action.verses,
                // Update chaptersVerses for the current chapter as well to keep it in sync
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
                chaptersVerses = emptyMap() // Clear cache when versions change
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
                    if (bookIndex < books.size - 1) {
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

    private fun handleLoadInitialData() {
        viewModelScope.launch {
            dispatch(BibleAction.Loading(true))
            getBibleVersionsUseCase(language = "en").fold(
                onSuccess = { versions ->
                    // Restore last passage
                    val (lastBook, lastChapter, lastVerse) = preferenceStorage.lastPassage.first()

                    // Restore selected versions
                    val savedIds = preferenceStorage.selectedVersionIds.first()
                    val savedVersions = versions.filter { it.id in savedIds }

                    val selectedVersions = savedVersions.ifEmpty {
                        versions.filter { it.abbreviation == "NKJV" }
                    }

                    if (versions.isNotEmpty()) {
                        dispatch(BibleAction.DataLoaded(
                            versions = versions,
                            selectedVersions = selectedVersions,
                            currentBook = lastBook,
                            currentChapter = lastChapter,
                            currentVerse = lastVerse
                        ))
                        loadVerses()
                    } else {
                        dispatch(BibleAction.Loading(false))
                    }
                },
                onFailure = { error ->
                    dispatch(BibleAction.Loading(false))
                    viewModelScope.launch {
                        _effects.emit(BibleEffect.ShowSnackbar(
                            message = error.message ?: "Failed to load Bible versions",
                            actionLabel = "Retry",
                            onAction = { onIntent(BibleIntent.RetryOperation(Operation.LOAD_VERSIONS)) }
                        ))
                    }
                }
            )
        }
    }

    private fun saveLastPassage(book: Book? = null, chapter: Int? = null, verse: Int? = null) {
        viewModelScope.launch {
            val currentState = _state.value
            val b = book ?: currentState.currentBook
            val ch = chapter ?: currentState.currentChapter
            val v = verse ?: currentState.currentVerse
            preferenceStorage.setLastPassage(b, ch, v)
        }
    }

    private fun addToHistory(book: Book, chapter: Int, verse: Int) {
        viewModelScope.launch {
            val currentHistory = _state.value.history
            val existingIndex = currentHistory.indexOfFirst { it.book == book && it.chapter == chapter }

            if (existingIndex != -1) {
                // Chapter exists in history: update the verse if it changed, keeping its position
                if (currentHistory[existingIndex].verse != verse) {
                    val newList = currentHistory.toMutableList()
                    newList[existingIndex] = HistoryItem(book, chapter, verse)
                    preferenceStorage.saveHistory(newList)
                }
            } else {
                // New chapter visit: add to the top of history
                val newItem = HistoryItem(book, chapter, verse)
                val newList = (listOf(newItem) + currentHistory).take(50)
                preferenceStorage.saveHistory(newList)
            }
        }
    }

    private fun handleSelectVersions(versions: List<BibleVersion>) {
        dispatch(BibleAction.VersionsChanged(versions))
        viewModelScope.launch {
            preferenceStorage.saveSelectedVersions(versions)
        }
        loadVerses()
    }

    private fun handleToggleParallelVersion(version: BibleVersion) {
        val currentSelected = _state.value.selectedVersions.toMutableList()
        if (currentSelected.any { it.id == version.id }) {
            if (currentSelected.size > 1) {
                currentSelected.removeAll { it.id == version.id }
            }
        } else {
            currentSelected.add(version)
        }
        dispatch(BibleAction.VersionsChanged(currentSelected))
        viewModelScope.launch {
            preferenceStorage.saveSelectedVersions(currentSelected)
        }
        loadVerses()
    }

    private suspend fun fetchVerses(selectedVersions: List<BibleVersion>, book: Book, chapter: Int): Result<List<Verse>> {
        val allVerses = mutableListOf<Verse>()
        if (selectedVersions.size > 1) {
            val results = selectedVersions.map { version ->
                getVersesUseCase(version.id, book, chapter).map { verses ->
                    verses.map { it.copy(versionAbbreviation = version.abbreviation) }
                }
            }

            val failure = results.find { it.isFailure }
            if (failure != null) return Result.failure(failure.exceptionOrNull()!!)

            val versesByVersion = results.map { it.getOrThrow() }

            if (versesByVersion.isNotEmpty()) {
                val maxVerseCount = versesByVersion.maxOf { it.size }
                for (i in 0 until maxVerseCount) {
                    versesByVersion.forEach { versionVerses ->
                        if (i < versionVerses.size) {
                            allVerses.add(versionVerses[i])
                        }
                    }
                }
            }
        } else if (selectedVersions.isNotEmpty()) {
            val result = getVersesUseCase(selectedVersions.first().id, book, chapter)
            if (result.isFailure) return result
            allVerses.addAll(result.getOrThrow())
        }
        return Result.success(allVerses)
    }

    private fun loadVerses() {
        viewModelScope.launch {
            val currentState = state.value
            val selectedVersions = currentState.selectedVersions
            val book = currentState.currentBook
            val chapter = currentState.currentChapter

            fetchVerses(selectedVersions, book, chapter).fold(
                onSuccess = { verses ->
                    dispatch(BibleAction.VersesLoaded(verses))
                },
                onFailure = { error ->
                    viewModelScope.launch {
                        _effects.emit(BibleEffect.ShowSnackbar(
                            message = error.message ?: "Failed to load verses",
                            actionLabel = "Retry",
                            onAction = { onIntent(BibleIntent.RetryOperation(Operation.LOAD_VERSES)) }
                        ))
                    }
                }
            )
        }
    }

    fun onThemeChange(theme: AppTheme) {
        viewModelScope.launch {
            preferenceStorage.setTheme(theme)
        }
    }
}
