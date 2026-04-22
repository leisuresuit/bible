package org.tjc.bible.presentation.ui

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual val supportsDynamicColor: Boolean = false
actual val deviceLanguage: String
    get() = NSLocale.currentLocale.languageCode
