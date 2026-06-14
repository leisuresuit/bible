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
import org.tjc.bible.domain.usecase.GetBibleVersionsUseCase
import org.tjc.bible.domain.usecase.GetParallelVersesUseCase
import org.tjc.bible.domain.usecase.SearchUseCase

class BibleViewModel(
    private val getBibleVersionsUseCase: GetBibleVersionsUseCase,
    private val getParallelVersesUseCase: GetParallelVersesUseCase,
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
    private var searchJob: kotlinx.coroutines.Job? = null
    private var loadMoreJob: kotlinx.coroutines.Job? = null

    init {
        observePreferences()
        handleLoadInitialData()
    }

    fun onIntent(intent: BibleIntent) {
        when (intent) {
            is BibleIntent.ShowSheet -> handleShowSheet(intent.sheet)
            is BibleIntent.ToggleSheet -> handleToggleSheet(intent.sheet)
            is BibleIntent.SelectVersions -> handleSelectVersions(intent.versions)
            is BibleIntent.ToggleParallelVersion -> handleToggleParallelVersion(intent.version)
            is BibleIntent.SelectBook -> dispatch(BibleAction.BookSelected(intent.book))
            is BibleIntent.SelectChapter -> handleSelectChapter(intent.chapter)
            is BibleIntent.SelectVerse -> handleSelectVerse(intent.verse)
            is BibleIntent.SelectPassage -> handleSelectPassage(intent.book, intent.chapter, intent.verse)
            is BibleIntent.UpdateVisiblePassage -> handleUpdateVisiblePassage(intent.book, intent.chapter, intent.verse)
            is BibleIntent.LoadChapterVerses -> handleLoadChapterVerses(intent.book, intent.chapter, intent.globalIndex)
            is BibleIntent.UpdateTheme -> viewModelScope.launch { preferenceStorage.setTheme(intent.theme) }
            is BibleIntent.UpdateDynamicColor -> viewModelScope.launch { preferenceStorage.setDynamicColor(intent.enabled) }
            is BibleIntent.UpdateShowWordsOfJesus -> viewModelScope.launch { preferenceStorage.setShowWordsOfJesus(intent.enabled) }
            is BibleIntent.UpdateDisplayMode -> viewModelScope.launch { preferenceStorage.setDisplayMode(intent.mode) }
            is BibleIntent.NextChapter -> handleNavigateChapter(1)
            is BibleIntent.PreviousChapter -> handleNavigateChapter(-1)
            is BibleIntent.ClearHistory -> viewModelScope.launch { preferenceStorage.saveHistory(emptyList()) }
            is BibleIntent.NavigateToHistoryItem -> handleNavigateToHistoryItem(intent.item)
            is BibleIntent.UpdateSearchQuery -> handleSearch(intent.query)
            is BibleIntent.UpdateSearchSort -> handleSearchSort(intent.sort)
            is BibleIntent.ToggleSearchSortVisibility -> viewModelScope.launch { preferenceStorage.setSearchSortVisible(!_state.value.isSearchSortVisible) }
            is BibleIntent.ToggleVersionLanguageFilterVisibility -> viewModelScope.launch { preferenceStorage.setVersionLanguageFilterVisible(!_state.value.isVersionLanguageFilterVisible) }
            is BibleIntent.UpdateSelectedLanguages -> viewModelScope.launch { preferenceStorage.setSelectedLanguages(intent.languages) }
            is BibleIntent.LoadMoreSearchResults -> handleLoadMoreSearchResults()
            is BibleIntent.RetryOperation -> handleRetry(intent.operation)
        }
    }

    private fun handleShowSheet(sheet: ActiveSheet?) {
        dispatch(BibleAction.ShowSheet(sheet))
    }

    private fun handleToggleSheet(sheet: ActiveSheet) {
        val current = _state.value.activeSheet
        val next = if (current != null && current::class == sheet::class) null else sheet
        handleShowSheet(next)
    }

    private fun handleSelectChapter(chapter: Int) {
        dispatch(BibleAction.ChapterSelected(chapter))
        saveLastPassage(_state.value.currentBook, chapter)
        loadVerses()
    }

    private fun handleSelectVerse(verse: Int) {
        dispatch(BibleAction.VerseSelected(verse))
        addToHistory(_state.value.currentBook, _state.value.currentChapter, verse)
    }

    private fun handleNavigateChapter(delta: Int) {
        dispatch(BibleAction.NavigateChapter(delta))
        saveLastPassage()
        loadVerses()
    }

    private fun handleNavigateToHistoryItem(item: HistoryItem) {
        val eventId = nextEventId++
        dispatch(BibleAction.HistoryItemNavigated(item, eventId))
        saveLastPassage(item.book, item.chapter, item.verse)
        addToHistory(item.book, item.chapter, item.verse)
        loadVerses()
    }

    private fun handleRetry(operation: Operation) {
        when (operation) {
            Operation.LOAD_VERSIONS -> handleLoadInitialData()
            Operation.LOAD_VERSES -> loadVerses()
            Operation.SEARCH -> handleSearch(_state.value.searchQuery)
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

        if (currentState.currentBook == book && currentState.currentChapter == chapter && currentState.currentVerse == v) return

        dispatch(BibleAction.VisiblePassageChanged(book, chapter, v))

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

            getParallelVersesUseCase(selectedVersions, book, chapter).fold(
                onSuccess = { verses -> dispatch(BibleAction.ChapterVersesLoaded(globalIndex, verses)) },
                onFailure = { error ->
                    _effects.emit(BibleEffect.ShowSnackbar(
                        message = error.message ?: "Failed to load verses",
                        actionLabel = "Retry",
                        onAction = { onIntent(BibleIntent.LoadChapterVerses(book, chapter, globalIndex)) }
                    ))
                }
            )
        }
    }

    private fun handleSearch(query: String) {
        dispatch(BibleAction.SearchQueryChanged(query))
        searchJob?.cancel()
        loadMoreJob?.cancel()
        
        val trimmedQuery = query.trim()
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
                if (coroutineContext.isActive) dispatch(BibleAction.Loading(false))
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
                            _effects.emit(BibleEffect.ShowSnackbar(message = error.message ?: "Failed to load more results"))
                        }
                    }
                )
            } finally {
                if (coroutineContext.isActive) dispatch(BibleAction.SearchingMore(false))
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch { preferenceStorage.theme.collect { dispatch(BibleAction.ThemeChanged(it)) } }
        viewModelScope.launch { preferenceStorage.displayMode.collect { dispatch(BibleAction.DisplayModeChanged(it)) } }
        viewModelScope.launch { preferenceStorage.isDynamicColor.collect { dispatch(BibleAction.DynamicColorChanged(it)) } }
        viewModelScope.launch { preferenceStorage.showWordsOfJesus.collect { dispatch(BibleAction.ShowWordsOfJesusChanged(it)) } }
        viewModelScope.launch { preferenceStorage.history.collect { dispatch(BibleAction.HistoryLoaded(it)) } }
        viewModelScope.launch {
            preferenceStorage.isSearchSortVisible.collect { sortVisible ->
                if (sortVisible != _state.value.isSearchSortVisible) dispatch(BibleAction.SearchSortVisibilityToggled)
            }
        }
        viewModelScope.launch {
            preferenceStorage.isVersionLanguageFilterVisible.collect { filterVisible ->
                if (filterVisible != _state.value.isVersionLanguageFilterVisible) dispatch(BibleAction.VersionLanguageFilterVisibilityToggled)
            }
        }
        viewModelScope.launch { preferenceStorage.selectedLanguages.collect { dispatch(BibleAction.SelectedLanguagesChanged(it)) } }
    }

    private fun dispatch(action: BibleAction) {
        _state.update { currentState -> bibleReducer(currentState, action) }
    }

    private fun handleLoadInitialData() {
        viewModelScope.launch {
            dispatch(BibleAction.Loading(true))
            getBibleVersionsUseCase(languages = listOf("en", "zh")).fold(
                onSuccess = { versions ->
                    val (lastBook, lastChapter, lastVerse) = preferenceStorage.lastPassage.first()
                    val savedIds = preferenceStorage.selectedVersionIds.first()
                    val savedVersions = versions.filter { it.id in savedIds }
                    val selectedVersions = savedVersions.ifEmpty { versions.filter { it.abbreviation == "NKJV" } }

                    if (versions.isNotEmpty()) {
                        dispatch(BibleAction.DataLoaded(versions, selectedVersions, lastBook, lastChapter, lastVerse))
                        loadVerses()
                    } else {
                        dispatch(BibleAction.Loading(false))
                    }
                },
                onFailure = { error ->
                    dispatch(BibleAction.Loading(false))
                    _effects.emit(BibleEffect.ShowSnackbar(
                        message = error.message ?: "Failed to load Bible versions",
                        actionLabel = "Retry",
                        onAction = { onIntent(BibleIntent.RetryOperation(Operation.LOAD_VERSIONS)) }
                    ))
                }
            )
        }
    }

    private fun saveLastPassage(book: Book? = null, chapter: Int? = null, verse: Int? = null) {
        viewModelScope.launch {
            val currentState = _state.value
            preferenceStorage.setLastPassage(book ?: currentState.currentBook, chapter ?: currentState.currentChapter, verse ?: currentState.currentVerse)
        }
    }

    private fun addToHistory(book: Book, chapter: Int, verse: Int) {
        viewModelScope.launch {
            val currentHistory = _state.value.history
            val existingIndex = currentHistory.indexOfFirst { it.book == book && it.chapter == chapter }

            if (existingIndex != -1) {
                if (currentHistory[existingIndex].verse != verse) {
                    val newList = currentHistory.toMutableList()
                    newList[existingIndex] = HistoryItem(book, chapter, verse)
                    preferenceStorage.saveHistory(newList)
                }
            } else {
                val newList = (listOf(HistoryItem(book, chapter, verse)) + currentHistory).take(50)
                preferenceStorage.saveHistory(newList)
            }
        }
    }

    private fun handleSelectVersions(versions: List<BibleVersion>) {
        dispatch(BibleAction.VersionsChanged(versions))
        viewModelScope.launch { preferenceStorage.saveSelectedVersions(versions) }
        loadVerses()
    }

    private fun handleToggleParallelVersion(version: BibleVersion) {
        val currentSelected = _state.value.selectedVersions.toMutableList()
        if (currentSelected.any { it.id == version.id }) {
            if (currentSelected.size > 1) currentSelected.removeAll { it.id == version.id }
        } else {
            currentSelected.add(version)
        }
        dispatch(BibleAction.VersionsChanged(currentSelected))
        viewModelScope.launch { preferenceStorage.saveSelectedVersions(currentSelected) }
        loadVerses()
    }

    private fun loadVerses() {
        viewModelScope.launch {
            val currentState = state.value
            getParallelVersesUseCase(currentState.selectedVersions, currentState.currentBook, currentState.currentChapter).fold(
                onSuccess = { verses -> dispatch(BibleAction.VersesLoaded(verses)) },
                onFailure = { error ->
                    _effects.emit(BibleEffect.ShowSnackbar(
                        message = error.message ?: "Failed to load verses",
                        actionLabel = "Retry",
                        onAction = { onIntent(BibleIntent.RetryOperation(Operation.LOAD_VERSES)) }
                    ))
                }
            )
        }
    }

    fun onThemeChange(theme: AppTheme) {
        viewModelScope.launch { preferenceStorage.setTheme(theme) }
    }
}
