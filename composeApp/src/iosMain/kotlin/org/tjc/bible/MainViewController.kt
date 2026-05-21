package org.tjc.bible

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.window.ComposeUIViewController
import org.tjc.bible.di.initKoin

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun MainViewController() = ComposeUIViewController {
    val windowSizeClass = calculateWindowSizeClass()
    App(windowSizeClass = windowSizeClass)
}
