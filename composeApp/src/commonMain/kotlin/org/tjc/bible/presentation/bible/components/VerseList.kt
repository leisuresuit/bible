package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.presentation.bible.ActiveDialog
import org.tjc.bible.presentation.bible.BibleIntent
import org.tjc.bible.presentation.bible.BibleState
import org.tjc.bible.presentation.bible.DisplayMode
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@Composable
fun VerseList(
    state: BibleState,
    onIntent: (BibleIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.currentBook == null) {
        return
    }

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

    if (state.displayMode == DisplayMode.SINGLE_CHAPTER) {
        val initialPage = remember { getGlobalIndex(state.currentBook, state.currentChapter) }
        val pagerState = rememberPagerState(initialPage = initialPage) { totalChapters }

        // Sync pager with state changes (e.g. from dialog selection)
        LaunchedEffect(state.currentBook, state.currentChapter) {
            val targetPage = getGlobalIndex(state.currentBook, state.currentChapter)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }

        // Sync state with pager swipes
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    val (book, chapter) = getPassageFromGlobalIndex(page)
                    if (book != state.currentBook || chapter != state.currentChapter) {
                        onIntent(BibleIntent.UpdateVisiblePassage(book, chapter))
                    }
                }
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val (book, chapter) = getPassageFromGlobalIndex(page)
            val verses = state.chaptersVerses[page]
            
            LaunchedEffect(book, chapter) {
                onIntent(BibleIntent.LoadChapterVerses(book, chapter, page))
            }

            if (verses != null) {
                VerseListContent(
                    book = book,
                    chapter = chapter,
                    verses = verses,
                    targetVerse = if (page == pagerState.currentPage) state.currentVerse else null,
                    onIntent = onIntent
                )
            } else {
                VerseListPlaceholder(book, chapter, onIntent)
            }
        }
    } else {
        // Contiguous mode
        VerseListContent(
            book = state.currentBook,
            chapter = state.currentChapter,
            verses = state.verses,
            targetVerse = state.currentVerse,
            onIntent = onIntent,
            modifier = modifier
        )
    }
}

@Composable
private fun VerseListContent(
    book: Book,
    chapter: Int,
    verses: List<Verse>,
    targetVerse: Int?,
    onIntent: (BibleIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(book, chapter, targetVerse, verses) {
        if (targetVerse != null && verses.isNotEmpty()) {
            val index = verses.indexOfFirst { it.number == targetVerse }
            if (index != -1) {
                lazyListState.scrollToItem(index + 1)
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ChapterHeader(book, chapter) {
                onIntent(BibleIntent.ShowDialog(ActiveDialog.PassageSelection(0)))
            }
        }
        
        items(verses, key = { "${it.number}_${it.versionAbbreviation.orEmpty()}" }) { verse ->
            VerseItem(verse)
        }
    }
}

@Composable
private fun ChapterHeader(book: Book, chapter: Int, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "${stringResource(book.nameResource)} $chapter",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VerseItem(verse: Verse) {
    Column {
        if (verse.versionAbbreviation != null) {
            Text(
                text = verse.versionAbbreviation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Row {
            Text(
                text = verse.number.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(end = 12.dp, top = 2.dp)
            )
            Text(
                text = verse.text,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
            )
        }
    }
}

@Composable
private fun VerseListPlaceholder(book: Book, chapter: Int, onIntent: (BibleIntent) -> Unit) {
    Box(Modifier.fillMaxSize().padding(top = 16.dp)) {
        ChapterHeader(book, chapter) {
            onIntent(BibleIntent.ShowDialog(ActiveDialog.PassageSelection(0)))
        }
    }
}

@ThemePreviews
@Composable
fun VerseListPreview() {
    BibleTheme {
        Surface {
            VerseList(
                state = BibleState(
                    currentBook = Book.Luke,
                    currentChapter = 18,
                    verses = listOf(
                        Verse(1, "Then He spoke a parable to them, that men always ought to pray and not lose heart,"),
                        Verse(2, "saying: \"There was in a certain city a judge who did not fear God nor regard man.")
                    )
                ),
                onIntent = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun VerseListParallelPreview() {
    BibleTheme {
        Surface {
            VerseList(
                state = BibleState(
                    currentBook = Book.Luke,
                    currentChapter = 24,
                    verses = listOf(
                        Verse(1, "Now on the first day of the week, very early in the morning, they, and certain other women with them, came to the tomb bringing the spices which they had prepared.", versionAbbreviation = "NKJV"),
                        Verse(1, "七日的頭一日黎明的時候，那些婦女帶著所預備的香料，來到墳墓前。", versionAbbreviation = "CUV"),
                        Verse(2, "But they found the stone rolled away from the tomb.", versionAbbreviation = "NKJV"),
                        Verse(2, "看見石頭已經從墳墓輥開了；", versionAbbreviation = "CUV"),
                        Verse(3, "Then they went in and did not find the body of the Lord Jesus.", versionAbbreviation = "NKJV"),
                        Verse(3, "他們就進去，只是不見主耶穌的身體。", versionAbbreviation = "CUV")
                    )
                ),
                onIntent = {}
            )
        }
    }
}
