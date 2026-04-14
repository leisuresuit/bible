package org.tjc.bible.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.arrow_back
import bible.composeapp.generated.resources.back
import bible.composeapp.generated.resources.clear
import bible.composeapp.generated.resources.no_results_found
import bible.composeapp.generated.resources.search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.SearchResult

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: List<SearchResult>,
    isLoading: Boolean = false,
    showTopBar: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSearchQueryChange: (String) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (showTopBar) {
            focusRequester.requestFocus()
        }
    }

    val content = @Composable { padding: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (searchResults.isEmpty() && searchQuery.length >= 3 && !isLoading) {
                Text(
                    text = stringResource(Res.string.no_results_found),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = searchResults.isNotEmpty(),
                    contentPadding = PaddingValues(
                        start = 16.dp + padding.calculateStartPadding(LayoutDirection.Ltr),
                        top = padding.calculateTopPadding(),
                        end = 16.dp + padding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = 16.dp + padding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults) { result ->
                        SearchItem(
                            result = result,
                            searchQuery = searchQuery,
                            onClick = { onResultClick(result) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    title = {
                        val textFieldValue = remember(searchQuery) {
                            TextFieldValue(
                                text = searchQuery,
                                selection = TextRange(searchQuery.length)
                            )
                        }
                        TextField(
                            value = textFieldValue,
                            onValueChange = { onSearchQueryChange(it.text.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text(stringResource(Res.string.search)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                if (isLoading) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.clear),
                                            contentDescription = stringResource(Res.string.clear)
                                        )
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(Res.drawable.arrow_back),
                                contentDescription = stringResource(Res.string.back)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        ) { padding ->
            content(padding)
        }
    } else {
        content(contentPadding)
    }
}

@Composable
private fun SearchItem(
    result: SearchResult,
    searchQuery: String,
    onClick: () -> Unit
) {
    val annotatedText = remember(result.text, searchQuery) {
        buildAnnotatedString {
            val text = result.text
            if (searchQuery.isBlank()) {
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
                    withStyle(SpanStyle(background = Color.Yellow, color = Color.Black, fontWeight = FontWeight.Bold)) {
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
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = "${stringResource(result.book.nameResource)} ${result.chapterNumber}:${result.verseNumber}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}
