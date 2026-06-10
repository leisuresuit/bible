package org.tjc.bible

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.close
import bible.composeapp.generated.resources.history
import bible.composeapp.generated.resources.search
import bible.composeapp.generated.resources.settings
import bible.composeapp.generated.resources.versions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.presentation.bible.*
import org.tjc.bible.presentation.bible.components.HistoryScreen
import org.tjc.bible.presentation.bible.components.PassageSelectionScreen
import org.tjc.bible.presentation.bible.components.SettingsScreen
import org.tjc.bible.presentation.bible.components.VersionSelectionScreen
import org.tjc.bible.presentation.search.SearchScreen
import org.tjc.bible.presentation.ui.Bible
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.nameResource
import org.tjc.bible.presentation.ui.navConfig
import org.tjc.bible.presentation.ui.supportsDynamicColor

val LocalShowTopBar = compositionLocalOf { true }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App(windowSizeClass: WindowSizeClass) {
    val viewModel: BibleViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    val backStack = rememberNavBackStack(navConfig, Bible)

    val darkTheme = when (state.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val useSidePanel by remember(windowSizeClass) {
        derivedStateOf {
            windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact ||
                    windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
        }
    }

    BibleTheme(
        darkTheme = darkTheme,
        dynamicColor = state.isDynamicColor
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val isCtrlOrCmd = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                            when {
                                isCtrlOrCmd && keyEvent.key == Key.F -> {
                                    viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.Search))
                                    true
                                }
                                isCtrlOrCmd && keyEvent.key == Key.H -> {
                                    viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.History))
                                    true
                                }
                                isCtrlOrCmd && keyEvent.key == Key.Comma -> {
                                    viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.Settings))
                                    true
                                }
                                isCtrlOrCmd && keyEvent.key == Key.V -> {
                                    viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.VersionSelection))
                                    true
                                }
                                keyEvent.key == Key.Escape -> {
                                    viewModel.onIntent(BibleIntent.ShowSheet(null))
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
            ) {
                if (useSidePanel) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Spacer(Modifier.statusBarsPadding())
                        CurrentBookHeader(state.currentBook, state.currentChapter) {
                            viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.PassageSelection(0)))
                        }

                        VersionButton(state.selectedVersions) {
                            viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.VersionSelection))
                        }

                        NavigationRailItem(
                            selected = state.activeSheet is ActiveSheet.Search,
                            onClick = { viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.Search)) },
                            icon = { Icon(painterResource(Res.drawable.search), contentDescription = stringResource(Res.string.search)) }
                        )
                        NavigationRailItem(
                            selected = state.activeSheet is ActiveSheet.History,
                            onClick = { viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.History)) },
                            icon = { Icon(painterResource(Res.drawable.history), contentDescription = stringResource(Res.string.history)) }
                        )
                        NavigationRailItem(
                            selected = state.activeSheet is ActiveSheet.Settings,
                            onClick = { viewModel.onIntent(BibleIntent.ToggleSheet(ActiveSheet.Settings)) },
                            icon = { Icon(painterResource(Res.drawable.settings), contentDescription = stringResource(Res.string.settings)) }
                        )
                    }
                    VerticalDivider()

                    AnimatedContent(
                        targetState = state.activeSheet,
                        transitionSpec = {
                            if (initialState == null || targetState == null) {
                                (fadeIn() + expandHorizontally(expandFrom = Alignment.Start)) togetherWith
                                        (fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start))
                            } else {
                                EnterTransition.None togetherWith ExitTransition.None
                            }
                        },
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxHeight()
                    ) { activeSheet ->
                        if (activeSheet != null) {
                            Row(modifier = Modifier.fillMaxHeight()) {
                                Surface(
                                    modifier = Modifier
                                        .width(400.dp)
                                        .fillMaxHeight(),
                                    tonalElevation = 1.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .statusBarsPadding()
                                    ) {
                                        SheetContent(
                                            activeSheet,
                                            state,
                                            viewModel,
                                            isSidePanel = true,
                                            Modifier.fillMaxSize().padding(top = 48.dp)
                                        )

                                        IconButton(
                                            onClick = { viewModel.onIntent(BibleIntent.ShowSheet(null)) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .zIndex(99f)
                                        ) {
                                            Icon(
                                                painterResource(Res.drawable.close),
                                                contentDescription = "Close"
                                            )
                                        }
                                    }
                                }
                                VerticalDivider()
                            }
                        }
                    }
                }

                CompositionLocalProvider(LocalShowTopBar provides !useSidePanel) {
                    NavDisplay(
                        backStack = backStack,
                        modifier = Modifier.weight(1f)
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
                }
            }
        }

        if (!useSidePanel && state.activeSheet != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onIntent(BibleIntent.ShowSheet(null)) },
                sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                )
            ) {
                SheetContent(state.activeSheet!!, state, viewModel, isSidePanel = false, Modifier.fillMaxHeight(0.8f))
            }
        }
    }
}

@Composable
private fun CurrentBookHeader(book: Book?, chapter: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(
            text = book?.let { stringResource(it.nameResource) } ?: "",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = chapter.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VersionButton(selectedVersions: List<BibleVersion>, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        val versionText = if (selectedVersions.size > 1) {
            stringResource(Res.string.versions)
        } else {
            selectedVersions.firstOrNull()?.abbreviation ?: stringResource(Res.string.versions)
        }
        Text(
            text = versionText,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SheetContent(
    sheet: ActiveSheet,
    state: BibleState,
    viewModel: BibleViewModel,
    isSidePanel: Boolean,
    modifier: Modifier = Modifier
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
                    if (!isSidePanel) {
                        viewModel.onIntent(BibleIntent.ShowSheet(null))
                    }
                },
                modifier = modifier,
                isSidePanel = isSidePanel
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
                modifier = modifier
            )
        }

        is ActiveSheet.VersionSelection -> {
            VersionSelectionScreen(
                versions = state.versions,
                selectedVersions = state.selectedVersions,
                isLanguageFilterVisible = state.isVersionLanguageFilterVisible,
                selectedLanguages = state.selectedLanguages,
                onVersionToggle = { viewModel.onIntent(BibleIntent.ToggleParallelVersion(it)) },
                onToggleLanguageFilterVisibility = { viewModel.onIntent(BibleIntent.ToggleVersionLanguageFilterVisibility) },
                onSelectedLanguagesChange = { viewModel.onIntent(BibleIntent.UpdateSelectedLanguages(it)) },
                modifier = modifier
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
                    if (!isSidePanel) {
                        viewModel.onIntent(BibleIntent.ShowSheet(null))
                    }
                },
                onClear = { viewModel.onIntent(BibleIntent.ClearHistory) },
                modifier = modifier,
                isSidePanel = isSidePanel
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
                modifier = modifier,
                isSidePanel = isSidePanel
            )
        }
    }
}
