package org.tjc.bible

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.viewmodel.koinViewModel
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.presentation.bible.BibleIntent
import org.tjc.bible.presentation.bible.BibleScreen
import org.tjc.bible.presentation.bible.BibleViewModel
import org.tjc.bible.presentation.search.SearchScreen
import org.tjc.bible.presentation.ui.Bible
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.Search
import org.tjc.bible.presentation.ui.navConfig

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

                is Search -> NavEntry(route) {
                    SearchScreen(
                        searchQuery = state.searchQuery,
                        searchResults = state.searchResults,
                        searchSort = state.searchSort,
                        isSearchSortVisible = state.isSearchSortVisible,
                        isLoading = state.isLoading,
                        isSearchingMore = state.isSearchingMore,
                        hasMoreResults = state.hasMoreSearchResults,
                        showTopBar = true,
                        onSearchQueryChange = {
                            viewModel.onIntent(
                                BibleIntent.UpdateSearchQuery(it)
                            )
                        },
                        onSearchSortChange = {
                            viewModel.onIntent(
                                BibleIntent.UpdateSearchSort(it)
                            )
                        },
                        onToggleSearchSortVisibility = {
                            viewModel.onIntent(
                                BibleIntent.ToggleSearchSortVisibility
                            )
                        },
                        onLoadMore = {
                            viewModel.onIntent(
                                BibleIntent.LoadMoreSearchResults
                            )
                        },
                        onResultClick = { result ->
                            viewModel.onIntent(
                                BibleIntent.SelectPassage(
                                    result.book,
                                    result.chapterNumber,
                                    result.verseNumber
                                )
                            )
                            backStack.removeLastOrNull()
                        },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                else -> NavEntry(route) { Text("Unknown route: $route") }
            }
        }
    }
}
