package org.tjc.bible

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.presentation.bible.BibleIntent
import org.tjc.bible.presentation.bible.BibleScreen
import org.tjc.bible.presentation.bible.BibleViewModel
import org.tjc.bible.presentation.search.SearchScreen
import org.tjc.bible.presentation.ui.BibleTheme

@Composable
fun App() {
    val viewModel: BibleViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    
    val backStack = rememberNavBackStack(navConfig, NavRoute.Bible)

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
            transitionSpec = {
                val enter = fadeIn(animationSpec = tween(300))
                val exit = fadeOut(animationSpec = tween(300))
                enter togetherWith exit
            }
        ) { key ->
            when (key as NavRoute) {
                NavRoute.Bible -> NavEntry(
                    key = key,
                    content = {
                        BibleScreen(
                            viewModel = viewModel,
                            onNavigateToSearch = {
                                backStack.add(NavRoute.Search)
                            }
                        )
                    }
                )
                NavRoute.Search -> NavEntry(
                    key = key,
                    content = {
                        SearchScreen(
                            searchQuery = state.searchQuery,
                            searchResults = state.searchResults,
                            isLoading = state.isLoading,
                            onSearchQueryChange = { viewModel.onIntent(BibleIntent.UpdateSearchQuery(it)) },
                            onResultClick = { result ->
                                viewModel.onIntent(BibleIntent.SelectPassage(result.book, result.chapterNumber, result.verseNumber))
                                if (backStack.size > 1) {
                                    // removeLast() is only on target API 35+
                                    backStack.removeAt(backStack.size - 1)
                                }
                            },
                            onBack = {
                                if (backStack.size > 1) {
                                    // removeLast() is only on target API 35+
                                    backStack.removeAt(backStack.size - 1)
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}
