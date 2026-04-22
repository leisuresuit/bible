package org.tjc.bible.domain.repository

import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.model.SearchResult

import org.tjc.bible.domain.model.SearchResponse
import org.tjc.bible.domain.model.SearchSort

interface BibleRepository {
    /**
     * @param languages List of ISO 639-1 language codes, e.g. ["en", "zh"]
     */
    suspend fun getVersions(languages: List<String> = emptyList()): Result<List<BibleVersion>>
    suspend fun getVerses(versionId: String, book: Book, chapter: Int): Result<List<Verse>>
    suspend fun search(
        versionId: String,
        query: String,
        offset: Int = 0,
        limit: Int = 20,
        sort: SearchSort = SearchSort.RELEVANCE
    ): Result<SearchResponse>
}
