package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.TextSpan
import org.tjc.bible.domain.model.TextStyle
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.model.VerseElement
import org.tjc.bible.presentation.bible.DisplayMode
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerseList(
    currentBook: Book?,
    currentChapter: Int,
    currentVerse: Int?,
    verses: List<Verse>,
    chaptersVerses: Map<Int, List<Verse>>,
    displayMode: DisplayMode,
    showWordsOfJesus: Boolean,
    selectionEventId: Long,
    isLoading: Boolean,
    onShowPassageSelection: (initialPage: Int) -> Unit,
    onUpdateVisiblePassage: (Book, Int, Int) -> Unit,
    onLoadChapterVerses: (Book, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (currentBook == null) {
        if (isLoading) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
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

    if (displayMode == DisplayMode.SINGLE_CHAPTER) {
        val initialPage = remember { getGlobalIndex(currentBook, currentChapter) }
        val pagerState = rememberPagerState(initialPage = initialPage) { totalChapters }

        // Sync pager with state changes (e.g. from dialog selection)
        LaunchedEffect(currentBook, currentChapter) {
            val targetPage = getGlobalIndex(currentBook, currentChapter)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }

        // Sync state with pager swipes
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
    } else {
        // Contiguous mode
        VerseListContent(
            book = currentBook,
            chapter = currentChapter,
            verses = verses,
            targetVerse = currentVerse,
            showWordsOfJesus = showWordsOfJesus,
            selectionEventId = selectionEventId,
            onShowPassageSelection = onShowPassageSelection,
            onVerseVisible = { onUpdateVisiblePassage(currentBook, currentChapter, it) },
            modifier = modifier,
            nestedScrollConnection = nestedScrollConnection,
            contentPadding = contentPadding
        )
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
                // index 0 in LazyColumn is the ChapterHeader. 
                // We scroll to 0 for the first verse to show the header.
                // For other verses, we scroll to index + 1.
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
        
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = maxColumnWidth)
                .fillMaxSize()
                .then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier),
            contentPadding = PaddingValues(
                start = 16.dp + contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                top = contentPadding.calculateTopPadding(),
                end = 16.dp + contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                bottom = 16.dp + contentPadding.calculateBottomPadding()
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

@Composable
private fun ChapterHeader(book: Book, chapter: Int, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "${stringResource(book.nameResource)} $chapter",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Serif
        )
    }
}

@Composable
private fun VerseItem(verse: Verse, showWordsOfJesus: Boolean) {
    Column {
        if (verse.versionAbbreviation != null) {
            Text(
                text = verse.versionAbbreviation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        verse.elements.forEachIndexed { index, element ->
            when (element) {
                is VerseElement.Heading -> {
                    val headingText = buildAnnotatedString {
                        element.spans.forEach { span ->
                            val text = if (span.style == TextStyle.SMALL_CAPS) span.text.uppercase() else span.text
                            withStyle(span.style.toSpanStyle(SpanStyle(), showWordsOfJesus)) {
                                append(text)
                            }
                        }
                    }
                    Text(
                        text = headingText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            lineHeight = 24.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        // For the first heading (above the verse), set the top margin to 4.dp
                        // so that the gap between it and verse before it is
                        // 12.dp (verticalArrangement) + 4.dp = 16.dp
                        modifier = Modifier.padding(top = if (index == 0) 4.dp else 16.dp, bottom = 6.dp)
                    )
                }
                is VerseElement.Text -> {
                    val annotatedString = buildAnnotatedString {
                        // Only show verse number on the first text element of the verse
                        if (element == verse.elements.firstOrNull { it is VerseElement.Text }) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("${verse.number} ")
                            }
                        }

                        val baseStyle = SpanStyle(fontFamily = FontFamily.Serif)
                        element.spans.forEach { span ->
                            val text = if (span.style == TextStyle.SMALL_CAPS) span.text.uppercase() else span.text
                            withStyle(span.style.toSpanStyle(baseStyle, showWordsOfJesus)) {
                                append(text)
                            }
                        }
                    }
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun TextStyle.toSpanStyle(baseStyle: SpanStyle, showWordsOfJesus: Boolean) =
    when (this) {
        TextStyle.BOLD -> baseStyle.copy(
            fontWeight = FontWeight.Bold
        )

        TextStyle.ITALIC -> baseStyle.copy(
            fontStyle = FontStyle.Italic
        )

        TextStyle.ITALIC_BOLD -> baseStyle.copy(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic
        )

        TextStyle.HEADING -> baseStyle.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        TextStyle.SMALL_CAPS -> baseStyle

        TextStyle.WORDS_OF_JESUS -> if (showWordsOfJesus) {
            baseStyle.copy(color = Color.Red)
        } else {
            baseStyle
        }

        TextStyle.NORMAL -> baseStyle
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
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

@ThemePreviews
@Composable
fun VerseListPreview() {
    BibleTheme {
        Surface {
            VerseList(
                currentBook = Book.Luke,
                currentChapter = 18,
                currentVerse = null,
                verses = listOf(
                    Verse(
                        number = 1, 
                        elements = listOf(
                            VerseElement.Heading(listOf(TextSpan("The Parable of the Persistent Widow", TextStyle.HEADING))),
                            VerseElement.Text(listOf(TextSpan("Then He spoke a parable to them, that men always ought to pray and not lose heart,", TextStyle.NORMAL)))
                        )
                    ),
                    Verse(
                        number = 2,
                        elements = listOf(
                            VerseElement.Text(listOf(TextSpan("saying: \"There was in a certain city a judge who did not fear God nor regard man.", TextStyle.NORMAL)))
                        )
                    )
                ),
                chaptersVerses = emptyMap(),
                displayMode = DisplayMode.CONTIGUOUS,
                showWordsOfJesus = true,
                selectionEventId = 0L,
                isLoading = false,
                onShowPassageSelection = {},
                onUpdateVisiblePassage = { _, _, _ -> },
                onLoadChapterVerses = { _, _, _ -> }
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
                currentBook = Book.Luke,
                currentChapter = 24,
                currentVerse = null,
                verses = listOf(
                    Verse(1, elements = listOf(VerseElement.Text(listOf(TextSpan("Now on the first day of the week, very early in the morning, they, and certain other women with them, came to the tomb bringing the spices which they had prepared.", TextStyle.NORMAL)))), versionAbbreviation = "NKJV"),
                    Verse(1, elements = listOf(VerseElement.Text(listOf(TextSpan("七日的頭一日黎明的時候，那些婦女帶著所預備的香料，來到墳墓前。", TextStyle.NORMAL)))), versionAbbreviation = "CUV"),
                    Verse(2, elements = listOf(VerseElement.Text(listOf(TextSpan("But they found the stone rolled away from the tomb.", TextStyle.NORMAL)))), versionAbbreviation = "NKJV"),
                    Verse(2, elements = listOf(VerseElement.Text(listOf(TextSpan("看見石頭已經從墳墓輥開了；", TextStyle.NORMAL)))), versionAbbreviation = "CUV"),
                    Verse(3, elements = listOf(VerseElement.Text(listOf(TextSpan("Then they went in and did not find the body of the Lord Jesus.", TextStyle.NORMAL)))), versionAbbreviation = "NKJV"),
                    Verse(3, elements = listOf(VerseElement.Text(listOf(TextSpan("他們就進去，只是不見主耶穌的身體。", TextStyle.NORMAL)))), versionAbbreviation = "CUV")
                ),
                chaptersVerses = emptyMap(),
                displayMode = DisplayMode.CONTIGUOUS,
                showWordsOfJesus = true,
                selectionEventId = 0L,
                isLoading = false,
                onShowPassageSelection = {},
                onUpdateVisiblePassage = { _, _, _ -> },
                onLoadChapterVerses = { _, _, _ -> }
            )
        }
    }
}

@ThemePreviews
@Composable
fun VerseListLoadingPreview() {
    BibleTheme {
        Surface {
            VerseList(
                currentBook = Book.Genesis,
                currentChapter = 1,
                currentVerse = null,
                verses = emptyList(),
                chaptersVerses = emptyMap(),
                displayMode = DisplayMode.SINGLE_CHAPTER,
                showWordsOfJesus = true,
                selectionEventId = 0L,
                isLoading = true,
                onShowPassageSelection = {},
                onUpdateVisiblePassage = { _, _, _ -> },
                onLoadChapterVerses = { _, _, _ -> }
            )
        }
    }
}
