package org.tjc.bible.domain.repository

import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.model.SearchResult

interface BibleRepository {
    /**
     * @param language ISO 639-1 language code, e.g. "en" for English
     */
    suspend fun getVersions(language: String? = null): List<BibleVersion>
    suspend fun getVerses(versionId: String, book: Book, chapter: Int): List<Verse>
    suspend fun search(versionId: String, query: String): List<SearchResult>
}
