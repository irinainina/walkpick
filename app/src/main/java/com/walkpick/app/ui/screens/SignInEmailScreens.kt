package com.walkpick.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.walkpick.app.R
import com.walkpick.app.data.account.AuthProblem
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppTextField
import com.walkpick.app.ui.components.AppTopBar
import com.walkpick.app.ui.components.CodeField
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing

@StringRes
fun AuthProblem.messageRes(): Int = when (this) {
    AuthProblem.InvalidEmail -> R.string.auth_email_invalid
    AuthProblem.Offline -> R.string.auth_offline
    AuthProblem.WrongCode -> R.string.auth_code_wrong
    AuthProblem.TooOften -> R.string.auth_too_often
    AuthProblem.NoGoogle -> R.string.auth_no_google
    AuthProblem.NoAnswer -> R.string.auth_no_answer
}

@Composable
fun SignInEmailScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    busy: Boolean,
    problem: AuthProblem?,
) {
    SignInStep(
        title = stringResource(R.string.auth_email_title),
        subtitle = stringResource(R.string.sign_in_email_hint),
        onBack = onBack,
    ) {
        AppTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = stringResource(R.string.auth_email_label),
            keyboardType = KeyboardType.Email,
            onDone = onContinue,
            error = problem?.let { stringResource(it.messageRes()) },
            container = AppTheme.colors.surface,
        )
        Spacer(Modifier.height(Spacing.ml))
        AppButton(
            text = stringResource(R.string.auth_send_code),
            onClick = { if (!busy) onContinue() },
        )
    }
}

/** Checked as the last digit lands, so there is no button. */
@Composable
fun SignInCodeScreen(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    onCodeComplete: () -> Unit,
    /** The seconds until another code may be asked for. */
    resendIn: Int,
    onResend: () -> Unit,
    onBack: () -> Unit,
    problem: AuthProblem?,
) {
    SignInStep(
        title = stringResource(R.string.auth_code_title),
        subtitle = stringResource(R.string.auth_code_subtitle, email),
        onBack = onBack,
    ) {
        CodeField(
            value = code,
            onValueChange = onCodeChange,
            onComplete = onCodeComplete,
            contentDescription = stringResource(R.string.auth_code_label),
            error = problem?.let { stringResource(it.messageRes()) },
        )
        Spacer(Modifier.height(Spacing.m))
        Text(
            text = stringResource(R.string.auth_code_slow),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        val waiting = resendIn > 0
        Text(
            text = if (waiting) {
                stringResource(R.string.auth_resend_in, resendIn)
            } else {
                stringResource(R.string.auth_resend)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !waiting, onClick = onResend)
                .padding(vertical = Spacing.m),
            style = MaterialTheme.typography.titleSmall,
            color = if (waiting) AppTheme.colors.textSecondary else AppTheme.colors.accent,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SignInStep(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            // From Android 15 the window is not resized for the keyboard any more.
            .imePadding(),
    ) {
        AppTopBar(title = "", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = Spacing.ml),
        ) {
            Spacer(Modifier.height(Spacing.m))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = AppTheme.colors.textPrimary)
            Spacer(Modifier.height(Spacing.s))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary)
            Spacer(Modifier.height(Spacing.l))
            content()
        }
    }
}
