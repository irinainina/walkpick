package com.walkpick.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppSheet
import com.walkpick.app.ui.components.AppTextField
import com.walkpick.app.ui.theme.Spacing

// Longer than this a name does not fit the card of the diary in two lines.
private const val LONGEST_NAME = 40

/** A name to change: of a walk, of the walker; a blank name is not saved. */
@Composable
fun NameSheet(title: String, name: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(name) }
    val submit = { if (text.isNotBlank()) onSave(text.trim()) }
    AppSheet(title, onDismiss) {
        Spacer(Modifier.height(Spacing.m))
        AppTextField(value = text, onValueChange = { text = it.take(LONGEST_NAME) }, onDone = submit)
        Spacer(Modifier.height(Spacing.xl))
        AppButton(stringResource(R.string.profile_save), onClick = submit)
    }
}
