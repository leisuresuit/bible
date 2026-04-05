package org.tjc.bible

import androidx.compose.ui.window.ComposeUIViewController
import org.tjc.bible.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}
