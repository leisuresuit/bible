package org.tjc.bible.data.repository

import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.TextSpan
import org.tjc.bible.domain.model.TextStyle
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.model.VerseElement
import org.tjc.bible.domain.repository.BibleRepository

import org.tjc.bible.domain.model.SearchResponse

import org.tjc.bible.domain.model.SearchSort

class MockBibleRepository : BibleRepository {
    private val versions = listOf(
        BibleVersion("nkjv", "New King James Version", "English", "NKJV"),
        BibleVersion("kjv", "King James Version", "English", "KJV"),
        BibleVersion("cuv", "Chinese Union Version", "Chinese", "CUV"),
        BibleVersion("niv", "New International Version", "English", "NIV")
    )

    override suspend fun getVersions(languages: List<String>): Result<List<BibleVersion>> = Result.success(
        if (languages.isEmpty()) versions else versions.filter { languages.contains(it.language) }
    )

    override suspend fun getVerses(versionId: String, book: Book, chapter: Int): Result<List<Verse>> = Result.success(
        buildList {
            repeat(50) { index ->
                val number = index + 1
                add(
                    Verse(
                        number = number,
                        elements = listOf(
                            VerseElement.Text(
                                listOf(TextSpan("Mock verse $number", TextStyle.NORMAL))
                            )
                        )
                    )
                )
            }
        }
    )

    override suspend fun search(
        versionId: String,
        query: String,
        offset: Int,
        limit: Int,
        sort: SearchSort
    ): Result<SearchResponse> {
        val totalAvailable = 100 // Simulate 100 total results
        val count = if (offset + limit > totalAvailable) {
            (totalAvailable - offset).coerceAtLeast(0)
        } else {
            limit
        }
        
        val results = List(count) { i ->
            val globalIndex = offset + i
            SearchResult(
                id = "${versionId}_${globalIndex}",
                versionId = versionId,
                book = Book.entries[globalIndex % Book.entries.size],
                chapterNumber = (globalIndex / 10) + 1,
                verseNumber = (globalIndex % 10) + 1,
                text = "Mock result ${globalIndex + 1} for '$query' in $versionId. This is a longer text to test the search result item layout and how it handles multi-line content."
            )
        }

        return Result.success(
            SearchResponse(
                results = results,
                total = totalAvailable,
                offset = offset,
                limit = limit
            )
        )
    }
}
