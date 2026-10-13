package com.walkpick.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.walkpick.app.R
import com.walkpick.app.data.account.AuthProblem
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppTextField
import com.walkpick.app.ui.components.CircleButton
import com.walkpick.app.ui.components.CodeField
import com.walkpick.app.ui.components.GlassScreen
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Sizes
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
            color = AppTheme.colors.onAccent,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.s))
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
                .padding(top = Spacing.m),
            style = with(MaterialTheme.typography) { if (waiting) bodyMedium else titleSmall },
            color = AppTheme.colors.onAccent,
            textAlign = TextAlign.Center,
        )
    }
}

/** A step of the sign-in: the back button over the photo and a glass card with the step in it. */
@Composable
private fun SignInStep(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassScreen { glass ->
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                // From Android 15 the window is not resized for the keyboard any more.
                .imePadding(),
        ) {
            Box(
                Modifier
                    .height(Sizes.control)
                    .padding(horizontal = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                CircleButton(
                    iconRes = R.drawable.ic_back,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    tint = AppTheme.colors.onAccent,
                    ground = AppTheme.colors.photoChrome,
                )
            }
            Spacer(Modifier.height(Spacing.m))
            Column(
                Modifier
                    .padding(horizontal = Spacing.s)
                    .fillMaxWidth()
                    .then(glass)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.l),
            ) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = AppTheme.colors.onAccent)
                Spacer(Modifier.height(Spacing.s))
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onAccent)
                Spacer(Modifier.height(Spacing.l))
                content()
            }
        }
    }
}
