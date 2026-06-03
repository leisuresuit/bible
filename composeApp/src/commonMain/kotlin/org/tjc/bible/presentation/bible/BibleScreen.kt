package org.tjc.bible.presentation.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation3.runtime.NavKey
import org.tjc.bible.presentation.bible.components.BibleTopBar
import org.tjc.bible.presentation.bible.components.VerseList
import org.tjc.bible.LocalShowTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(
    viewModel: BibleViewModel,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val isTopBarVisible = LocalShowTopBar.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = if (isTopBarVisible) {
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    } else {
        null
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BibleEffect.Navigate -> onNavigate(effect.destination)
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
        modifier = modifier
            .fillMaxSize()
            .then(if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier),
        topBar = {
            if (isTopBarVisible) {
                BibleTopBar(
                    currentBook = state.currentBook,
                    currentChapter = state.currentChapter,
                    selectedVersions = state.selectedVersions,
                    onShowSearch = { viewModel.onIntent(BibleIntent.ShowSheet(ActiveSheet.Search)) },
                    onShowPassageSelection = { viewModel.onIntent(BibleIntent.ShowSheet(ActiveSheet.PassageSelection(it))) },
                    onShowVersionSelection = { viewModel.onIntent(BibleIntent.ShowSheet(ActiveSheet.VersionSelection)) },
                    onShowHistory = { viewModel.onIntent(BibleIntent.ShowSheet(ActiveSheet.History)) },
                    onShowSettings = { viewModel.onIntent(BibleIntent.ShowSheet(ActiveSheet.Settings)) },
                    scrollBehavior = scrollBehavior
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .statusBarsPadding()
                )
            }
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
                    BibleIntent.ShowSheet(
                        ActiveSheet.PassageSelection(it)
                    )
                )
            },
            onUpdateVisiblePassage = { book, chapter, verse ->
                viewModel.onIntent(
                    BibleIntent.UpdateVisiblePassage(book, chapter, verse)
                )
            },
            onLoadChapterVerses = { book, chapter, page ->
                viewModel.onIntent(
                    BibleIntent.LoadChapterVerses(book, chapter, page)
                )
            },
            contentPadding = padding,
            nestedScrollConnection = scrollBehavior?.nestedScrollConnection
        )
    }
}
