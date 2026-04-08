package org.tjc.bible.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.tjc.bible.data.cache.DriverFactory
import org.tjc.bible.data.local.createDataStore

actual val platformModule: Module = module {
    single { createDataStore(androidContext()) }
    single { DriverFactory(androidContext()) }
}
