package com.walkspot.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing

// Six is what Supabase is set to send.
const val CODE_LENGTH = 6

/**
 * A one-line field that takes the focus as it appears unless [autoFocus] is off; [error] goes under it and
 * turns the border red.
 * [label] stands above it, [suffix] names the unit at its right edge.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: () -> Unit = {},
    error: String? = null,
    container: Color = AppTheme.colors.canvas,
    autoFocus: Boolean = true,
) {
    val colors = AppTheme.colors
    val type = MaterialTheme.typography
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (autoFocus) focus.requestFocus() }
    Column(modifier.fillMaxWidth()) {
        if (label != null) FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .onFocusChanged { focused = it.isFocused },
            singleLine = true,
            textStyle = type.titleLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { field ->
                val border = when {
                    error != null -> colors.danger
                    focused -> colors.accent
                    else -> colors.divider
                }
                FieldFrame(container, border, if (focused) Sizes.focusBorder else Sizes.hairline) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = type.titleLarge, color = colors.textSecondary)
                        }
                        field()
                    }
                    if (suffix != null) Text(suffix, style = type.bodyMedium, color = colors.textSecondary)
                }
            },
        )
        if (error != null) {
            Spacer(Modifier.height(Spacing.s))
            FieldError(error)
        }
    }
}

/** Looks like a field, but a tap opens a picker instead of the keyboard. */
@Composable
fun PickerField(
    text: String,
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        FieldFrame(
            container = colors.canvas,
            border = colors.divider,
            borderWidth = Sizes.hairline,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onClick),
        ) {
            val style = MaterialTheme.typography.titleLarge
            Text(text, Modifier.weight(1f), style = style, color = colors.textPrimary)
            Icon(painterResource(iconRes), contentDescription = null, tint = colors.textSecondary)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
    Spacer(Modifier.height(Spacing.xs))
}

@Composable
private fun FieldFrame(
    container: Color,
    border: Color,
    borderWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .height(Sizes.control)
            .background(container, MaterialTheme.shapes.medium)
            .border(borderWidth, border, MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
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
