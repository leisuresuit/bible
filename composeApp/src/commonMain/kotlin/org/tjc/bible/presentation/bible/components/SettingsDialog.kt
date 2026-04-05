package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.presentation.bible.BibleIntent
import org.tjc.bible.presentation.bible.BibleState
import org.tjc.bible.presentation.bible.DisplayMode
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@Composable
fun SettingsDialog(
    state: BibleState,
    supportsDynamicColor: Boolean,
    onDismiss: () -> Unit,
    onIntent: (BibleIntent) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
        title = {
            Row(
                Modifier.wrapContentHeight().fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text(
                    text = stringResource(Res.string.settings),
                    Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.display_mode),
                    style = MaterialTheme.typography.titleMedium
                )
                Column(Modifier.selectableGroup()) {
                    DisplayMode.entries.forEach { mode ->
                        val selected = state.displayMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    onClick = { onIntent(BibleIntent.UpdateDisplayMode(mode)) },
                                    role = Role.RadioButton
                                )
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                            val label = when (mode) {
                                DisplayMode.SINGLE_CHAPTER -> stringResource(Res.string.display_mode_single_chapter)
                                DisplayMode.CONTIGUOUS -> stringResource(Res.string.display_mode_contiguous)
                            }
                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.theme),
                    style = MaterialTheme.typography.titleMedium
                )
                Column(Modifier.selectableGroup()) {
                    AppTheme.entries.forEach { theme ->
                        val selected = state.theme == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    onClick = { onIntent(BibleIntent.UpdateTheme(theme)) },
                                    role = Role.RadioButton
                                )
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                            val label = when (theme) {
                                AppTheme.SYSTEM -> stringResource(Res.string.theme_system)
                                AppTheme.LIGHT -> stringResource(Res.string.theme_light)
                                AppTheme.DARK -> stringResource(Res.string.theme_dark)
                            }
                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (supportsDynamicColor) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(BibleIntent.UpdateDynamicColor(!state.isDynamicColor)) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.isDynamicColor,
                            onCheckedChange = { onIntent(BibleIntent.UpdateDynamicColor(it)) }
                        )
                        Text(
                            text = stringResource(Res.string.dynamic_color),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    )
}

@ThemePreviews
@Composable
fun SettingsDialogPreview() {
    BibleTheme {
        SettingsDialog(
            state = BibleState(),
            supportsDynamicColor = true,
            onDismiss = {},
            onIntent = {}
        )
    }
}
