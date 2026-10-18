package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.TextStyle
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.model.VerseElement
import org.tjc.bible.presentation.ui.nameResource

@Composable
internal fun ChapterHeader(book: Book, chapter: Int, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "${stringResource(book.nameResource)} $chapter",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Serif
        )
    }
}

@Composable
internal fun VerseItem(verse: Verse, showWordsOfJesus: Boolean) {
    val isNonAlphabetLanguage = verse.elements.any { element ->
        when (element) {
            is VerseElement.Text -> element.spans.any { it.text.any { char -> char.code in 0x4E00..0x9FFF } }
            is VerseElement.Heading -> element.spans.any { it.text.any { char -> char.code in 0x4E00..0x9FFF } }
        }
    }
    val defaultFontFamily = if (isNonAlphabetLanguage) FontFamily.SansSerif else FontFamily.Serif

    Column {
        if (verse.versionAbbreviation != null) {
            Text(
                text = verse.versionAbbreviation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        verse.elements.forEachIndexed { index, element ->
            when (element) {
                is VerseElement.Heading -> {
                    val headingText = buildAnnotatedString {
                        element.spans.forEach { span ->
                            val text = if (span.style == TextStyle.SMALL_CAPS) span.text.uppercase() else span.text
                            withStyle(span.style.toSpanStyle(SpanStyle(fontFamily = defaultFontFamily), showWordsOfJesus)) {
                                append(text)
                            }
                        }
                    }
                    Text(
                        text = headingText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            lineHeight = 24.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        modifier = Modifier.padding(top = if (index == 0) 4.dp else 16.dp, bottom = 6.dp)
                    )
                }
                is VerseElement.Text -> {
                    val annotatedString = buildAnnotatedString {
                        if (element == verse.elements.firstOrNull { it is VerseElement.Text }) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("${verse.number}  ")
                            }
                        }

                        val baseStyle = SpanStyle(fontFamily = defaultFontFamily)
                        element.spans.forEach { span ->
                            val text = if (span.style == TextStyle.SMALL_CAPS) span.text.uppercase() else span.text
                            withStyle(span.style.toSpanStyle(baseStyle, showWordsOfJesus)) {
                                append(text)
                            }
                        }
                    }
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

internal fun TextStyle.toSpanStyle(baseStyle: SpanStyle, showWordsOfJesus: Boolean) =
    when (this) {
        TextStyle.BOLD -> baseStyle.copy(
            fontWeight = FontWeight.Bold
        )

        TextStyle.ITALIC -> baseStyle.copy(
            fontStyle = FontStyle.Italic
        )

        TextStyle.ITALIC_BOLD -> baseStyle.copy(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic
        )

        TextStyle.HEADING -> baseStyle.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        TextStyle.SMALL_CAPS -> baseStyle

        TextStyle.WORDS_OF_JESUS -> if (showWordsOfJesus) {
            baseStyle.copy(color = Color.Red)
        } else {
            baseStyle
        }

        TextStyle.WORDS_OF_JESUS_BOLD -> if (showWordsOfJesus) {
            baseStyle.copy(color = Color.Red, fontWeight = FontWeight.Bold)
        } else {
            baseStyle.copy(fontWeight = FontWeight.Bold)
        }

        TextStyle.WORDS_OF_JESUS_ITALIC -> if (showWordsOfJesus) {
            baseStyle.copy(color = Color.Red, fontStyle = FontStyle.Italic)
        } else {
            baseStyle.copy(fontStyle = FontStyle.Italic)
        }

        TextStyle.WORDS_OF_JESUS_ITALIC_BOLD -> if (showWordsOfJesus) {
            baseStyle.copy(color = Color.Red, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
        } else {
            baseStyle.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
        }

        TextStyle.NORMAL -> baseStyle
    }
