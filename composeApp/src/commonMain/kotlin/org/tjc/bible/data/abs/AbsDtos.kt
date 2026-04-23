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
        "cmn" -> "zh"
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
        "zh" -> "cmn"
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
    val id: String,
    val bibleId: String,
    val bookId: String,
    val chapterId: String,
    val reference: String,
    val text: String
)

@Serializable
internal data class AbsSearchDataDto(
    val query: String,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val verses: List<AbsSearchResultDto>? = null
)

@Serializable
internal data class AbsSearchResponse(
    val data: AbsSearchDataDto
)

internal fun AbsSearchResultDto.toDomain(versionId: String): SearchResult {
    val book = Book.entries.find { it.absId == bookId } ?: Book.Genesis

    // Reference typically has format "Genesis 1:1"
    // ID typically has format "GEN.1.1" or "GEN.1.1-GEN.1.2"
    val parts = id.split(".")
    val chapter = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val verse = parts.getOrNull(2)?.toIntOrNull() ?: 1

    return SearchResult(
        id = id,
        versionId = versionId,
        book = book,
        chapterNumber = chapter,
        verseNumber = verse,
        text = text
    )
}

internal val Book.absId: String
    get() = when (this) {
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
