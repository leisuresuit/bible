package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.AppTheme
import org.tjc.bible.presentation.bible.DisplayMode
import org.tjc.bible.presentation.ui.BibleTheme
import org.tjc.bible.presentation.ui.ThemePreviews

@Composable
fun SettingsScreen(
    displayMode: DisplayMode,
    showWordsOfJesus: Boolean,
    theme: AppTheme,
    isDynamicColor: Boolean,
    supportsDynamicColor: Boolean,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onShowWordsOfJesusChange: (Boolean) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
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
            Icon(painterResource(Res.drawable.settings), contentDescription = null)
            Text(
                text = stringResource(Res.string.settings),
                Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.weight(1f))
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            val maxHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.display_mode),
                    style = MaterialTheme.typography.titleMedium
                )
                Column(Modifier.selectableGroup()) {
                    DisplayMode.entries.forEach { mode ->
                        val selected = displayMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    onClick = { onDisplayModeChange(mode) },
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

                HorizontalDivider(Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowWordsOfJesusChange(!showWordsOfJesus) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showWordsOfJesus,
                        onCheckedChange = { onShowWordsOfJesusChange(it) }
                    )
                    Text(
                        text = stringResource(Res.string.show_words_of_jesus),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                HorizontalDivider(Modifier.fillMaxWidth())

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.theme),
                    style = MaterialTheme.typography.titleMedium
                )
                Column(Modifier.selectableGroup()) {
                    AppTheme.entries.forEach { themeEntry ->
                        val selected = theme == themeEntry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    onClick = { onThemeChange(themeEntry) },
                                    role = Role.RadioButton
                                )
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                            val label = when (themeEntry) {
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
                            .clickable { onDynamicColorChange(!isDynamicColor) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDynamicColor,
                            onCheckedChange = { onDynamicColorChange(it) }
                        )
                        Text(
                            text = stringResource(Res.string.dynamic_color),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

    }
}

@ThemePreviews
@Composable
fun SettingsScreenPreview() {
    BibleTheme {
        SettingsScreen(
            displayMode = DisplayMode.SINGLE_CHAPTER,
            showWordsOfJesus = true,
            theme = AppTheme.SYSTEM,
            isDynamicColor = true,
            supportsDynamicColor = true,
            onDisplayModeChange = {},
            onShowWordsOfJesusChange = {},
            onThemeChange = {},
            onDynamicColorChange = {}
        )
    }
}
