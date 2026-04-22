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
import org.tjc.bible.domain.model.SearchResponse
import org.tjc.bible.domain.model.SearchSort
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.repository.BibleRepository

class AbsBibleRepository(
    private val httpClient: HttpClient
) : BibleRepository {

    override suspend fun getVersions(languages: List<String>): Result<List<BibleVersion>> = runCatching {
        val response = httpClient.get(BASE_URL) {
            absHeaders()
        }.body<AbsVersionsResponse>()

        val versions = response.data
            .map { it.toDomain() }
            .distinctBy { it.abbreviation }

        if (languages.isNotEmpty()) {
            versions.filter { languages.contains(it.language) }
        } else {
            versions
        }
    }

    override suspend fun getVerses(versionId: String, book: Book, chapter: Int): Result<List<Verse>> = runCatching {
        val chapterId = "${book.absId}.$chapter"
        val response = httpClient.get("$BASE_URL/$versionId/chapters/$chapterId") {
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

    override suspend fun search(
        versionId: String,
        query: String,
        offset: Int,
        limit: Int,
        sort: SearchSort
    ): Result<SearchResponse> = runCatching {
        val response = httpClient.get("$BASE_URL/$versionId/search") {
            absHeaders()
            parameter("query", query)
            parameter("sort", when (sort) {
                SearchSort.RELEVANCE -> "relevance"
                SearchSort.CANONICAL -> "canonical"
            })
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<AbsSearchResponse>()

        SearchResponse(
            results = response.data.verses?.map { it.toDomain(versionId) } ?: emptyList(),
            total = response.data.total,
            offset = response.data.offset,
            limit = response.data.limit
        )
    }

    private fun HttpRequestBuilder.absHeaders() {
        header("api-key", Config.ABS_API_KEY)
    }

    private companion object {
        const val BASE_URL = "https://rest.api.bible/v1/bibles"
    }
}
