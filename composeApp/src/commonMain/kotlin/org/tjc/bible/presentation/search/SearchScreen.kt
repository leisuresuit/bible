package org.tjc.bible.presentation.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.check
import bible.composeapp.generated.resources.no_results_found
import bible.composeapp.generated.resources.search
import bible.composeapp.generated.resources.sort_by
import bible.composeapp.generated.resources.sort_canonical
import bible.composeapp.generated.resources.sort_relevance
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.SearchSort
import org.tjc.bible.presentation.bible.components.SelectionDialogHeader
import org.tjc.bible.presentation.ui.nameResource

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: List<SearchResult>,
    searchSort: SearchSort = SearchSort.RELEVANCE,
    isSearchSortVisible: Boolean = true,
    isLoading: Boolean = false,
    isSearchingMore: Boolean = false,
    hasMoreResults: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSearchQueryChange: (String) -> Unit,
    onSearchSortChange: (SearchSort) -> Unit = {},
    onToggleSearchSortVisibility: () -> Unit = {},
    onLoadMore: () -> Unit,
    onResultClick: (SearchResult) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val shouldLoadMore = remember(listState, hasMoreResults, isLoading, isSearchingMore, searchResults) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf false

            val lastVisibleItemIndex = visibleItemsInfo.last().index
            val totalItemsCount = layoutInfo.totalItemsCount

            searchResults.isNotEmpty() &&
                    hasMoreResults &&
                    !isLoading &&
                    !isSearchingMore &&
                    lastVisibleItemIndex >= totalItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onLoadMore()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .navigationBarsPadding()
            .imePadding()
    ) {
        SearchHeader(
            searchQuery = searchQuery,
            searchSort = searchSort,
            isSearchSortVisible = isSearchSortVisible,
            onSearchQueryChange = onSearchQueryChange,
            onSearchSortChange = onSearchSortChange,
            onToggleSearchSortVisibility = onToggleSearchSortVisibility
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            if (isLoading && searchResults.isEmpty()) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (searchResults.isEmpty() && searchQuery.length >= 3 && !isLoading) {
                Text(
                    text = stringResource(Res.string.no_results_found),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = searchResults.isNotEmpty(),
                    contentPadding = PaddingValues(
                        start = 16.dp + contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = 16.dp + contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = 16.dp + contentPadding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { it.id }
                    ) { result ->
                        SearchItem(
                            result = result,
                            searchQuery = searchQuery,
                            onClick = { onResultClick(result) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    }

                    if (isSearchingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
private fun SearchHeader(
    searchQuery: String,
    searchSort: SearchSort,
    isSearchSortVisible: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchSortChange: (SearchSort) -> Unit,
    onToggleSearchSortVisibility: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SelectionDialogHeader(
            title = stringResource(Res.string.search),
            searchHint = stringResource(Res.string.search),
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            showSortButton = true,
            onSortClick = onToggleSearchSortVisibility,
            requestFocus = false // Focus managed by SearchScreen's LaunchedEffect
        )
        AnimatedVisibility(
            visible = isSearchSortVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SortOptions(
                currentSort = searchSort,
                onSortChange = onSearchSortChange
            )
        }
    }
}

@Composable
private fun SortOptions(
    currentSort: SearchSort,
    onSortChange: (SearchSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.sort_by),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        FilterChip(
            selected = currentSort == SearchSort.RELEVANCE,
            onClick = { onSortChange(SearchSort.RELEVANCE) },
            label = { Text(stringResource(Res.string.sort_relevance)) },
            leadingIcon = if (currentSort == SearchSort.RELEVANCE) {
                {
                    Icon(
                        painter = painterResource(Res.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )

        FilterChip(
            selected = currentSort == SearchSort.CANONICAL,
            onClick = { onSortChange(SearchSort.CANONICAL) },
            label = { Text(stringResource(Res.string.sort_canonical)) },
            leadingIcon = if (currentSort == SearchSort.CANONICAL) {
                {
                    Icon(
                        painter = painterResource(Res.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )
    }
}

@Composable
private fun SearchItem(
    result: SearchResult,
    searchQuery: String,
    onClick: () -> Unit
) {
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val onHighlightColor = MaterialTheme.colorScheme.onPrimaryContainer

    val annotatedText = remember(result.text, searchQuery, highlightColor, onHighlightColor) {
        buildAnnotatedString {
            val text = result.text
            if (searchQuery.isBlank() || searchQuery.length < 3) {
                append(text)
            } else {
                var start = 0
                while (true) {
                    val index = text.indexOf(searchQuery, start, ignoreCase = true)
                    if (index == -1) {
                        append(text.substring(start))
                        break
                    }
                    append(text.substring(start, index))
                    withStyle(
                        SpanStyle(
                            background = highlightColor,
                            color = onHighlightColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(text.substring(index, index + searchQuery.length))
                    }
                    start = index + searchQuery.length
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = "${stringResource(result.book.nameResource)} ${result.chapterNumber}:${result.verseNumber}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
        )
    }
}
