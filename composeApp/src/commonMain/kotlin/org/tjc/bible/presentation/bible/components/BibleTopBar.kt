package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.history
import bible.composeapp.generated.resources.search
import bible.composeapp.generated.resources.settings
import bible.composeapp.generated.resources.versions
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.presentation.bible.ActiveDialog
import org.tjc.bible.presentation.bible.BibleIntent
import org.tjc.bible.presentation.ui.AutoResizedText
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleTopBar(
    currentBook: Book?,
    currentChapter: Int,
    selectedVersions: List<BibleVersion>,
    onIntent: (BibleIntent) -> Unit
) {
    TopAppBar(
        title = {
            Row(
                Modifier.fillMaxWidth().padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentBook?.let { book ->
                    Row(
                        Modifier.width(0.dp).weight(1f)
                    ) {
                        TextButton(
                            onClick = { onIntent(BibleIntent.ShowDialog(ActiveDialog.PassageSelection(0))) }
                        ) {
                            AutoResizedText(
                                text = "${stringResource(book.nameResource)} $currentChapter",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
                TextButton(
                    onClick = { onIntent(BibleIntent.ShowDialog(ActiveDialog.VersionSelection)) }
                ) {
                    val versionText = if (selectedVersions.size > 1) {
                        stringResource(Res.string.versions)
                    } else {
                        selectedVersions.firstOrNull()?.abbreviation ?: stringResource(Res.string.versions)
                    }
                    Text(
                        text = versionText,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { onIntent(BibleIntent.ShowDialog(ActiveDialog.Search)) }) {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(Res.string.search))
            }
            IconButton(onClick = { onIntent(BibleIntent.ShowDialog(ActiveDialog.History)) }) {
                Icon(Icons.Outlined.DateRange, contentDescription = stringResource(Res.string.history))
            }
            IconButton(onClick = { onIntent(BibleIntent.ShowDialog(ActiveDialog.Settings)) }) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(Res.string.settings))
            }
        }
    )
}

@ThemePreviews
@Composable
fun BibleTopBarPreview() {
    BibleTheme {
        Surface {
            BibleTopBar(
                currentBook = Book.Luke,
                currentChapter = 18,
                selectedVersions = listOf(BibleVersion("nkjv", "New King James Version", "English", "NKJV")),
                onIntent = {}
            )
        }
    }
}
