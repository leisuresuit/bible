package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.arrow_back
import bible.composeapp.generated.resources.arrow_forward
import bible.composeapp.generated.resources.clear
import bible.composeapp.generated.resources.history
import bible.composeapp.generated.resources.next
import bible.composeapp.generated.resources.previous
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.HistoryItem
import org.tjc.bible.presentation.ui.AutoResizedText
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews
import org.tjc.bible.presentation.ui.nameResource

@Composable
fun HistoryScreen(
    history: List<HistoryItem>,
    currentBook: Book?,
    currentChapter: Int,
    currentVerse: Int?,
    onItemClick: (HistoryItem) -> Unit,
    onClear: () -> Unit
) {
    val currentIndex = history.indexOfFirst {
        it.book == currentBook && it.chapter == currentChapter && it.verse == currentVerse
    }
    
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex != -1) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(Res.drawable.history), contentDescription = null)
            Text(
                text = stringResource(Res.string.history),
                Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear) {
                Text(stringResource(Res.string.clear))
            }
            IconButton(
                onClick = { if (currentIndex < history.size - 1) onItemClick(history[currentIndex + 1]) },
                enabled = currentIndex != -1 && currentIndex < history.size - 1
            ) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_back),
                    contentDescription = stringResource(Res.string.previous)
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { if (currentIndex > 0) onItemClick(history[currentIndex - 1]) },
                enabled = currentIndex > 0
            ) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_forward),
                    contentDescription = stringResource(Res.string.next)
                )
            }
        }

        HorizontalDivider()

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            val maxHeight = maxHeight
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(history) { item ->
                    val isSelected = item.book == currentBook &&
                            item.chapter == currentChapter &&
                            item.verse == currentVerse

                    AutoResizedText(
                        text = "${stringResource(item.book.nameResource)} ${item.chapter}${":${item.verse}"}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onItemClick(item) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = if (isSelected) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun HistoryScreenPreview() {
    BibleTheme {
        HistoryScreen(
            history = listOf(
                HistoryItem(Book.Luke, 18, 1),
                HistoryItem(Book.Luke, 24, 1),
                HistoryItem(Book.Matthew, 5, 1)
            ),
            currentBook = Book.Luke,
            currentChapter = 18,
            currentVerse = 1,
            onItemClick = {},
            onClear = {}
        )
    }
}
