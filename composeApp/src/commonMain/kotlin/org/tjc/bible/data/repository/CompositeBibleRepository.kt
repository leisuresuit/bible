package org.tjc.bible.data.repository

import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResponse
import org.tjc.bible.domain.model.SearchSort
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.repository.BibleRepository

class CompositeBibleRepository(
    private val repositories: List<BibleRepository>
) : BibleRepository {

    private val versionToRepoMap = mutableMapOf<String, BibleRepository>()

    override suspend fun getVersions(languages: List<String>): Result<List<BibleVersion>> {
        val results = repositories.map { repo ->
            repo.getVersions(languages).onSuccess { versions ->
                versions.forEach { version ->
                    versionToRepoMap[version.id] = repo
                }
            }
        }
        val successfulVersions = results.mapNotNull { it.getOrNull() }.flatten().distinctBy { it.id }

        return if (results.any { it.isSuccess }) {
            Result.success(successfulVersions)
        } else {
            results.firstOrNull { it.isFailure } ?: Result.success(emptyList())
        }
    }

    override suspend fun getVerses(versionId: String, book: Book, chapter: Int): Result<List<Verse>> {
        // Try the cached repository first
        versionToRepoMap[versionId]?.let { repo ->
            val result = repo.getVerses(versionId, book, chapter)
            if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
                return result
            }
        }

        var firstFailure: Result<List<Verse>>? = null
        var firstEmptySuccess: Result<List<Verse>>? = null

        for (repo in repositories) {
            // Skip the one we already tried
            if (repo == versionToRepoMap[versionId]) continue

            val result = repo.getVerses(versionId, book, chapter)
            if (result.isSuccess) {
                val verses = result.getOrThrow()
                if (verses.isNotEmpty()) {
                    versionToRepoMap[versionId] = repo
                    return result
                }
                if (firstEmptySuccess == null) firstEmptySuccess = result
            } else {
                if (firstFailure == null) firstFailure = result
            }
        }
        return firstEmptySuccess ?: firstFailure ?: Result.failure(Exception("Verses not found for version $versionId"))
    }

    override suspend fun search(
        versionId: String,
        query: String,
        offset: Int,
        limit: Int,
        sort: SearchSort
    ): Result<SearchResponse> {
        // Try the cached repository first
        versionToRepoMap[versionId]?.let { repo ->
            val result = repo.search(versionId, query, offset, limit, sort)
            if (result.isSuccess && result.getOrThrow().results.isNotEmpty()) {
                return result
            }
        }

        var firstFailure: Result<SearchResponse>? = null
        var firstEmptySuccess: Result<SearchResponse>? = null

        for (repo in repositories) {
            // Skip the one we already tried
            if (repo == versionToRepoMap[versionId]) continue

            val result = repo.search(versionId, query, offset, limit, sort)
            if (result.isSuccess) {
                val response = result.getOrThrow()
                if (response.results.isNotEmpty()) {
                    versionToRepoMap[versionId] = repo
                    return result
                }
                if (firstEmptySuccess == null) firstEmptySuccess = result
            } else {
                if (firstFailure == null) firstFailure = result
            }
        }
        return firstEmptySuccess ?: firstFailure ?: Result.failure(Exception("Search results not found for version $versionId"))
    }
}
