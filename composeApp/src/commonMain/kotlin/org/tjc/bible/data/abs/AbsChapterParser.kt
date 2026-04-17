package org.tjc.bible.data.abs

import org.tjc.bible.domain.model.TextSpan
import org.tjc.bible.domain.model.TextStyle
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.model.VerseElement

/**
 * Parser for API.Bible (from American Bible Society)
 */
internal class AbsChapterParser {
    private val verseElementsMap = mutableMapOf<Int, MutableList<VerseElement>>()
    private val currentHeadings = mutableListOf<MutableList<TextSpan>>()
    private var pendingBreak = false

    /**
     * Parses the ABS chapter content DTO into a list of domain [Verse] objects.
     */
    fun parse(dto: AbsChapterContentDto): List<Verse> {
        dto.content.forEach { process(it, null, TextStyle.NORMAL, false) }
        return buildVerses()
    }

    /**
     * Recursively processes each content item, maintaining state for verse IDs, styles, and heading status.
     */
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

    /**
     * Cleans raw text and routes it to either heading or verse text handlers.
     */
    private fun handleText(text: String, style: TextStyle, verseId: String?, inHeading: Boolean) {
        val cleanedText = text.replace("\u00B6", "").replace("\r", "")
        if (cleanedText.isEmpty()) return

        val verseNumber = verseId?.let { extractVerseNumber(it) } ?: 0
        if (inHeading) {
            handleHeadingText(cleanedText, style, verseNumber)
        } else if (verseNumber > 0) {
            handleVerseText(cleanedText, style, verseNumber)
        }
    }

    /**
     * Collects text belonging to a heading, creating new heading blocks as needed.
     */
    private fun handleHeadingText(text: String, style: TextStyle, verseNumber: Int) {
        if (pendingBreak || currentHeadings.isEmpty()) {
            currentHeadings.add(mutableListOf())
        }

        val headingStyle = if (style == TextStyle.NORMAL) TextStyle.HEADING else style
        appendTextToSpans(currentHeadings.last(), text, headingStyle, addNewline = false)
    }

    /**
     * Appends text to the current verse, ensuring pending headings are attached first.
     */
    private fun handleVerseText(text: String, style: TextStyle, verseNumber: Int) {
        attachPendingHeadings(verseNumber)

        val elements = verseElementsMap.getOrPut(verseNumber) { mutableListOf() }
        val lastElement = elements.lastOrNull() as? VerseElement.Text
        val lastSpans = lastElement?.spans?.toMutableList() ?: mutableListOf()

        appendTextToSpans(lastSpans, text, style, addNewline = true)

        val newElement = VerseElement.Text(lastSpans.mergeAdjacent())
        if (lastElement != null) {
            elements[elements.size - 1] = newElement
        } else {
            elements.add(newElement)
        }
    }

    /**
     * Core logic for appending text to a list of spans, handling smart spacing and structural breaks.
     */
    private fun appendTextToSpans(spans: MutableList<TextSpan>, text: String, style: TextStyle, addNewline: Boolean) {
        val textToAdd = if (!pendingBreak && spans.isNotEmpty() && shouldAddSpace(spans.last().text, text)) {
            " $text"
        } else {
            text
        }

        if (pendingBreak && spans.isNotEmpty() && addNewline) {
            spans.add(TextSpan("\n", TextStyle.NORMAL))
        }

        pendingBreak = false
        spans.add(TextSpan(textToAdd, style))
    }

    /**
     * Determines if a space should be inserted between two text segments based on punctuation and language.
     */
    private fun shouldAddSpace(prev: String, next: String): Boolean {
        if (prev.isEmpty() || next.isEmpty()) return false
        val lastChar = prev.last()
        val firstChar = next.first()

        if (lastChar.isWhitespace() || firstChar.isWhitespace()) return false
        if (",.?!:;)]}\"".contains(lastChar)) return false
        if ("([{".contains(firstChar)) return false

        // Only add space for "character-based" (alphabetical) languages, not ideographic ones.
        return !lastChar.isIdeographic() && !firstChar.isIdeographic()
    }

    /**
     * Checks if a character belongs to a CJK (Chinese, Japanese, Korean) ideographic range.
     */
    private fun Char.isIdeographic(): Boolean =
        code in 0x4E00..0x9FFF || // CJK Unified Ideographs
        code in 0x3400..0x4DBF || // CJK Unified Ideographs Extension A
        code in 0x3040..0x30FF || // Japanese Hiragana and Katakana
        code in 0xAC00..0xD7AF    // Korean Hangul Syllables

    /**
     * Moves any headings collected since the last verse and attaches them to the specified verse number.
     */
    private fun attachPendingHeadings(verseNumber: Int) {
        if (currentHeadings.isNotEmpty()) {
            val elements = verseElementsMap.getOrPut(verseNumber) { mutableListOf() }
            currentHeadings.forEach { spans ->
                elements.add(VerseElement.Heading(spans.mergeAdjacent()))
            }
            currentHeadings.clear()
            pendingBreak = false // Reset pendingBreak after a heading block
        }
    }

    /**
     * Finalizes the parsed data into a sorted list of [Verse] objects.
     */
    private fun buildVerses(): List<Verse> {
        // Attach any trailing headings to the last verse found
        if (currentHeadings.isNotEmpty()) {
            verseElementsMap.keys.maxOrNull()?.let { lastVerse ->
                attachPendingHeadings(lastVerse)
            }
        }

        return verseElementsMap.keys.sorted().map { number ->
            Verse(
                number = number,
                elements = verseElementsMap[number] ?: emptyList()
            )
        }
    }

    /**
     * Maps ABS style attributes to domain [TextStyle] constants.
     */
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

    /**
     * Identifies if a content item represents a structural block (like a paragraph or heading).
     */
    private fun AbsContentItemDto.isBlock(): Boolean = type == "tag" && attrs?.style?.let { s ->
        // Blocks trigger a structural break (pendingBreak = true).
        // Headings (s, ms, d, sp, qa) and paragraphs (p, q, m, nb) are blocks.
        // 'sc' (small-caps) is explicitly excluded as it is an inline style.
        s.startsWith("p") || s.startsWith("q") || (s.startsWith("s") && !s.startsWith("sc")) || s == "m" || s == "nb" || s == "d" || s == "sp" || s == "qa"
    } == true

    /**
     * Extracts the numeric verse number from a standard ABS verse ID string (e.g., "GEN.1.1").
     */
    private fun extractVerseNumber(vId: String): Int {
        val parts = vId.split(".", " ", ":")
        return parts.lastOrNull { it.any { c -> c.isDigit() } }
            ?.takeWhile { it.isDigit() }
            ?.toIntOrNull() ?: 0
    }

    /**
     * Merges adjacent [TextSpan] objects that share the same style to simplify the domain model.
     */
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
