package org.tjc.bible.presentation.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tjc.bible.data.local.PreferenceStorage
import org.tjc.bible.domain.model.*
import org.tjc.bible.domain.usecase.GetBibleVersionsUseCase
import org.tjc.bible.domain.usecase.GetVersesUseCase
import org.tjc.bible.domain.usecase.SearchUseCase
import org.tjc.bible.presentation.bible.ActiveDialog.Error
import org.tjc.bible.presentation.bible.ActiveDialog.PassageSelection

class BibleViewModel(
    private val getBibleVersionsUseCase: GetBibleVersionsUseCase,
    private val getVersesUseCase: GetVersesUseCase,
    private val searchUseCase: SearchUseCase,
    private val preferenceStorage: PreferenceStorage
) : ViewModel() {
    private val _state = MutableStateFlow(BibleState())
    val state: StateFlow<BibleState> = _state.asStateFlow()

    init {
        observePreferences()
    }

    fun onIntent(intent: BibleIntent) {
        when (intent) {
            is BibleIntent.LoadInitialData -> handleLoadInitialData()
            is BibleIntent.SelectVersions -> handleSelectVersions(intent.versions)
            is BibleIntent.ToggleParallelVersion -> handleToggleParallelVersion(intent.version)
            is BibleIntent.SelectBook -> dispatch(BibleAction.BookSelected(intent.book))
            is BibleIntent.SelectChapter -> {
                dispatch(BibleAction.ChapterSelected(intent.chapter))
                saveLastPassage(intent.chapter)
                loadVerses()
            }
            is BibleIntent.SelectVerse -> {
                dispatch(BibleAction.VerseSelected(intent.verse))
                addToHistory(intent.verse)
            }
            is BibleIntent.SelectPassage -> handleSelectPassage(intent.book, intent.chapter, intent.verse)
            is BibleIntent.UpdateVisiblePassage -> handleUpdateVisiblePassage(intent.book, intent.chapter)
            is BibleIntent.LoadChapterVerses -> handleLoadChapterVerses(intent.book, intent.chapter, intent.globalIndex)
            is BibleIntent.ShowDialog -> dispatch(BibleAction.DialogChanged(intent.dialog))
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
                dispatch(BibleAction.HistoryItemNavigated(intent.item))
                saveLastPassage(intent.item.chapter)
                loadVerses()
            }
            is BibleIntent.UpdateSearchQuery -> handleSearch(intent.query)
            is BibleIntent.DismissError -> dispatch(BibleAction.DismissError)
            is BibleIntent.RetryOperation -> {
                when (intent.operation) {
                    Operation.LOAD_VERSIONS -> handleLoadInitialData()
                    Operation.LOAD_VERSES -> loadVerses()
                    Operation.SEARCH -> handleSearch(_state.value.searchQuery)
                }
            }
        }
    }

    private fun handleSelectPassage(book: Book, chapter: Int, verse: Int?) {
        dispatch(BibleAction.PassageSelected(book, chapter, verse))
        saveLastPassage(chapter)
        if (verse != null) {
            addToHistory(verse)
        }
        loadVerses()
    }

    private fun handleUpdateVisiblePassage(book: Book, chapter: Int) {
        dispatch(BibleAction.VisiblePassageChanged(book, chapter))
        saveLastPassage(chapter)
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
                    dispatch(BibleAction.ErrorOccurred(Operation.LOAD_VERSES, error.message ?: "Failed to load verses"))
                }
            )
        }
    }

    private fun handleSearch(query: String) {
        dispatch(BibleAction.SearchQueryChanged(query))
        if (query.length < 3) {
            dispatch(BibleAction.SearchResultsLoaded(emptyList()))
            return
        }
        viewModelScope.launch {
            val versionId = _state.value.selectedVersions.firstOrNull()?.id ?: return@launch

            searchUseCase(versionId, query).fold(
                onSuccess = { results ->
                    dispatch(BibleAction.SearchResultsLoaded(results))
                },
                onFailure = { error ->
                    dispatch(BibleAction.ErrorOccurred(
                        Operation.SEARCH,
                        error.message ?: "Search failed")
                    )
                }
            )
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferenceStorage.theme,
                preferenceStorage.displayMode,
                preferenceStorage.isDynamicColor,
                preferenceStorage.showWordsOfJesus,
                preferenceStorage.history
            ) { theme, mode, dynamic, showWJ, history ->
                dispatch(BibleAction.ThemeChanged(theme))
                dispatch(BibleAction.DisplayModeChanged(mode))
                dispatch(BibleAction.DynamicColorChanged(dynamic))
                dispatch(BibleAction.ShowWordsOfJesusChanged(showWJ))
                dispatch(BibleAction.HistoryLoaded(history))
            }.collectLatest { }
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
                isLoading = false
            )
            is BibleAction.VersesLoaded -> state.copy(
                verses = action.verses,
                // Update chaptersVerses for the current chapter as well to keep it in sync
                chaptersVerses = state.chaptersVerses + (getGlobalIndex(state.currentBook!!, state.currentChapter) to action.verses)
            )
            is BibleAction.ChapterVersesLoaded -> state.copy(
                chaptersVerses = state.chaptersVerses + (action.globalIndex to action.verses)
            )
            is BibleAction.DialogChanged -> state.copy(activeDialog = action.dialog)
            is BibleAction.ThemeChanged -> state.copy(theme = action.theme)
            is BibleAction.DynamicColorChanged -> state.copy(isDynamicColor = action.enabled)
            is BibleAction.ShowWordsOfJesusChanged -> state.copy(showWordsOfJesus = action.enabled)
            is BibleAction.DisplayModeChanged -> state.copy(displayMode = action.mode)
            is BibleAction.HistoryLoaded -> state.copy(history = action.history)
            is BibleAction.BookSelected -> {
                if (state.currentBook != action.book) {
                    state.copy(
                        currentBook = action.book,
                        currentChapter = 1,
                        currentVerse = 1,
                        activeDialog = PassageSelection(1)
                    )
                } else {
                    state.copy(activeDialog = PassageSelection(1))
                }
            }
            is BibleAction.ChapterSelected -> {
                if (state.currentChapter != action.chapter) {
                    state.copy(
                        currentChapter = action.chapter,
                        currentVerse = 1,
                        activeDialog = PassageSelection(2)
                    )
                } else {
                    state.copy(activeDialog = PassageSelection(2))
                }
            }
            is BibleAction.VerseSelected -> state.copy(
                currentVerse = action.verse,
                activeDialog = null
            )
            is BibleAction.PassageSelected -> state.copy(
                currentBook = action.book,
                currentChapter = action.chapter,
                currentVerse = action.verse,
                activeDialog = null
            )
            is BibleAction.VisiblePassageChanged -> state.copy(
                currentBook = action.book,
                currentChapter = action.chapter,
                currentVerse = null // Reset verse when swiping to new chapter
            )
            is BibleAction.VersionsChanged -> state.copy(
                selectedVersions = action.selected,
                chaptersVerses = emptyMap() // Clear cache when versions change
            )
            is BibleAction.NavigateChapter -> {
                val currentBook = state.currentBook ?: return state
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
            is BibleAction.SearchQueryChanged -> state.copy(searchQuery = action.query)
            is BibleAction.SearchResultsLoaded -> state.copy(searchResults = action.results)
            is BibleAction.HistoryItemNavigated -> state.copy(
                currentBook = action.item.book,
                currentChapter = action.item.chapter,
                currentVerse = action.item.verse
            )
            is BibleAction.ErrorOccurred -> state.copy(
                activeDialog = Error(action.message, action.operation)
            )
            BibleAction.DismissError -> state.copy(activeDialog = null)
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
                    val (lastBook, lastChapter) = preferenceStorage.lastPassage.first()

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
                            currentChapter = lastChapter
                        ))
                        loadVerses()
                    } else {
                        dispatch(BibleAction.Loading(false))
                    }
                },
                onFailure = { error ->
                    dispatch(BibleAction.Loading(false))
                    dispatch(
                        BibleAction.ErrorOccurred(
                            Operation.LOAD_VERSIONS,
                            error.message ?: "Failed to load Bible versions"
                        )
                    )
                }
            )
        }
    }

    private fun saveLastPassage(chapter: Int? = null) {
        viewModelScope.launch {
            val book = _state.value.currentBook ?: return@launch
            val ch = chapter ?: _state.value.currentChapter
            preferenceStorage.setLastPassage(book, ch)
        }
    }

    private fun addToHistory(verse: Int) {
        viewModelScope.launch {
            val book = _state.value.currentBook ?: return@launch
            val chapter = _state.value.currentChapter
            val currentHistory = _state.value.history.toMutableList()
            
            val newItem = HistoryItem(book, chapter, verse, timestamp = 0L) // Timestamp simplified
            currentHistory.add(0, newItem)
            
            preferenceStorage.saveHistory(currentHistory.take(50)) // Keep last 50
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
            val book = currentState.currentBook ?: return@launch
            val chapter = currentState.currentChapter

            fetchVerses(selectedVersions, book, chapter).fold(
                onSuccess = { verses ->
                    dispatch(BibleAction.VersesLoaded(verses))
                },
                onFailure = { error ->
                    dispatch(
                        BibleAction.ErrorOccurred(
                            Operation.LOAD_VERSES,
                            error.message ?: "Failed to load verses")
                    )
                }
            )
        }
    }
}
