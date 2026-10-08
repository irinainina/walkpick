package com.walkspot.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.ui.PRIVACY_URL
import com.walkspot.app.ui.TERMS_URL
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.GradientHeader
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing

// Below the status bar: the frame draws 312 with a status bar of 24.
private val HeaderHeight = 288.dp
private val LogoTop = 32.dp
private val LogoSize = 64.dp
private val IllustrationHeight = 260.dp

// The illustration starts on the wave, this far above the bottom of the header.
private val IllustrationOverlap = 64.dp
private val LegalTop = 52.dp
private val LegalBottom = 36.dp

@Composable
fun SignInScreen(
    onGoogle: () -> Unit,
    onEmail: () -> Unit,
    modifier: Modifier = Modifier,
    // Why the last try did not sign anybody in; takes the place of the hint.
    problem: String? = null,
) {
    val colors = AppTheme.colors
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier
            .fillMaxSize()
            .background(colors.canvas),
    ) {
        Header(statusBar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBar + HeaderHeight - IllustrationOverlap)
                .navigationBarsPadding()
                .padding(horizontal = Spacing.ml),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The only part that gives way on a short screen.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = Spacing.xl),
                contentAlignment = Alignment.TopCenter,
            ) {
                Image(
                    painter = painterResource(R.drawable.ill_walk),
                    contentDescription = null,
                    modifier = Modifier
                        .heightIn(max = IllustrationHeight)
                        .fillMaxSize(),
                    alignment = Alignment.TopCenter,
                )
            }
            AppButton(
                text = stringResource(R.string.sign_in_google),
                onClick = onGoogle,
                style = AppButtonStyle.Secondary,
                iconRes = R.drawable.ic_google,
            )
            Spacer(Modifier.height(Spacing.ml))
            AppButton(
                text = stringResource(R.string.sign_in_email),
                onClick = onEmail,
                iconRes = R.drawable.ic_mail,
            )
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = problem ?: stringResource(R.string.sign_in_email_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = if (problem != null) colors.danger else colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LegalTop))
            val terms = stringResource(R.string.settings_terms)
            val privacy = stringResource(R.string.settings_privacy)
            val legal = stringResource(R.string.sign_in_legal, terms, privacy)
            val link = TextLinkStyles(SpanStyle(color = colors.accent))
            Text(
                text = buildAnnotatedString {
                    append(legal)
                    listOf(terms to TERMS_URL, privacy to PRIVACY_URL).forEach { (words, url) ->
                        val start = legal.indexOf(words)
                        addLink(LinkAnnotation.Url(url, link), start, start + words.length)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LegalBottom))
        }
    }
}

@Composable
private fun Header(statusBar: Dp) {
    val colors = AppTheme.colors
    GradientHeader(statusBar + HeaderHeight) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBar + LogoTop),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(LogoSize),
                tint = colors.onAccent,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = colors.onAccent,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.sign_in_tagline),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onAccent,
            )
        }
    }
}
