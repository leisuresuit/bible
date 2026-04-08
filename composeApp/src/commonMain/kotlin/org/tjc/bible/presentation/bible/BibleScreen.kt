package org.tjc.bible.presentation.bible

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.error
import bible.composeapp.generated.resources.ok
import bible.composeapp.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.presentation.bible.components.*
import org.tjc.bible.presentation.ui.supportsDynamicColor

@Composable
fun BibleScreen(
    viewModel: BibleViewModel,
    onNavigateToSearch: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onIntent(BibleIntent.LoadInitialData)
    }

    Scaffold(
        topBar = {
            BibleTopBar(
                currentBook = state.currentBook,
                currentChapter = state.currentChapter,
                selectedVersions = state.selectedVersions,
                onShowPassageSelection = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.PassageSelection(it))) },
                onShowVersionSelection = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.VersionSelection)) },
                onShowSearch = onNavigateToSearch,
                onShowHistory = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.History)) },
                onShowSettings = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.Settings)) }
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
            isLoading = state.isLoading,
            onShowPassageSelection = { viewModel.onIntent(BibleIntent.ShowDialog(ActiveDialog.PassageSelection(it))) },
            onUpdateVisiblePassage = { book, chapter -> viewModel.onIntent(BibleIntent.UpdateVisiblePassage(book, chapter)) },
            onLoadChapterVerses = { book, chapter, page -> viewModel.onIntent(BibleIntent.LoadChapterVerses(book, chapter, page)) },
            modifier = Modifier.padding(padding)
        )

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

                is ActiveDialog.Error -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.onIntent(BibleIntent.DismissError) },
                        title = { Text(stringResource(Res.string.error)) },
                        text = { Text(dialog.message) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onIntent(BibleIntent.DismissError) }) {
                                Text(stringResource(Res.string.ok))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    viewModel.onIntent(BibleIntent.DismissError)
                                    viewModel.onIntent(BibleIntent.RetryOperation(dialog.operation))
                                }
                            ) {
                                Text(stringResource(Res.string.retry))
                            }
                        }
                    )
                }
            }
        }
    }
}
