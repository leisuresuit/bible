package org.tjc.bible.data.abs

import org.tjc.bible.domain.model.TextSpan
import org.tjc.bible.domain.model.TextStyle
import org.tjc.bible.domain.model.Verse

/**
 * Parser for API.Bible (from American Bible Society)
 */
internal class AbsChapterParser {
    private val versesMap = mutableMapOf<Int, MutableList<TextSpan>>()
    private val headingsMap = mutableMapOf<Int, MutableList<MutableList<TextSpan>>>()
    private val currentHeadings = mutableListOf<MutableList<TextSpan>>()
    private var pendingBreak = false

    fun parse(dto: AbsChapterContentDto): List<Verse> {
        dto.content.forEach { process(it, null, TextStyle.NORMAL, false) }
        return buildVerses()
    }

    private fun process(item: AbsContentItemDto, inheritedVerseId: String?, inheritedStyle: TextStyle, inHeading: Boolean) {
        val style = item.getStyle(inheritedStyle)
        val currentVerseId = item.attrs?.verseId ?: item.attrs?.vid ?: inheritedVerseId

        val triggersHeading = item.attrs?.style?.let { s ->
            // Styles starting with 's' are headings (s1, s2, s3), excluding 'sc' which is inline small-caps.
            // Also include major sections (ms), descriptive titles (d), speakers (sp), and acrostics (qa).
            (s.startsWith("s") && !s.startsWith("sc")) || s.startsWith("ms") || s == "d" || s == "sp" || s == "qa"
        } == true
        val currentlyInHeading = inHeading || triggersHeading

        if (item.isBlock()) pendingBreak = true

        if (item.type == "text") {
            item.text?.let { handleText(it, style, currentVerseId, currentlyInHeading) }
        }

        item.items?.forEach { process(it, currentVerseId, style, currentlyInHeading) }
    }

    private fun handleText(text: String, style: TextStyle, verseId: String?, inHeading: Boolean) {
        if (inHeading) {
            handleHeadingText(text, style)
        } else if (verseId != null) {
            handleVerseText(text, style, verseId)
        }
    }

    private fun handleHeadingText(text: String, style: TextStyle) {
        if (pendingBreak || currentHeadings.isEmpty()) {
            currentHeadings.add(mutableListOf())
            pendingBreak = false
        }
        val headingStyle = if (style == TextStyle.NORMAL) TextStyle.HEADING else style
        currentHeadings.last().add(TextSpan(text, headingStyle))
    }

    private fun handleVerseText(text: String, style: TextStyle, verseId: String?) {
        val verseNumber = verseId?.let { extractVerseNumber(it) } ?: 0
        if (verseNumber > 0) {
            attachPendingHeadings(verseNumber)
            val verseSpans = versesMap.getOrPut(verseNumber) { mutableListOf() }
            if (pendingBreak && verseSpans.isNotEmpty()) {
                verseSpans.add(TextSpan("\n", TextStyle.NORMAL))
            }
            pendingBreak = false
            verseSpans.add(TextSpan(text, style))
        }
    }

    private fun attachPendingHeadings(verseNumber: Int) {
        if (currentHeadings.isNotEmpty()) {
            headingsMap.getOrPut(verseNumber) { mutableListOf() }.addAll(currentHeadings)
            currentHeadings.clear()
            pendingBreak = true
        }
    }

    private fun buildVerses(): List<Verse> {
        val allVerseNumbers = (versesMap.keys + headingsMap.keys).distinct().sorted()
        return allVerseNumbers.map { number ->
            Verse(
                number = number,
                richText = (versesMap[number] ?: emptyList()).mergeAdjacent(),
                headings = (headingsMap[number] ?: emptyList()).map { it.mergeAdjacent() }
            )
        }
    }

    private fun AbsContentItemDto.getStyle(inherited: TextStyle): TextStyle = when {
        attrs?.style == "bd" || attrs?.style == "nd" || attrs?.style == "d" -> TextStyle.BOLD
        attrs?.style == "it" -> TextStyle.ITALIC
        attrs?.style == "itbd" -> TextStyle.ITALIC_BOLD
        attrs?.style?.startsWith("sc") == true -> TextStyle.SMALL_CAPS
        attrs?.style == "wj" -> TextStyle.WORDS_OF_JESUS
        attrs?.style?.let { s ->
            (s.startsWith("s") && !s.startsWith("sc")) || s.startsWith("ms") || s == "qa" || s == "sp"
        } == true -> TextStyle.HEADING
        else -> inherited
    }

    private fun AbsContentItemDto.isBlock(): Boolean = type == "tag" && attrs?.style?.let { s ->
        // Blocks trigger a structural break (pendingBreak = true).
        // Headings (s, ms, d, sp, qa) and paragraphs (p, q, m, nb) are blocks.
        // 'sc' (small-caps) is explicitly excluded as it is an inline style.
        s.startsWith("p") || s.startsWith("q") || (s.startsWith("s") && !s.startsWith("sc")) || s == "m" || s == "nb" || s == "d" || s == "sp" || s == "qa"
    } == true

    private fun extractVerseNumber(vId: String): Int {
        val parts = vId.split(".", " ", ":")
        if (parts.size < 3) return 0
        return parts.lastOrNull { it.any { c -> c.isDigit() } }
            ?.takeWhile { it.isDigit() }
            ?.toIntOrNull() ?: 0
    }

    private fun List<TextSpan>.mergeAdjacent(): List<TextSpan> {
        val merged = mutableListOf<TextSpan>()
        for (span in this) {
            val last = merged.lastOrNull()
            if (last != null && last.style == span.style) {
                merged[merged.size - 1] = last.copy(text = last.text + span.text)
            } else {
                merged.add(span)
            }
        }
        return merged
    }
}
