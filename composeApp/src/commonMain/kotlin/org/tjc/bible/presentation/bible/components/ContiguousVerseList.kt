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
import androidx.compose.foundation.lazy.rememberLazyListState
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

private data class ContiguousChapterInfo(
    val book: Book,
    val chapter: Int,
    val globalChapterIndex: Int,
    val startItemIndex: Int,
    val verseCount: Int
)

private object ContiguousIndex {
    val chapters: List<ContiguousChapterInfo>
    val totalItems: Int

    init {
        val chapterList = mutableListOf<ContiguousChapterInfo>()
        var globalChapterIndex = 0
        var currentItemIndex = 0
        for (book in Book.entries) {
            for (cIndex in book.versesInChapters.indices) {
                val chapterNumber = cIndex + 1
                val verseCount = book.versesInChapters[cIndex]
                chapterList.add(
                    ContiguousChapterInfo(
                        book = book,
                        chapter = chapterNumber,
                        globalChapterIndex = globalChapterIndex,
                        startItemIndex = currentItemIndex,
                        verseCount = verseCount
                    )
                )
                globalChapterIndex++
                currentItemIndex += 1 + verseCount
            }
        }
        chapters = chapterList
        totalItems = currentItemIndex
    }

    fun getChapterInfo(globalChapterIndex: Int): ContiguousChapterInfo {
        return chapters[globalChapterIndex.coerceIn(0, chapters.lastIndex)]
    }

    private fun getGlobalIndex(book: Book, chapter: Int): Int {
        var sum = 0
        for (b in Book.entries) {
            if (b == book) break
            sum += b.chaptersCount
        }
        return sum + (chapter - 1)
    }

    fun findChapterForGlobalIndex(book: Book, chapter: Int): ContiguousChapterInfo {
        val globalIndex = getGlobalIndex(book, chapter)
        return chapters[globalIndex.coerceIn(0, chapters.lastIndex)]
    }

    fun getItemIndex(book: Book, chapter: Int, verse: Int? = null): Int {
        val info = findChapterForGlobalIndex(book, chapter)
        val v = (verse ?: 1).coerceIn(1, info.verseCount)
        return info.startItemIndex + v
    }

    fun getItemLocation(itemIndex: Int): Triple<ContiguousChapterInfo, Int?, Boolean> {
        val clampedIndex = itemIndex.coerceIn(0, totalItems - 1)
        var low = 0
        var high = chapters.lastIndex
        var foundChapter = chapters[0]

        while (low <= high) {
            val mid = (low + high) / 2
            val info = chapters[mid]
            val endItemIndex = info.startItemIndex + info.verseCount
            if (clampedIndex < info.startItemIndex) {
                high = mid - 1
            } else if (clampedIndex > endItemIndex) {
                low = mid + 1
            } else {
                foundChapter = info
                break
            }
        }

        val offset = clampedIndex - foundChapter.startItemIndex
        val isHeader = (offset == 0)
        val verseNumber = if (isHeader) null else offset
        return Triple(foundChapter, verseNumber, isHeader)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ContiguousVerseList(
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
    val initialItemIndex = remember {
        ContiguousIndex.getItemIndex(currentBook, currentChapter, currentVerse)
    }
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialItemIndex)
    var lastProcessedEventId by rememberSaveable { mutableStateOf(-1L) }

    LaunchedEffect(selectionEventId, currentBook, currentChapter, currentVerse) {
        if (selectionEventId > lastProcessedEventId) {
            val targetItemIndex = ContiguousIndex.getItemIndex(currentBook, currentChapter, currentVerse)
            lazyListState.scrollToItem(targetItemIndex)
            lastProcessedEventId = selectionEventId
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIndex ->
                val (chapterInfo, verseNum, _) = ContiguousIndex.getItemLocation(firstIndex)
                val visibleVerse = verseNum ?: 1
                onUpdateVisiblePassage(chapterInfo.book, chapterInfo.chapter, visibleVerse)

                val currentGlobalIndex = chapterInfo.globalChapterIndex
                onLoadChapterVerses(chapterInfo.book, chapterInfo.chapter, currentGlobalIndex)

                if (currentGlobalIndex > 0) {
                    val prevInfo = ContiguousIndex.getChapterInfo(currentGlobalIndex - 1)
                    onLoadChapterVerses(prevInfo.book, prevInfo.chapter, currentGlobalIndex - 1)
                }
                if (currentGlobalIndex < ContiguousIndex.chapters.lastIndex) {
                    val nextInfo = ContiguousIndex.getChapterInfo(currentGlobalIndex + 1)
                    onLoadChapterVerses(nextInfo.book, nextInfo.chapter, currentGlobalIndex + 1)
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
                items(
                    count = ContiguousIndex.totalItems,
                    key = { index ->
                        val (info, verseNum, isHeader) = ContiguousIndex.getItemLocation(index)
                        if (isHeader) {
                            "header_${info.book.name}_${info.chapter}"
                        } else {
                            "verse_${info.book.name}_${info.chapter}_$verseNum"
                        }
                    }
                ) { index ->
                    val (info, verseNum, isHeader) = ContiguousIndex.getItemLocation(index)

                    LaunchedEffect(info.globalChapterIndex) {
                        onLoadChapterVerses(info.book, info.chapter, info.globalChapterIndex)
                    }

                    if (isHeader) {
                        ChapterHeader(info.book, info.chapter) {
                            onShowPassageSelection(0)
                        }
                    } else {
                        val chapterVerses = chaptersVerses[info.globalChapterIndex]
                        if (chapterVerses != null && verseNum != null) {
                            val versesForNumber = chapterVerses.filter { it.number == verseNum }
                            if (versesForNumber.isNotEmpty()) {
                                Column {
                                    versesForNumber.forEach { v ->
                                        VerseItem(v, showWordsOfJesus)
                                    }
                                }
                            } else {
                                LoadingVersePlaceholder()
                            }
                        } else {
                            LoadingVersePlaceholder()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingVersePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}
