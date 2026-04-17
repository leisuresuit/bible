package org.tjc.bible.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.tjc.bible.presentation.bible.BibleViewModel

class KoinHelper : KoinComponent {
    fun getBibleViewModel(): BibleViewModel = get()
}

fun doInitKoinIos() = initKoin {}
