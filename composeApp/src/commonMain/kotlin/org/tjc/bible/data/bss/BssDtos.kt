package org.tjc.bible.data.bss

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.model.VerseElement
import org.tjc.bible.domain.model.TextSpan
import org.tjc.bible.domain.model.TextStyle

@Serializable
internal data class BssVersionsResponse(
    val results: Map<String, BssVersionDto>
)

@Serializable
internal data class BssVersionDto(
    val name: String,
    val shortname: String,
    val module: String,
    @SerialName("lang_short")
    val language: String
)

internal fun BssVersionDto.toDomain() = BibleVersion(
    id = module,
    name = name,
    language = language,
    abbreviation = shortname
)

@Serializable
internal data class BssVersesResponse(
    val results: List<BssVerseResultDto>
)

@Serializable
internal data class BssVerseResultDto(
    val verses: Map<String, Map<String, Map<String, BssVerseDto>>>
)

@Serializable
internal data class BssVerseDto(
    val id: Int,
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

internal fun BssVerseDto.toDomain(): Verse {
    // remove non-breaking spaces from the text
    val cleanedText = if (text.any { it.code in 0x4E00..0x9FFF }) {
        text.replace(" ", "").replace("\u3000", "")
    } else {
        text
    }
    return Verse(
        number = verse,
        elements = listOf(
            VerseElement.Text(
                spans = listOf(
                    TextSpan(text = cleanedText, style = TextStyle.NORMAL)
                )
            )
        )
    )
}

@Serializable
internal data class BssSearchResponse(
    val results: List<BssSearchResultDto>,
    val paging: BssPagingDto? = null
)

@Serializable
internal data class BssSearchResultDto(
    val id: String,
    @SerialName("book_name") val bookName: String,
    val book: String,
    val chapter: String,
    val verse: String,
    val text: String
)

@Serializable
internal data class BssPagingDto(
    val total: Int,
    @SerialName("per_page") val perPage: Int,
    @SerialName("current_page") val currentPage: Int
)

internal fun BssSearchResultDto.toDomain(versionId: String): SearchResult {
    val bookIdInt = book.toIntOrNull() ?: 1
    // BSS uses 1-66 for books usually, let's map them to our Book enum
    val bookEnum = Book.entries.getOrNull(bookIdInt - 1) ?: Book.Genesis
    
    return SearchResult(
        id = id,
        versionId = versionId,
        book = bookEnum,
        chapterNumber = chapter.toIntOrNull() ?: 1,
        verseNumber = verse.toIntOrNull() ?: 1,
        text = text
    )
}
