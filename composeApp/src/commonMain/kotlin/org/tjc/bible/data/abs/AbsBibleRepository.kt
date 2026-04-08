package org.tjc.bible.data.abs

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import org.tjc.bible.Config
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.repository.BibleRepository

class AbsBibleRepository(
    private val httpClient: HttpClient
) : BibleRepository {

    private fun HttpRequestBuilder.absHeaders() {
        header("api-key", Config.ABS_API_KEY)
    }

    override suspend fun getVersions(language: String?): Result<List<BibleVersion>> = runCatching {
        val response = httpClient.get("https://rest.api.bible/v1/bibles") {
            absHeaders()
            language?.let {
                parameter("language", it.toIso6393())
            }
        }.body<AbsVersionsResponse>()

        response.data
            .map { it.toDomain() }
            .distinctBy { it.abbreviation }
    }

    override suspend fun getVerses(versionId: String, book: Book, chapter: Int): Result<List<Verse>> = runCatching {
        val chapterId = "${book.absId}.$chapter"
        val response = httpClient.get("https://rest.api.bible/v1/bibles/$versionId/chapters/$chapterId") {
            absHeaders()
            parameter("content-type", "json")
            parameter("include-notes", "false")
            parameter("include-titles", "false")
            parameter("include-chapter-numbers", "false")
            parameter("include-verse-numbers", "false")
            parameter("include-verse-spans", "false")
        }.body<AbsChapterResponse>()

        response.data.toDomain()
    }

    override suspend fun search(versionId: String, query: String): Result<List<SearchResult>> = runCatching {
        val response = httpClient.get("https://rest.api.bible/v1/bibles/$versionId/search") {
            absHeaders()
            parameter("query", query)
            parameter("limit", 20)
        }.body<AbsSearchResponse>()

        response.data.verses?.map { it.toDomain(versionId) } ?: emptyList()
    }
}
