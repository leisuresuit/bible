package org.tjc.bible

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.presentation.bible.BibleScreen
import org.tjc.bible.presentation.bible.BibleViewModel
import org.tjc.bible.presentation.ui.BibleTheme

@Composable
fun App() {
    val viewModel: BibleViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    val darkTheme = when (state.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    BibleTheme(darkTheme = darkTheme) {
        BibleScreen(viewModel)
    }
}
