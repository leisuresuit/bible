package org.tjc.bible.data.bss

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResponse
import org.tjc.bible.domain.model.SearchSort
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.repository.BibleRepository

class BssBibleRepository(
    private val httpClient: HttpClient
) : BibleRepository {

    override suspend fun getVersions(languages: List<String>): Result<List<BibleVersion>> = runCatching {
        val response = httpClient.get("$BASE_URL/bibles").body<BssVersionsResponse>()
        val versions = response.results.values.map { it.toDomain() }

        // Use this repo for Chinese versions only
        versions.filter { it.language == "zh" }
    }

    override suspend fun getVerses(versionId: String, book: Book, chapter: Int): Result<List<Verse>> = runCatching {
        val reference = "${book.toBssReferenceName()} $chapter"
        val response = httpClient.get(BASE_URL) {
            parameter("bible", versionId)
            parameter("reference", reference)
        }.body<BssVersesResponse>()

        val verseMap = response.results.firstOrNull()?.verses?.get(versionId)?.get(chapter.toString())
        verseMap?.values?.map { it.toDomain() } ?: emptyList()
    }

    private fun Book.toBssReferenceName(): String = when (this) {
        Book.Samuel1 -> "1 Samuel"
        Book.Samuel2 -> "2 Samuel"
        Book.Kings1 -> "1 Kings"
        Book.Kings2 -> "2 Kings"
        Book.Chronicles1 -> "1 Chronicles"
        Book.Chronicles2 -> "2 Chronicles"
        Book.Corinthians1 -> "1 Corinthians"
        Book.Corinthians2 -> "2 Corinthians"
        Book.Thessalonians1 -> "1 Thessalonians"
        Book.Thessalonians2 -> "2 Thessalonians"
        Book.Timothy1 -> "1 Timothy"
        Book.Timothy2 -> "2 Timothy"
        Book.Peter1 -> "1 Peter"
        Book.Peter2 -> "2 Peter"
        Book.John1 -> "1 John"
        Book.John2 -> "2 John"
        Book.John3 -> "3 John"
        Book.SongOfSolomon -> "Song of Solomon"
        else -> name
    }

    override suspend fun search(
        versionId: String,
        query: String,
        offset: Int,
        limit: Int,
        sort: SearchSort
    ): Result<SearchResponse> = runCatching {
        val page = (offset / limit) + 1
        val response = httpClient.get(BASE_URL) {
            parameter("bible", versionId)
            parameter("search", query)
            parameter("limit", limit)
            parameter("page", page)
        }.body<BssSearchResponse>()

        SearchResponse(
            results = response.results.map { it.toDomain(versionId) },
            total = response.paging?.total ?: response.results.size,
            offset = offset,
            limit = limit
        )
    }

    private companion object {
        const val BASE_URL = "https://api.biblesupersearch.com/api"
    }
}
