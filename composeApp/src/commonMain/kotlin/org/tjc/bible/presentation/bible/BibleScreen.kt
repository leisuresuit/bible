package org.tjc.bible.presentation.bible

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.tjc.bible.presentation.bible.components.BibleTopBar
import org.tjc.bible.presentation.bible.components.HistoryDialog
import org.tjc.bible.presentation.bible.components.PassageSelectionDialog
import org.tjc.bible.presentation.bible.components.SettingsDialog
import org.tjc.bible.presentation.bible.components.VerseList
import org.tjc.bible.presentation.bible.components.VersionSelectionDialog
import org.tjc.bible.presentation.ui.supportsDynamicColor

@Composable
fun BibleScreen(viewModel: BibleViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onIntent(BibleIntent.LoadInitialData)
    }

    Scaffold(
        topBar = {
            BibleTopBar(state, viewModel::onIntent)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                VerseList(state, viewModel::onIntent)
            }
        }

        // Dialogs
        when (val dialog = state.activeDialog) {
            is ActiveDialog.PassageSelection -> {
                PassageSelectionDialog(
                    state = state,
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
                    state = state,
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
    }
}
