package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.clear
import bible.composeapp.generated.resources.close
import bible.composeapp.generated.resources.history
import bible.composeapp.generated.resources.search
import bible.composeapp.generated.resources.settings
import bible.composeapp.generated.resources.versions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.presentation.ui.AutoResizedText
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun BibleTopBar(
    currentBook: Book?,
    currentChapter: Int,
    selectedVersions: List<BibleVersion>,
    onSetSearchMode: (Boolean) -> Unit,
    onShowPassageSelection: (initialPage: Int) -> Unit,
    onShowVersionSelection: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
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
                            onClick = { onShowPassageSelection(0) }
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
                    onClick = onShowVersionSelection
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
            IconButton(onClick = { onSetSearchMode(true) }) {
                Icon(
                    painter = painterResource(Res.drawable.search),
                    contentDescription = stringResource(Res.string.search)
                )
            }
            IconButton(onClick = onShowHistory) {
                Icon(
                    painter = painterResource(Res.drawable.history),
                    contentDescription = stringResource(Res.string.history)
                )
            }
            IconButton(onClick = onShowSettings) {
                Icon(
                    painter = painterResource(Res.drawable.settings),
                    contentDescription = stringResource(Res.string.settings)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemePreviews
@Composable
fun BibleTopBarPreview() {
    BibleTheme {
        Surface {
            BibleTopBar(
                currentBook = Book.Luke,
                currentChapter = 18,
                selectedVersions = listOf(BibleVersion("nkjv", "New King James Version", "English", "NKJV")),
                onSetSearchMode = {},
                onShowPassageSelection = {},
                onShowVersionSelection = {},
                onShowHistory = {},
                onShowSettings = {}
            )
        }
    }
}
