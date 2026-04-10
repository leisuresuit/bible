package org.tjc.bible.presentation.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.error
import bible.composeapp.generated.resources.ok
import bible.composeapp.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.presentation.bible.components.*
import org.tjc.bible.presentation.search.SearchScreen
import org.tjc.bible.presentation.ui.supportsDynamicColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(
    viewModel: BibleViewModel
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(BibleIntent.LoadInitialData)
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BibleEffect.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        effect.onAction?.invoke()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BibleTopBar(
                currentBook = state.currentBook,
                currentChapter = state.currentChapter,
                selectedVersions = state.selectedVersions,
                isSearchMode = state.isSearchMode,
                searchQuery = state.searchQuery,
                onSearchQueryChange = { viewModel.onIntent(BibleIntent.UpdateSearchQuery(it)) },
                onSetSearchMode = { viewModel.onIntent(BibleIntent.SetSearchMode(it)) },
                onShowPassageSelection = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.PassageSelection(it))) },
                onShowVersionSelection = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.VersionSelection)) },
                onShowHistory = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.History)) },
                onShowSettings = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.Settings)) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
        }
    ) { padding ->
        if (state.isSearchMode) {
            SearchScreen(
                searchQuery = state.searchQuery,
                searchResults = state.searchResults,
                isLoading = state.isLoading,
                onSearchQueryChange = { viewModel.onIntent(BibleIntent.UpdateSearchQuery(it)) },
                onResultClick = { result ->
                    viewModel.onIntent(
                        BibleIntent.SelectPassage(
                            result.book,
                            result.chapterNumber,
                            result.verseNumber
                        )
                    )
                },
                onBack = { viewModel.onIntent(BibleIntent.SetSearchMode(false)) },
                showTopBar = false,
                contentPadding = padding
            )
        } else {
            VerseList(
                currentBook = state.currentBook,
                currentChapter = state.currentChapter,
                currentVerse = state.currentVerse,
                verses = state.verses,
                chaptersVerses = state.chaptersVerses,
                displayMode = state.displayMode,
                showWordsOfJesus = state.showWordsOfJesus,
                selectionEventId = state.selectionEventId,
                isLoading = state.isLoading,
                onShowPassageSelection = {
                    viewModel.onIntent(
                        BibleIntent.ShowDialog(
                            ActiveDialog.PassageSelection(it)
                        )
                    )
                },
                onUpdateVisiblePassage = { book, chapter ->
                    viewModel.onIntent(
                        BibleIntent.UpdateVisiblePassage(book, chapter)
                    )
                },
                onLoadChapterVerses = { book, chapter, page ->
                    viewModel.onIntent(
                        BibleIntent.LoadChapterVerses(book, chapter, page)
                    )
                },
                contentPadding = padding,
                nestedScrollConnection = scrollBehavior.nestedScrollConnection
            )
        }

        // Dialogs
        state.activeDialog?.let { dialog ->
            when (dialog) {
                is ActiveDialog.PassageSelection -> {
                    PassageSelectionDialog(
                        currentBook = state.currentBook,
                        currentChapter = state.currentChapter,
                        currentVerse = state.currentVerse,
                        initialPage = dialog.initialPage,
                        onPassageSelected = { book, chapter, verse ->
                            viewModel.onIntent(BibleIntent.SelectPassage(book, chapter, verse))
                        },
                        onDismiss = { viewModel.onIntent(BibleIntent.ShowDialog(null)) }
                    )
                }

                is ActiveDialog.VersionSelection -> {
                    VersionSelectionDialog(
                        versions = state.versions,
                        selectedVersions = state.selectedVersions,
                        onVersionToggle = { viewModel.onIntent(BibleIntent.ToggleParallelVersion(it)) },
                        onDismiss = { viewModel.onIntent(BibleIntent.ShowDialog(null)) }
                    )
                }

                is ActiveDialog.Settings -> {
                    SettingsDialog(
                        displayMode = state.displayMode,
                        showWordsOfJesus = state.showWordsOfJesus,
                        theme = state.theme,
                        isDynamicColor = state.isDynamicColor,
                        supportsDynamicColor = supportsDynamicColor,
                        onDisplayModeChange = { viewModel.onIntent(BibleIntent.UpdateDisplayMode(it)) },
                        onShowWordsOfJesusChange = { viewModel.onIntent(BibleIntent.UpdateShowWordsOfJesus(it)) },
                        onThemeChange = { viewModel.onIntent(BibleIntent.UpdateTheme(it)) },
                        onDynamicColorChange = { viewModel.onIntent(BibleIntent.UpdateDynamicColor(it)) },
                        onDismiss = { viewModel.onIntent(BibleIntent.ShowDialog(null)) }
                    )
                }

                is ActiveDialog.History -> {
                    HistoryDialog(
                        history = state.history,
                        currentBook = state.currentBook,
                        currentChapter = state.currentChapter,
                        currentVerse = state.currentVerse,
                        onDismiss = { viewModel.onIntent(BibleIntent.ShowDialog(null)) },
                        onItemClick = { viewModel.onIntent(BibleIntent.NavigateToHistoryItem(it)) },
                        onClear = { viewModel.onIntent(BibleIntent.ClearHistory) }
                    )
                }
            }
        }
    }
}
