package org.tjc.bible

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.viewmodel.koinViewModel
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.presentation.bible.ActiveSheet
import org.tjc.bible.presentation.bible.BibleIntent
import org.tjc.bible.presentation.bible.BibleScreen
import org.tjc.bible.presentation.bible.BibleViewModel
import org.tjc.bible.presentation.bible.components.HistoryScreen
import org.tjc.bible.presentation.bible.components.PassageSelectionScreen
import org.tjc.bible.presentation.bible.components.SettingsScreen
import org.tjc.bible.presentation.bible.components.VersionSelectionScreen
import org.tjc.bible.presentation.search.SearchScreen
import org.tjc.bible.presentation.ui.Bible
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.navConfig
import org.tjc.bible.presentation.ui.supportsDynamicColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val viewModel: BibleViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    val backStack = rememberNavBackStack(navConfig, Bible)

    val darkTheme = when (state.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    BibleTheme(
        darkTheme = darkTheme,
        dynamicColor = state.isDynamicColor
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize()
        ) { route ->
            when (route) {
                is Bible -> NavEntry(route) {
                    BibleScreen(
                        viewModel = viewModel,
                        onNavigate = { backStack.add(it) }
                    )
                }

                else -> NavEntry(route) { Text("Unknown route: $route") }
            }
        }

        state.activeSheet?.let { sheet ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.onIntent(BibleIntent.ShowSheet(null)) },
                sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                )
            ) {
                when (sheet) {
                    is ActiveSheet.Search -> {
                        SearchScreen(
                            searchQuery = state.searchQuery,
                            searchResults = state.searchResults,
                            searchSort = state.searchSort,
                            isSearchSortVisible = state.isSearchSortVisible,
                            isLoading = state.isLoading,
                            isSearchingMore = state.isSearchingMore,
                            hasMoreResults = state.hasMoreSearchResults,
                            showTopBar = true,
                            onSearchQueryChange = { viewModel.onIntent(BibleIntent.UpdateSearchQuery(it)) },
                            onSearchSortChange = { viewModel.onIntent(BibleIntent.UpdateSearchSort(it)) },
                            onToggleSearchSortVisibility = { viewModel.onIntent(BibleIntent.ToggleSearchSortVisibility) },
                            onLoadMore = { viewModel.onIntent(BibleIntent.LoadMoreSearchResults) },
                            onResultClick = { result ->
                                viewModel.onIntent(
                                    BibleIntent.SelectPassage(
                                        result.book,
                                        result.chapterNumber,
                                        result.verseNumber
                                    )
                                )
                                viewModel.onIntent(BibleIntent.ShowSheet(null))
                            },
                            onBack = { viewModel.onIntent(BibleIntent.ShowSheet(null)) }
                        )
                    }

                    is ActiveSheet.PassageSelection -> {
                        PassageSelectionScreen(
                            currentBook = state.currentBook,
                            currentChapter = state.currentChapter,
                            currentVerse = state.currentVerse,
                            initialPage = sheet.initialPage,
                            onPassageSelected = { book, chapter, verse ->
                                viewModel.onIntent(BibleIntent.SelectPassage(book, chapter, verse))
                                viewModel.onIntent(BibleIntent.ShowSheet(null))
                            },
                            onDismiss = { viewModel.onIntent(BibleIntent.ShowSheet(null)) }
                        )
                    }

                    is ActiveSheet.VersionSelection -> {
                        VersionSelectionScreen(
                            versions = state.versions,
                            selectedVersions = state.selectedVersions,
                            onVersionToggle = { viewModel.onIntent(BibleIntent.ToggleParallelVersion(it)) },
                            onDismiss = { viewModel.onIntent(BibleIntent.ShowSheet(null)) }
                        )
                    }

                    is ActiveSheet.History -> {
                        HistoryScreen(
                            history = state.history,
                            currentBook = state.currentBook,
                            currentChapter = state.currentChapter,
                            currentVerse = state.currentVerse,
                            onItemClick = {
                                viewModel.onIntent(BibleIntent.NavigateToHistoryItem(it))
                                viewModel.onIntent(BibleIntent.ShowSheet(null))
                            },
                            onClear = { viewModel.onIntent(BibleIntent.ClearHistory) }
                        )
                    }

                    is ActiveSheet.Settings -> {
                        SettingsScreen(
                            displayMode = state.displayMode,
                            showWordsOfJesus = state.showWordsOfJesus,
                            theme = state.theme,
                            isDynamicColor = state.isDynamicColor,
                            supportsDynamicColor = supportsDynamicColor,
                            onDisplayModeChange = { viewModel.onIntent(BibleIntent.UpdateDisplayMode(it)) },
                            onShowWordsOfJesusChange = { viewModel.onIntent(BibleIntent.UpdateShowWordsOfJesus(it)) },
                            onThemeChange = { viewModel.onThemeChange(it) },
                            onDynamicColorChange = { viewModel.onIntent(BibleIntent.UpdateDynamicColor(it)) },
                            onDismiss = { viewModel.onIntent(BibleIntent.ShowSheet(null)) }
                        )
                    }
                }
            }
        }
    }
}
