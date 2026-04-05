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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.cancel
import bible.composeapp.generated.resources.ok
import bible.composeapp.generated.resources.versions
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@Composable
fun VersionSelectionDialog(
    versions: List<BibleVersion>,
    initialSelectedVersions: List<BibleVersion>,
    onDismiss: () -> Unit,
    onVersionsSelected: (List<BibleVersion>) -> Unit
) {
    var tempSelectedVersions by remember { mutableStateOf(initialSelectedVersions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onVersionsSelected(tempSelectedVersions)
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        title = {
            Text(stringResource(Res.string.versions))
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(versions) { version ->
                    val isSelected = tempSelectedVersions.any { it.id == version.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempSelectedVersions = if (isSelected) {
                                    if (tempSelectedVersions.size > 1) {
                                        tempSelectedVersions.filter { it.id != version.id }
                                    } else {
                                        tempSelectedVersions
                                    }
                                } else {
                                    tempSelectedVersions + version
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp)) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(version.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = version.abbreviation,
                                style = MaterialTheme.typography.bodyMedium,
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
                BibleVersion("kjv", "King James Version", "English", "KJV"),
                BibleVersion("niv", "New International Version", "English", "NIV")
            ),
            initialSelectedVersions = listOf(BibleVersion("nkjv", "New King James Version", "English", "NKJV")),
            onDismiss = {},
            onVersionsSelected = {}
        )
    }
}
