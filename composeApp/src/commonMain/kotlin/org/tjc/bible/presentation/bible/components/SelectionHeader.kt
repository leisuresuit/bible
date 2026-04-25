package org.tjc.bible.presentation.bible.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.arrow_back
import bible.composeapp.generated.resources.arrow_forward
import bible.composeapp.generated.resources.next
import bible.composeapp.generated.resources.previous
import bible.composeapp.generated.resources.sort
import bible.composeapp.generated.resources.toggle_sort_order
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tjc.bible.presentation.ui.AutoResizedText

@Composable
fun SelectionHeader(
    title: String,
    searchHint: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSortButton: Boolean = false,
    onSortClick: () -> Unit = {},
    sortIcon: Painter? = null,
    sortDescription: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    titleWeight: Float? = null,
    requestFocus: Boolean = true,
    showPreviousButton: Boolean = false,
    previousButtonEnabled: Boolean = true,
    onPreviousClick: () -> Unit = {},
    showNextButton: Boolean = false,
    nextButtonEnabled: Boolean = true,
    onNextClick: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AutoResizedText(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = if (titleWeight != null) Modifier.weight(titleWeight) else Modifier
            )
            if (showSortButton) {
                IconButton(onClick = onSortClick) {
                    Icon(
                        painter = sortIcon ?: painterResource(Res.drawable.sort),
                        contentDescription = sortDescription ?: stringResource(Res.string.toggle_sort_order),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            ClearableTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = searchHint,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                focusRequester = focusRequester
            )
            if (showPreviousButton) {
                IconButton(
                    onClick = onPreviousClick,
                    enabled = previousButtonEnabled
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_back),
                        contentDescription = stringResource(Res.string.previous)
                    )
                }
            }
            if (showNextButton) {
                IconButton(
                    onClick = onNextClick,
                    enabled = nextButtonEnabled
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_forward),
                        contentDescription = stringResource(Res.string.next)
                    )
                }
            }
        }
    }
}
