package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.back
import bible.composeapp.generated.resources.book
import bible.composeapp.generated.resources.cancel
import bible.composeapp.generated.resources.chapter
import bible.composeapp.generated.resources.ok
import bible.composeapp.generated.resources.search
import bible.composeapp.generated.resources.verse
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.Book
import org.tjc.bible.presentation.ui.AutoResizedText
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@Composable
fun PassageSelectionDialog(
    currentBook: Book?,
    currentChapter: Int,
    currentVerse: Int,
    initialPage: Int,
    onPassageSelected: (Book, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { 3 }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isAlphabeticalOrder by remember { mutableStateOf(false) }

    var selectedBook by remember { mutableStateOf(currentBook) }
    var selectedChapter by remember { mutableStateOf(currentChapter) }
    var selectedVerse by remember { mutableStateOf(currentVerse) }

    // Reset search query when page changes
    LaunchedEffect(pagerState.currentPage) {
        searchQuery = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    selectedBook?.let { book ->
                        onPassageSelected(book, selectedChapter, selectedVerse)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.back))
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Stationary Header
                val title = when (pagerState.currentPage) {
                    0 -> stringResource(Res.string.book)
                    1 -> selectedBook?.let { stringResource(it.nameResource) } ?: stringResource(Res.string.chapter)
                    else -> {
                        val bookName = selectedBook?.let { stringResource(it.nameResource) } ?: ""
                        if (bookName.isNotEmpty()) "$bookName $selectedChapter" else stringResource(Res.string.verse)
                    }
                }
                
                val keyboardType = if (pagerState.currentPage == 0) KeyboardType.Text else KeyboardType.Number
                
                val searchHint = when (pagerState.currentPage) {
                    0 -> stringResource(Res.string.search)
                    1 -> stringResource(Res.string.chapter)
                    else -> stringResource(Res.string.verse)
                }
                
                SelectionDialogHeader(
                    title = title,
                    searchHint = searchHint,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    showSortButton = pagerState.currentPage == 0,
                    onSortClick = { isAlphabeticalOrder = !isAlphabeticalOrder },
                    keyboardType = keyboardType,
                    titleWeight = if (pagerState.currentPage == 0) null else 1.5f,
                    searchWeight = if (pagerState.currentPage == 0) null else 1f
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.heightIn(max = 450.dp)
                ) { page ->
                    when (page) {
                        0 -> {
                            val booksWithNames = Book.entries.map { it to stringResource(it.nameResource) }
                            val sortedBooksWithNames = if (isAlphabeticalOrder) {
                                booksWithNames.sortedBy { it.second }
                            } else {
                                booksWithNames
                            }
                            
                            BookSelectionPage(
                                booksWithNames = sortedBooksWithNames,
                                selectedBook = selectedBook,
                                searchQuery = searchQuery,
                                onBookSelected = { book ->
                                    if (selectedBook != book) {
                                        selectedBook = book
                                        selectedChapter = 1
                                        selectedVerse = 1
                                    }
                                    if (book.chaptersCount == 1) {
                                        scope.launch { pagerState.animateScrollToPage(2) }
                                    } else {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    }
                                }
                            )
                        }
                        1 -> ChapterSelectionPage(
                            selectedBook = selectedBook,
                            chaptersCount = selectedBook?.chaptersCount ?: 0,
                            selectedChapter = selectedChapter,
                            searchQuery = searchQuery,
                            onChapterSelected = { chapter ->
                                if (selectedChapter != chapter) {
                                    selectedChapter = chapter
                                    selectedVerse = 1
                                }
                                scope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )
                        2 -> {
                            val versesCount = selectedBook?.versesInChapters?.getOrNull(selectedChapter - 1) ?: 0
                            VerseSelectionPage(
                                selectedBook = selectedBook,
                                selectedChapter = selectedChapter,
                                versesCount = versesCount,
                                selectedVerse = selectedVerse,
                                searchQuery = searchQuery,
                                onVerseSelected = { verse ->
                                    selectedVerse = verse
                                    selectedBook?.let { book ->
                                        onPassageSelected(book, selectedChapter, selectedVerse)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun BookSelectionPage(
    booksWithNames: List<Pair<Book, String>>,
    selectedBook: Book?,
    searchQuery: String,
    onBookSelected: (Book) -> Unit
) {
    val filteredBooks = remember(booksWithNames, searchQuery) {
        booksWithNames.filter { it.second.contains(searchQuery, ignoreCase = true) }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(selectedBook) {
        val index = filteredBooks.indexOfFirst { it.first == selectedBook }
        if (index != -1) {
            listState.scrollToItem(index)
        }
    }

    LazyColumn(
        modifier = Modifier.height(350.dp),
        state = listState
    ) {
        items(filteredBooks) { (book, name) ->
            val isSelected = book == selectedBook
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onBookSelected(book) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AutoResizedText(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ChapterSelectionPage(
    selectedBook: Book?,
    chaptersCount: Int,
    selectedChapter: Int,
    searchQuery: String,
    onChapterSelected: (Int) -> Unit
) {
    val chapters = (1..chaptersCount).toList()
    val filteredChapters = remember(chaptersCount, searchQuery) {
        if (searchQuery.isEmpty()) chapters
        else chapters.filter { it.toString().contains(searchQuery) }
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(selectedBook, selectedChapter) {
        val index = filteredChapters.indexOf(selectedChapter)
        if (index != -1) {
            gridState.scrollToItem(index)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.height(350.dp),
        state = gridState
    ) {
        items(filteredChapters.size) { index ->
            val chapter = filteredChapters[index]
            val isSelected = chapter == selectedChapter
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable { onChapterSelected(chapter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chapter.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun VerseSelectionPage(
    selectedBook: Book?,
    selectedChapter: Int,
    versesCount: Int,
    selectedVerse: Int?,
    searchQuery: String,
    onVerseSelected: (Int) -> Unit
) {
    val verses = (1..versesCount).toList()
    val filteredVerses = remember(versesCount, searchQuery) {
        if (searchQuery.isEmpty()) verses
        else verses.filter { it.toString().contains(searchQuery) }
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(selectedBook, selectedChapter, selectedVerse) {
        val index = filteredVerses.indexOf(selectedVerse)
        if (index != -1) {
            gridState.scrollToItem(index)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.height(350.dp),
        state = gridState
    ) {
        items(filteredVerses.size) { index ->
            val verse = filteredVerses[index]
            val isSelected = verse == selectedVerse
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable { onVerseSelected(verse) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = verse.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


@ThemePreviews
@Composable
fun PassageSelectionDialogPreview() {
    BibleTheme {
        PassageSelectionDialog(
            currentBook = Book.Luke,
            currentChapter = 18,
            currentVerse = 1,
            initialPage = 0,
            onDismiss = {},
            onPassageSelected = { _, _, _ -> }
        )
    }
}
