package com.walkpick.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.PRIVACY_URL
import com.walkpick.app.ui.TERMS_URL
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppButtonStyle
import com.walkpick.app.ui.components.DarkSystemBars
import com.walkpick.app.ui.components.photo
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.GlassRadius
import com.walkpick.app.ui.theme.GlassShape
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing

private val LogoTop = 32.dp
private val LogoSize = 64.dp
private val GlassBlur = 24.dp
private val LegalTop = 28.dp
private val LegalBottom = 36.dp

// How much of the photo the dark veil hides at the top, in the middle and at the bottom: the
// white words stand at the ends, the picture shows in between.
private const val VEIL_TOP = 0.55f
private const val VEIL_MIDDLE = 0.15f
private const val VEIL_BOTTOM = 0.5f

@Composable
fun SignInScreen(
    onGoogle: () -> Unit,
    onEmail: () -> Unit,
    modifier: Modifier = Modifier,
    // Why the last try did not sign anybody in; takes the place of the hint.
    problem: String? = null,
) {
    DarkSystemBars()
    var card by remember { mutableStateOf(Rect.Zero) }
    val radius = with(LocalDensity.current) { GlassRadius.toPx() }
    Box(modifier.fillMaxSize()) {
        Backdrop(Modifier.matchParentSize())
        // The glass: the same picture once more, blurred and cut to the card.
        Backdrop(
            Modifier
                .matchParentSize()
                .clip(GenericShape { _, _ -> addRoundRect(RoundRect(card, CornerRadius(radius))) })
                .blur(GlassBlur, BlurredEdgeTreatment.Unbounded),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = LogoTop),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Title()
            Spacer(Modifier.weight(1f))
            Entry(onGoogle, onEmail, problem, Modifier.onGloballyPositioned { card = it.boundsInRoot() })
            Spacer(Modifier.height(LegalTop))
            Legal()
            Spacer(Modifier.height(LegalBottom))
        }
    }
}

/** The photo of the season over the whole screen, under a veil that lets white text be read. */
@Composable
private fun Backdrop(modifier: Modifier = Modifier) {
    val veil = AppTheme.colors.photoGround
    Box(modifier) {
        Image(
            painter = painterResource(AppTheme.season.photo()),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = VEIL_TOP),
                        0.5f to veil.copy(alpha = VEIL_MIDDLE),
                        1f to veil.copy(alpha = VEIL_BOTTOM),
                    ),
                ),
        )
    }
}

@Composable
private fun Title() {
    val colors = AppTheme.colors
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

@Composable
private fun Entry(
    onGoogle: () -> Unit,
    onEmail: () -> Unit,
    problem: String?,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.s)
            .fillMaxWidth()
            .then(modifier)
            .background(colors.glass, GlassShape)
            .border(Sizes.hairline, colors.glassEdge, GlassShape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            color = if (problem != null) colors.danger else colors.onAccent,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Legal() {
    val colors = AppTheme.colors
    val terms = stringResource(R.string.settings_terms)
    val privacy = stringResource(R.string.settings_privacy)
    val legal = stringResource(R.string.sign_in_legal, terms, privacy)
    // Over a photo a link is told by its line, not by a colour.
    val link = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline))
    Text(
        text = buildAnnotatedString {
            append(legal)
            listOf(terms to TERMS_URL, privacy to PRIVACY_URL).forEach { (words, url) ->
                val start = legal.indexOf(words)
                addLink(LinkAnnotation.Url(url, link), start, start + words.length)
            }
        },
        modifier = Modifier.padding(horizontal = Spacing.ml),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onAccent,
        textAlign = TextAlign.Center,
    )
}
