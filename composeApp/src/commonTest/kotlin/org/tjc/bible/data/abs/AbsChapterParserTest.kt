package org.tjc.bible.data.abs

import org.tjc.bible.domain.model.VerseElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbsChapterParserTest {

    @Test
    fun parseLetterInActs15WithVerseMarkers() {
        val dto = AbsChapterContentDto(
            id = "ACT.15",
            number = "15",
            content = listOf(
                AbsContentItemDto(
                    type = "tag",
                    name = "para",
                    attrs = AbsContentAttrsDto(style = "p"),
                    items = listOf(
                        AbsContentItemDto(
                            type = "tag",
                            name = "verse",
                            attrs = AbsContentAttrsDto(style = "v", number = "23", sid = "ACT 15:23"),
                            items = listOf(AbsContentItemDto(type = "text", text = "23"))
                        ),
                        AbsContentItemDto(
                            type = "text",
                            text = "and they sent this letter with them:",
                            attrs = AbsContentAttrsDto(verseId = "ACT.15.23")
                        )
                    )
                ),
                AbsContentItemDto(
                    type = "tag",
                    name = "para",
                    attrs = AbsContentAttrsDto(style = "lim", vid = "ACT 15:23"),
                    items = listOf(
                        AbsContentItemDto(
                            type = "text",
                            text = "“The apostles and the brothers who are elders, to the brothers in Antioch: Greetings."
                        ),
                        AbsContentItemDto(
                            type = "tag",
                            name = "verse",
                            attrs = AbsContentAttrsDto(style = "v", number = "24", sid = "ACT 15:24"),
                            items = listOf(AbsContentItemDto(type = "text", text = "24"))
                        ),
                        AbsContentItemDto(
                            type = "text",
                            text = "Since we have heard that some of our number have confused you..."
                        ),
                        AbsContentItemDto(
                            type = "tag",
                            name = "verse",
                            attrs = AbsContentAttrsDto(style = "v", number = "25", sid = "ACT 15:25"),
                            items = listOf(AbsContentItemDto(type = "text", text = "25"))
                        ),
                        AbsContentItemDto(
                            type = "text",
                            text = "it seemed good to us..."
                        )
                    )
                )
            )
        )

        val parser = AbsChapterParser()
        val verses = parser.parse(dto)

        assertEquals(3, verses.size)
        assertEquals(listOf(23, 24, 25), verses.map { it.number })

        val verse23 = verses.find { it.number == 23 }!!
        val verse24 = verses.find { it.number == 24 }!!
        val verse25 = verses.find { it.number == 25 }!!

        assertTrue(verse23.elements.isNotEmpty())
        assertTrue(verse24.elements.isNotEmpty())
        assertTrue(verse25.elements.isNotEmpty())
    }

    @Test
    fun parseEzekiel21_30MissingSpace() {
        val dto = AbsChapterContentDto(
            id = "EZK.21",
            number = "21",
            content = listOf(
                AbsContentItemDto(
                    type = "tag",
                    name = "para",
                    attrs = AbsContentAttrsDto(style = "p"),
                    items = listOf(
                        AbsContentItemDto(
                            type = "tag",
                            name = "verse",
                            attrs = AbsContentAttrsDto(style = "v", number = "30", sid = "EZK 21:30"),
                            items = listOf(AbsContentItemDto(type = "text", text = "30"))
                        ),
                        AbsContentItemDto(
                            type = "text",
                            text = "Return",
                            attrs = AbsContentAttrsDto(verseId = "EZK.21.30")
                        ),
                        AbsContentItemDto(
                            type = "tag",
                            name = "span",
                            attrs = AbsContentAttrsDto(style = "wj", verseId = "EZK.21.30"),
                            items = listOf(AbsContentItemDto(type = "text", text = "it"))
                        )
                    )
                )
            )
        )

        val parser = AbsChapterParser()
        val verses = parser.parse(dto)

        assertEquals(1, verses.size)
        val verse30 = verses[0]
        assertEquals(30, verse30.number)

        val textContent = verse30.elements.joinToString("") { element ->
            when (element) {
                is VerseElement.Text -> element.spans.joinToString("") { it.text }
                is VerseElement.Heading -> ""
            }
        }
        assertTrue(textContent.contains("Return it"))
    }
}
