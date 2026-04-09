package org.tjc.bible.presentation.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * A Text composable that automatically resizes its font size to fit the available width.
 * This implementation uses [BoxWithConstraints] and [rememberTextMeasurer] to calculate
 * the fitting font size synchronously during composition. This ensures the text is
 * always rendered at the correct size and updates immediately when the [text] or 
 * constraints change, without flickering.
 */
@Composable
fun AutoResizedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val maxWidth = constraints.maxWidth

        // Calculate the resized style synchronously during composition/layout.
        // We use all parameters that affect layout as keys for remember to ensure
        // the text updates immediately when any input changes.
        val resizedTextStyle = remember(
            text, style, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, maxWidth, maxLines
        ) {
            var combinedStyle = style
            if (!fontSize.isUnspecified) combinedStyle = combinedStyle.copy(fontSize = fontSize)
            if (fontStyle != null) combinedStyle = combinedStyle.copy(fontStyle = fontStyle)
            if (fontWeight != null) combinedStyle = combinedStyle.copy(fontWeight = fontWeight)
            if (fontFamily != null) combinedStyle = combinedStyle.copy(fontFamily = fontFamily)
            if (!letterSpacing.isUnspecified) combinedStyle = combinedStyle.copy(letterSpacing = letterSpacing)
            if (textDecoration != null) combinedStyle = combinedStyle.copy(textDecoration = textDecoration)
            if (textAlign != null) combinedStyle = combinedStyle.copy(textAlign = textAlign)
            if (!lineHeight.isUnspecified) combinedStyle = combinedStyle.copy(lineHeight = lineHeight)

            if (maxWidth <= 0 || maxWidth == Int.MAX_VALUE || text.isEmpty()) {
                return@remember combinedStyle
            }

            val startFontSize = if (combinedStyle.fontSize.isUnspecified) 16.sp else combinedStyle.fontSize
            var bestFontSize = startFontSize

            // Check if it fits at the start size first (fast path)
            val initialMeasure = textMeasurer.measure(
                text = text,
                style = combinedStyle.copy(fontSize = startFontSize),
                maxLines = maxLines,
                softWrap = false
            )

            if (initialMeasure.size.width > maxWidth) {
                var low = 8f
                var high = startFontSize.value
                
                // Binary search for the largest fitting font size
                repeat(10) {
                    val mid = (low + high) / 2
                    val measure = textMeasurer.measure(
                        text = text,
                        style = combinedStyle.copy(fontSize = mid.sp),
                        maxLines = maxLines,
                        softWrap = false
                    )
                    if (measure.size.width <= maxWidth) {
                        bestFontSize = mid.sp
                        low = mid
                    } else {
                        high = mid
                    }
                }
            }
            combinedStyle.copy(fontSize = bestFontSize)
        }

        Text(
            text = text,
            color = color,
            overflow = overflow,
            softWrap = false,
            maxLines = maxLines,
            minLines = minLines,
            style = resizedTextStyle,
            onTextLayout = onTextLayout
        )
    }
}
