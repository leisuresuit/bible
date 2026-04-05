package org.tjc.bible.presentation.ui

import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    uiMode = 0x20, // UI_MODE_NIGHT_YES
    showBackground = true
)
annotation class ThemePreviews
