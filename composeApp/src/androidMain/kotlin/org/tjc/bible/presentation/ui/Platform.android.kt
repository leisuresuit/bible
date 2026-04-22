package org.tjc.bible.presentation.ui

import android.os.Build

import java.util.Locale

actual val supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
actual val deviceLanguage: String
    get() = Locale.getDefault().language
