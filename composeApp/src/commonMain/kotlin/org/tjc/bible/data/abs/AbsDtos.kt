package org.tjc.bible.data.abs

import kotlinx.serialization.Serializable
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.Verse

@Serializable
internal data class AbsChapterResponse(
    val data: AbsChapterContentDto
)

@Serializable
internal data class AbsChapterContentDto(
    val id: String? = null,
    val bibleId: String? = null,
    val number: String,
    val bookId: String? = null,
    val reference: String? = null,
    val copyright: String? = null,
    val verseCount: Int? = null,
    val content: List<AbsContentItemDto>
)

@Serializable
internal data class AbsContentItemDto(
    val name: String? = null,
    val type: String,
    val text: String? = null,
    val items: List<AbsContentItemDto>? = null,
    val attrs: AbsContentAttrsDto? = null
)

@Serializable
internal data class AbsContentAttrsDto(
    val style: String? = null,
    val verseId: String? = null,
    val vid: String? = null,
    val number: String? = null,
    val verseOrgIds: List<String>? = null
)

@Serializable
internal data class AbsVersionsResponse(
    val data: List<AbsVersionDto>
)

@Serializable
internal data class AbsVersionDto(
    val id: String,
    val name: String,
    val language: AbsLanguageDto,
    val abbreviationLocal: String? = null
)

@Serializable
internal data class AbsLanguageDto(
    val id: String,
    val name: String
)

internal fun AbsVersionDto.toDomain() = BibleVersion(
    id = id,
    name = name,
    language = language.id.toIso6391(),
    abbreviation = abbreviationLocal ?: ""
)

internal fun String.toIso6391(): String =
    when (this.lowercase()) {
        "eng" -> "en"
        "zho" -> "zh"
        "jpn" -> "ja"
        "kor" -> "ko"
        "spa" -> "es"
        "fra" -> "fr"
        "deu" -> "de"
        else -> this
    }

internal fun String.toIso6393(): String =
    when (this.lowercase()) {
        "en" -> "eng"
        "zh" -> "zho"
        "ja" -> "jpn"
        "ko" -> "kor"
        "es" -> "spa"
        "fr" -> "fra"
        "de" -> "deu"
        else -> this
    }

internal fun AbsChapterContentDto.toDomain(): List<Verse> = AbsChapterParser().parse(this)

@Serializable
internal data class AbsSearchResultDto(
    val versionId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

internal fun AbsSearchResultDto.toDomain(): SearchResult {
    val book = try {
        Book.entries.find { it.name.equals(bookId, ignoreCase = true) } ?: Book.Genesis
    } catch (e: Exception) {
        Book.Genesis
    }
    return SearchResult(
        versionId = versionId,
        book = book,
        chapterNumber = chapter,
        verseNumber = verse,
        text = text
    )
}
