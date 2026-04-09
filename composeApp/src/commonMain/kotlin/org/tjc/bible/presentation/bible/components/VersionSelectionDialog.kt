package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.check
import bible.composeapp.generated.resources.close
import bible.composeapp.generated.resources.search
import bible.composeapp.generated.resources.versions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@Composable
fun VersionSelectionDialog(
    versions: List<BibleVersion>,
    selectedVersions: List<BibleVersion>,
    onVersionToggle: (BibleVersion) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredVersions by remember(versions, searchQuery) {
        derivedStateOf {
            versions
                .filter {
                    searchQuery.isEmpty() ||
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.abbreviation.contains(searchQuery, ignoreCase = true)
                }
                .sortedBy { it.abbreviation }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val firstSelectedIndex = filteredVersions.indexOfFirst { version ->
            selectedVersions.any { it.id == version.id }
        }
        if (firstSelectedIndex != -1) {
            listState.scrollToItem(firstSelectedIndex)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(Res.string.close))
            }
        },
        title = {
            SelectionDialogHeader(
                title = stringResource(Res.string.versions),
                searchHint = stringResource(Res.string.search),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                state = listState
            ) {
                items(filteredVersions) { version ->
                    val isSelected = selectedVersions.any { it.id == version.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVersionToggle(version) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp)) {
                            if (isSelected) {
                                Icon(painterResource(Res.drawable.check), contentDescription = null)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = version.abbreviation,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = version.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    )
}

@ThemePreviews
@Composable
fun VersionSelectionDialogPreview() {
    BibleTheme {
        VersionSelectionDialog(
            versions = listOf(
                BibleVersion("nkjv", "New King James Version", "English", "NKJV"),
                BibleVersion("kjv", "King King James Version", "English", "KJV"),
                BibleVersion("niv", "New International Version", "English", "NIV")
            ),
            selectedVersions = listOf(BibleVersion("nkjv", "New King James Version", "English", "NKJV")),
            onVersionToggle = {},
            onDismiss = {}
        )
    }
}
