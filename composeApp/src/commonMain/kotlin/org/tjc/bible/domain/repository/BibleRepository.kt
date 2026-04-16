package org.tjc.bible.domain.repository

import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.model.SearchResult

import org.tjc.bible.domain.model.SearchResponse
import org.tjc.bible.domain.model.SearchSort

interface BibleRepository {
    /**
     * @param language ISO 639-1 language code, e.g. "en" for English
     */
    suspend fun getVersions(language: String? = null): Result<List<BibleVersion>>
    suspend fun getVerses(versionId: String, book: Book, chapter: Int): Result<List<Verse>>
    suspend fun search(
        versionId: String,
        query: String,
        offset: Int = 0,
        limit: Int = 20,
        sort: SearchSort = SearchSort.RELEVANCE
    ): Result<SearchResponse>
}
