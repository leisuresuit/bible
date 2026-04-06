package org.tjc.bible.data.abs

import kotlinx.serialization.Serializable
import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.model.TextSpan
import org.tjc.bible.domain.model.TextStyle
import org.tjc.bible.domain.model.Verse

@Serializable
data class AbsChapterResponse(
    val data: AbsChapterContentDto
)

@Serializable
data class AbsChapterContentDto(
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
data class AbsContentItemDto(
    val name: String? = null,
    val type: String,
    val text: String? = null,
    val items: List<AbsContentItemDto>? = null,
    val attrs: AbsContentAttrsDto? = null
)

@Serializable
data class AbsContentAttrsDto(
    val style: String? = null,
    val verseId: String? = null,
    val vid: String? = null,
    val number: String? = null,
    val verseOrgIds: List<String>? = null
)

@Serializable
data class AbsVersionsResponse(
    val data: List<AbsVersionDto>
)

@Serializable
data class AbsVersionDto(
    val id: String,
    val name: String,
    val language: AbsLanguageDto,
    val abbreviation: String? = null
)

@Serializable
data class AbsLanguageDto(
    val id: String,
    val name: String
)

fun AbsVersionDto.toDomain() = BibleVersion(
    id = id,
    name = name,
    language = language.id.toIso6391(),
    abbreviation = abbreviation ?: ""
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

fun AbsChapterContentDto.toDomain(): List<Verse> {
    val versesMap = mutableMapOf<Int, MutableList<TextSpan>>()
    val headingsMap = mutableMapOf<Int, MutableList<MutableList<TextSpan>>>()
    
    val currentHeadings = mutableListOf<MutableList<TextSpan>>()
    var pendingBreak = false

    fun extractVerseNumber(vId: String): Int {
        val parts = vId.split(".", " ", ":")
        if (parts.size < 3) return 0
        return parts.lastOrNull { it.any { it.isDigit() } }
            ?.takeWhile { it.isDigit() }
            ?.toIntOrNull() ?: 0
    }

    fun process(item: AbsContentItemDto, inheritedVerseId: String?, inheritedStyle: TextStyle) {
        val style = when (val s = item.attrs?.style) {
            "bd", "itbd", "nd" -> TextStyle.BOLD
            "it" -> TextStyle.ITALIC
            "wj" -> TextStyle.BOLD // Words of Jesus
            "s", "s1", "s2", "s3", "ms", "ms1", "d", "sp", "qa" -> TextStyle.HEADING
            else -> inheritedStyle
        }

        val currentVerseId = item.attrs?.verseId ?: item.attrs?.vid ?: inheritedVerseId

        val isBlock = item.type == "tag" && (
            item.name == "p" || item.name == "para" ||
            item.attrs?.style?.let { s ->
                s.startsWith("p") || s.startsWith("q") || s.startsWith("s") || s == "m" || s == "nb"
            } == true
        )
        if (isBlock) {
            pendingBreak = true
        }

        if (item.type == "text") {
            item.text?.let { text ->
                if (style == TextStyle.HEADING) {
                    if (pendingBreak || currentHeadings.isEmpty()) {
                        currentHeadings.add(mutableListOf())
                        pendingBreak = false
                    }
                    currentHeadings.last().add(TextSpan(text, style))
                } else if (currentVerseId != null) {
                    val verseNumber = extractVerseNumber(currentVerseId)
                    if (verseNumber > 0) {
                        val spans = versesMap.getOrPut(verseNumber) { mutableListOf() }
                        
                        // If we have headings accumulated before this verse, attach them
                        if (currentHeadings.isNotEmpty()) {
                            headingsMap.getOrPut(verseNumber) { mutableListOf() }.addAll(currentHeadings)
                            currentHeadings.clear()
                            pendingBreak = true // Force break after heading in text flow if needed
                        }

                        if (pendingBreak) {
                            if (spans.isNotEmpty() && !spans.last().text.endsWith("\n")) {
                                spans.add(TextSpan("\n", TextStyle.NORMAL))
                            }
                            pendingBreak = false
                        }
//                        else if (spans.isNotEmpty() && !spans.last().text.let {
//                                it.endsWith(" ") || it.endsWith("\n")
//                            } && !text.startsWith(" ")) {
//                            spans.add(TextSpan(" ", TextStyle.NORMAL))
//                        }
                        spans.add(TextSpan(text, style))
                    }
                }
            }
        }

        item.items?.forEach { process(it, currentVerseId, style) }
    }

    content.forEach { process(it, null, TextStyle.NORMAL) }

    return versesMap.keys.union(headingsMap.keys).map { number ->
        val spans = versesMap[number] ?: emptyList()
        val mergedSpans = mutableListOf<TextSpan>()
        for (span in spans) {
            if (mergedSpans.isNotEmpty() && mergedSpans.last().style == span.style) {
                val last = mergedSpans.removeAt(mergedSpans.size - 1)
                mergedSpans.add(TextSpan(last.text + span.text, span.style))
            } else {
                mergedSpans.add(span)
            }
        }

        val verseHeadings = headingsMap[number]?.map { headingSpans ->
            val mergedHeading = mutableListOf<TextSpan>()
            for (span in headingSpans) {
                if (mergedHeading.isNotEmpty() && mergedHeading.last().style == span.style) {
                    val last = mergedHeading.removeAt(mergedHeading.size - 1)
                    mergedHeading.add(TextSpan(last.text + span.text, span.style))
                } else {
                    mergedHeading.add(span)
                }
            }
            mergedHeading
        } ?: emptyList()

        Verse(
            number = number,
            richText = mergedSpans,
            headings = verseHeadings
        )
    }.sortedBy { it.number }
}

@Serializable
data class AbsSearchResultDto(
    val versionId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

fun AbsSearchResultDto.toDomain(): SearchResult {
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
