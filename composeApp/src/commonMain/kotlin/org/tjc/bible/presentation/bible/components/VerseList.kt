package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
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

    if (displayMode == DisplayMode.SINGLE_CHAPTER) {
        SingleChapterVerseList(
            currentBook = currentBook,
            currentChapter = currentChapter,
            currentVerse = currentVerse,
            chaptersVerses = chaptersVerses,
            showWordsOfJesus = showWordsOfJesus,
            selectionEventId = selectionEventId,
            onShowPassageSelection = onShowPassageSelection,
            onUpdateVisiblePassage = onUpdateVisiblePassage,
            onLoadChapterVerses = onLoadChapterVerses,
            modifier = modifier,
            nestedScrollConnection = nestedScrollConnection,
            contentPadding = contentPadding
        )
    } else {
        ContiguousVerseList(
            currentBook = currentBook,
            currentChapter = currentChapter,
            currentVerse = currentVerse,
            chaptersVerses = chaptersVerses,
            showWordsOfJesus = showWordsOfJesus,
            selectionEventId = selectionEventId,
            onShowPassageSelection = onShowPassageSelection,
            onUpdateVisiblePassage = onUpdateVisiblePassage,
            onLoadChapterVerses = onLoadChapterVerses,
            modifier = modifier,
            nestedScrollConnection = nestedScrollConnection,
            contentPadding = contentPadding
        )
    }
}

private fun getGlobalIndex(book: Book, chapter: Int): Int {
    var sum = 0
    for (b in Book.entries) {
        if (b == book) break
        sum += b.chaptersCount
    }
    return sum + (chapter - 1)
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
                chaptersVerses = mapOf(
                    getGlobalIndex(Book.Luke, 18) to listOf(
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
                    )
                ),
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
                chaptersVerses = mapOf(
                    getGlobalIndex(Book.Luke, 24) to listOf(
                        Verse(1, elements = listOf(VerseElement.Text(listOf(TextSpan("Now on the first day of the week, very early in the morning, they, and certain other women with them, came to the tomb bringing the spices which they had prepared.", TextStyle.NORMAL)))), versionAbbreviation = "NKJV"),
                        Verse(1, elements = listOf(VerseElement.Text(listOf(TextSpan("七日的頭一日黎明的時候，那些婦女帶著所預備的香料，來到墳墓前。", TextStyle.NORMAL)))), versionAbbreviation = "CUV"),
                        Verse(2, elements = listOf(VerseElement.Text(listOf(TextSpan("But they found the stone rolled away from the tomb.", TextStyle.NORMAL)))), versionAbbreviation = "NKJV"),
                        Verse(2, elements = listOf(VerseElement.Text(listOf(TextSpan("看見石頭已經從墳墓輥開了；", TextStyle.NORMAL)))), versionAbbreviation = "CUV"),
                        Verse(3, elements = listOf(VerseElement.Text(listOf(TextSpan("Then they went in and did not find the body of the Lord Jesus.", TextStyle.NORMAL)))), versionAbbreviation = "NKJV"),
                        Verse(3, elements = listOf(VerseElement.Text(listOf(TextSpan("他們就進去，只是不見主耶穌的身體。", TextStyle.NORMAL)))), versionAbbreviation = "CUV")
                    )
                ),
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
