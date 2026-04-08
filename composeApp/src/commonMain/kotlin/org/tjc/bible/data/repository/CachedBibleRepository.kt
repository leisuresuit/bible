package org.tjc.bible.data.repository

import kotlinx.serialization.json.Json
import org.tjc.bible.cache.BibleDatabase
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.repository.BibleRepository
import kotlin.time.Clock

class CachedBibleRepository(
    private val delegate: BibleRepository,
    database: BibleDatabase
) : BibleRepository {

    private val queries = database.bibleDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getVersions(language: String?): Result<List<BibleVersion>> {
        val langKey = language ?: "all"
        // Try cache first
        val cached = queries.getVersions(langKey).executeAsOneOrNull()
        if (cached != null) {
            return Result.success(json.decodeFromString<List<BibleVersion>>(cached.versionsJson))
        }

        // Fetch from network if not in cache
        val result = delegate.getVersions(language)

        // Store in cache if successful
        result.onSuccess { versions ->
            queries.insertVersions(
                language = langKey,
                versionsJson = json.encodeToString(versions),
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )
        }

        return result
    }

    override suspend fun getVerses(versionId: String, book: Book, chapter: Int): Result<List<Verse>> {
        // Try cache first
        val cached = queries.getChapter(versionId, book.name, chapter.toLong()).executeAsOneOrNull()
        if (cached != null) {
            return Result.success(json.decodeFromString<List<Verse>>(cached.contentJson))
        }

        // Fetch from network if not in cache
        val result = delegate.getVerses(versionId, book, chapter)
        
        // Store in cache if successful
        result.onSuccess { verses ->
            queries.insertChapter(
                bibleId = versionId,
                bookId = book.name,
                chapterNumber = chapter.toLong(),
                contentJson = json.encodeToString(verses),
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )
        }
        
        return result
    }

    override suspend fun search(versionId: String, query: String): Result<List<SearchResult>> {
        // Try cache first
        val cached = queries.getSearch(versionId, query).executeAsOneOrNull()
        if (cached != null) {
            return Result.success(json.decodeFromString<List<SearchResult>>(cached.resultsJson))
        }

        // Fetch from network if not in cache
        val result = delegate.search(versionId, query)

        // Store in cache if successful
        result.onSuccess { results ->
            queries.insertSearch(
                bibleId = versionId,
                query = query,
                resultsJson = json.encodeToString(results),
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )
        }

        return result
    }
}
