package org.tjc.bible.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
expect fun platformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme

@Composable
fun BibleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = platformColorScheme(darkTheme, dynamicColor)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
