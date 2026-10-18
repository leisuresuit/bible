package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.Verse

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SingleChapterVerseList(
    currentBook: Book,
    currentChapter: Int,
    currentVerse: Int?,
    chaptersVerses: Map<Int, List<Verse>>,
    showWordsOfJesus: Boolean,
    selectionEventId: Long,
    onShowPassageSelection: (initialPage: Int) -> Unit,
    onUpdateVisiblePassage: (Book, Int, Int) -> Unit,
    onLoadChapterVerses: (Book, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val totalChapters = remember { Book.entries.sumOf { it.chaptersCount } }
    val cumulativeChapters = remember {
        var sum = 0
        Book.entries.map {
            val start = sum
            sum += it.chaptersCount
            start
        }
    }

    fun getGlobalIndex(book: Book, chapter: Int): Int {
        val bookIndex = Book.entries.indexOf(book)
        return if (bookIndex != -1) cumulativeChapters[bookIndex] + (chapter - 1) else 0
    }

    fun getPassageFromGlobalIndex(globalIndex: Int): Pair<Book, Int> {
        val bookIndex = cumulativeChapters.indexOfLast { it <= globalIndex }.coerceAtLeast(0)
        val book = Book.entries[bookIndex]
        val chapter = globalIndex - cumulativeChapters[bookIndex] + 1
        return book to chapter
    }

    val initialPage = remember { getGlobalIndex(currentBook, currentChapter) }
    val pagerState = rememberPagerState(initialPage = initialPage) { totalChapters }

    LaunchedEffect(currentBook, currentChapter) {
        val targetPage = getGlobalIndex(currentBook, currentChapter)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState, currentBook, currentChapter) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val (book, chapter) = getPassageFromGlobalIndex(page)
                if (book != currentBook || chapter != currentChapter) {
                    onUpdateVisiblePassage(book, chapter, 1)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1
    ) { page ->
        val (book, chapter) = getPassageFromGlobalIndex(page)
        val chapterVerses = chaptersVerses[page]

        LaunchedEffect(book, chapter) {
            onLoadChapterVerses(book, chapter, page)
        }

        if (chapterVerses != null) {
            VerseListContent(
                book = book,
                chapter = chapter,
                verses = chapterVerses,
                targetVerse = if (page == pagerState.currentPage) currentVerse else null,
                showWordsOfJesus = showWordsOfJesus,
                selectionEventId = selectionEventId,
                onShowPassageSelection = onShowPassageSelection,
                onVerseVisible = { verse ->
                    if (page == pagerState.currentPage) {
                        onUpdateVisiblePassage(book, chapter, verse)
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp),
                nestedScrollConnection = nestedScrollConnection,
                contentPadding = contentPadding
            )
        } else {
            VerseListPlaceholder(book, chapter, onShowPassageSelection)
        }
    }
}

@Composable
private fun VerseListContent(
    book: Book,
    chapter: Int,
    verses: List<Verse>,
    targetVerse: Int?,
    showWordsOfJesus: Boolean,
    selectionEventId: Long,
    onShowPassageSelection: (initialPage: Int) -> Unit,
    onVerseVisible: (Int) -> Unit,
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val lazyListState = rememberLazyListState()
    var lastProcessedEventId by rememberSaveable { mutableStateOf(-1L) }

    LaunchedEffect(selectionEventId, verses) {
        if (selectionEventId > lastProcessedEventId && verses.isNotEmpty()) {
            val verseToScroll = targetVerse ?: 1
            val index = verses.indexOfFirst { it.number == verseToScroll }
            if (index != -1) {
                val itemIndex = if (verseToScroll == 1) 0 else index + 1
                lazyListState.scrollToItem(itemIndex)
                lastProcessedEventId = selectionEventId
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index > 0 && index <= verses.size) {
                    onVerseVisible(verses[index - 1].number)
                } else if (index == 0) {
                    onVerseVisible(1)
                }
            }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxColumnWidth = if (maxWidth > 600.dp) 600.dp else maxWidth

        SelectionContainer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = maxColumnWidth)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier),
                contentPadding = PaddingValues(
                    start = 16.dp + contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                    top = contentPadding.calculateTopPadding(),
                    end = 16.dp + contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = 256.dp + contentPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ChapterHeader(book, chapter) {
                        onShowPassageSelection(0)
                    }
                }

                items(verses, key = { "${it.number}_${it.versionAbbreviation.orEmpty()}" }) { verse ->
                    VerseItem(verse, showWordsOfJesus)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VerseListPlaceholder(book: Book, chapter: Int, onShowPassageSelection: (initialPage: Int) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ChapterHeader(
            book = book,
            chapter = chapter,
            onClick = {
                onShowPassageSelection(0)
            }
        )
        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
