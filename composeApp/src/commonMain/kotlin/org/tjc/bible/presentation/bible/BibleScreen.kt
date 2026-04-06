package org.tjc.bible.presentation.bible

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.tjc.bible.presentation.bible.components.*
import org.tjc.bible.presentation.ui.supportsDynamicColor

@Composable
fun BibleScreen(viewModel: BibleViewModel) {
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
                onIntent = viewModel::onIntent
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
            onIntent = viewModel::onIntent,
            modifier = Modifier.padding(padding)
        )

        // Dialogs
        when (val dialog = state.activeDialog) {
            is ActiveDialog.PassageSelection -> {
                PassageSelectionDialog(
                    currentBook = state.currentBook,
                    currentChapter = state.currentChapter,
                    currentVerse = state.currentVerse,
                    initialPage = dialog.initialPage,
                    onDismiss = { viewModel.onIntent(BibleIntent.ShowDialog(null)) },
                    onIntent = viewModel::onIntent
                )
            }
            is ActiveDialog.VersionSelection -> {
                VersionSelectionDialog(
                    versions = state.versions,
                    initialSelectedVersions = state.selectedVersions,
                    onDismiss = { viewModel.onIntent(BibleIntent.ShowDialog(null)) },
                    onVersionsSelected = { viewModel.onIntent(BibleIntent.SelectVersions(it)) }
                )
            }
            is ActiveDialog.Settings -> {
                SettingsDialog(
                    displayMode = state.displayMode,
                    showWordsOfJesus = state.showWordsOfJesus,
                    theme = state.theme,
                    isDynamicColor = state.isDynamicColor,
                    supportsDynamicColor = supportsDynamicColor,
                    onDismiss = { viewModel.onIntent(BibleIntent.ShowDialog(null)) },
                    onIntent = viewModel::onIntent
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
            else -> {}
        }

        // Error Dialog
        state.errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(BibleIntent.DismissError) },
                title = { Text("Error") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onIntent(BibleIntent.DismissError) }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
