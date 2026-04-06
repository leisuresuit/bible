package org.tjc.bible.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable

@Composable
expect fun platformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BibleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = platformColorScheme(darkTheme, dynamicColor)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = content
    )
}
