package org.tjc.bible.data.repository

import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.repository.BibleRepository

class MockBibleRepository : BibleRepository {
    private val versions = listOf(
        BibleVersion("nkjv", "New King James Version", "English", "NKJV"),
        BibleVersion("kjv", "King James Version", "English", "KJV"),
        BibleVersion("cuv", "Chinese Union Version", "Chinese", "CUV"),
        BibleVersion("niv", "New International Version", "English", "NIV")
    )

    override suspend fun getVersions(language: String?): List<BibleVersion> {
        return if (language == null) versions else versions.filter { it.language == language }
    }

    override suspend fun getVerses(versionId: String, book: Book, chapter: Int): List<Verse> {
        return buildList {
            repeat(50) { index ->
                val number = index + 1
                add(Verse(number, "Mock verse $number"))
            }
        }
    }

    override suspend fun search(versionId: String, query: String): List<SearchResult> {
        return listOf(
            SearchResult(versionId, Book.Genesis, 1, 1, "In the beginning God created...")
        )
    }
}
