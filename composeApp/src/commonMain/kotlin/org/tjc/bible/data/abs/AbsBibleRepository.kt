package org.tjc.bible.data.abs

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
        val chapterId = getChapterId(book, chapter)
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

    private fun getChapterId(book: Book, chapter: Int): String {
        val bookId = when (book) {
            Book.Genesis -> "GEN"
            Book.Exodus -> "EXO"
            Book.Leviticus -> "LEV"
            Book.Numbers -> "NUM"
            Book.Deuteronomy -> "DEU"
            Book.Joshua -> "JOS"
            Book.Judges -> "JDG"
            Book.Ruth -> "RUT"
            Book.Samuel1 -> "1SA"
            Book.Samuel2 -> "2SA"
            Book.Kings1 -> "1KI"
            Book.Kings2 -> "2KI"
            Book.Chronicles1 -> "1CH"
            Book.Chronicles2 -> "2CH"
            Book.Ezra -> "EZR"
            Book.Nehemiah -> "NEH"
            Book.Esther -> "EST"
            Book.Job -> "JOB"
            Book.Psalms -> "PSA"
            Book.Proverbs -> "PRO"
            Book.Ecclesiastes -> "ECC"
            Book.SongOfSolomon -> "SNG"
            Book.Isaiah -> "ISA"
            Book.Jeremiah -> "JER"
            Book.Lamentations -> "LAM"
            Book.Ezekiel -> "EZK"
            Book.Daniel -> "DAN"
            Book.Hosea -> "HOS"
            Book.Joel -> "JOL"
            Book.Amos -> "AMO"
            Book.Obadiah -> "OBA"
            Book.Jonah -> "JON"
            Book.Micah -> "MIC"
            Book.Nahum -> "NAM"
            Book.Habakkuk -> "HAB"
            Book.Zephaniah -> "ZEP"
            Book.Haggai -> "HAG"
            Book.Zechariah -> "ZEC"
            Book.Malachi -> "MAL"
            Book.Matthew -> "MAT"
            Book.Mark -> "MRK"
            Book.Luke -> "LUK"
            Book.John -> "JHN"
            Book.Acts -> "ACT"
            Book.Romans -> "ROM"
            Book.Corinthians1 -> "1CO"
            Book.Corinthians2 -> "2CO"
            Book.Galatians -> "GAL"
            Book.Ephesians -> "EPH"
            Book.Philippians -> "PHP"
            Book.Colossians -> "COL"
            Book.Thessalonians1 -> "1TH"
            Book.Thessalonians2 -> "2TH"
            Book.Timothy1 -> "1TI"
            Book.Timothy2 -> "2TI"
            Book.Titus -> "TIT"
            Book.Philemon -> "PHM"
            Book.Hebrews -> "HEB"
            Book.James -> "JAS"
            Book.Peter1 -> "1PE"
            Book.Peter2 -> "2PE"
            Book.John1 -> "1JN"
            Book.John2 -> "2JN"
            Book.John3 -> "3JN"
            Book.Jude -> "JUD"
            Book.Revelation -> "REV"
        }
        return "$bookId.$chapter"
    }

    override suspend fun search(versionId: String, query: String): Result<List<SearchResult>> = runCatching {
        // Skeleton implementation
        emptyList()
    }
}
