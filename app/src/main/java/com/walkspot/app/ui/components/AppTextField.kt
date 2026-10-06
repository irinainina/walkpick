package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing

// Six is what Supabase is set to send.
const val CODE_LENGTH = 6

/** A one-line field that takes the focus as it appears; [error] goes under it and turns the border red. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: () -> Unit = {},
    error: String? = null,
) {
    val colors = AppTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { field ->
                Box(
                    modifier = Modifier
                        .height(Sizes.control)
                        .background(colors.surface, MaterialTheme.shapes.medium)
                        .border(
                            Sizes.hairline,
                            if (error != null) colors.danger else colors.divider,
                            MaterialTheme.shapes.medium,
                        )
                        .padding(horizontal = Spacing.m),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary,
                        )
                    }
                    field()
                }
            },
        )
        if (error != null) FieldError(error)
    }
}

/** Six cells for the code from the letter; [onComplete] fires as the last digit lands. */
@Composable
fun CodeField(
    value: String,
    onValueChange: (String) -> Unit,
    onComplete: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val colors = AppTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        BasicTextField(
            value = value,
            onValueChange = { typed ->
                val digits = typed.filter(Char::isDigit).take(CODE_LENGTH)
                onValueChange(digits)
                if (digits.length == CODE_LENGTH && digits != value) onComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .semantics { this.contentDescription = contentDescription },
            // Number, not NumberPassword: the phone may then offer the code that has just arrived.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            // The cells draw the digits; the field itself stays unseen.
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { field ->
                Box {
                    field()
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        repeat(CODE_LENGTH) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(Sizes.control)
                                    .background(colors.surface, MaterialTheme.shapes.medium)
                                    .border(
                                        Sizes.hairline,
                                        if (error != null) colors.danger else colors.divider,
                                        MaterialTheme.shapes.medium,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = value.getOrNull(index)?.toString().orEmpty(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colors.textPrimary,
                                )
                            }
                        }
                    }
                }
            },
        )
        if (error != null) FieldError(error)
    }
}

@Composable
private fun FieldError(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = AppTheme.colors.danger,
        textAlign = TextAlign.Start,
    )
}
