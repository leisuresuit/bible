package org.tjc.bible.data.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.tjc.bible.cache.BibleDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(BibleDatabase.Schema, "bible.db")
    }
}
