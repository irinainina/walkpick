package com.walkpick.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.local.FindKind
import com.walkpick.app.ui.camera.CameraState
import com.walkpick.app.ui.components.CircleButton
import com.walkpick.app.ui.components.DarkSystemBars
import com.walkpick.app.ui.components.PhotoBox
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.CardShape
import com.walkpick.app.ui.theme.PillShape
import com.walkpick.app.ui.theme.Spacing

private val HintEdge = 40.dp
private val HintHeight = 36.dp
private val FocusSize = 196.dp
private val FocusArm = 32.dp
private val FocusLine = 3.dp
private val BarTop = 36.dp
private val BarBottom = 52.dp
private val Shutter = 72.dp
private val ShutterRing = 4.dp
private val ShutterInner = 56.dp
private val FlipSize = 48.dp
private val FlipEdge = 48.dp
private val CardHeight = 96.dp
private val Spinner = 40.dp
private val SpinnerRing = 32.dp
private val SpinnerLine = 4.dp
private const val SPINNER_ARC = 90f
private const val TURN_MS = 900
private const val FULL_TURN = 360f
private const val SHUTTER_PRESSED = 0.8f

/**
 * The camera of a walk; [viewfinder] is the live picture, laid under everything else. A photo just
 * taken covers it until the walker says what it shows.
 */
@Composable
fun CameraScreen(
    state: CameraState,
    onClose: () -> Unit,
    onKind: (FindKind) -> Unit,
    onFlash: () -> Unit,
    onZoom: () -> Unit,
    onFlip: () -> Unit,
    onShutter: () -> Unit,
    modifier: Modifier = Modifier,
    viewfinder: @Composable () -> Unit,
) {
    DarkSystemBars()
    val colors = AppTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(colors.photoGround),
    ) {
        viewfinder()
        state.shot?.photo?.let { PhotoBox(it, Modifier.fillMaxSize(), ground = colors.photoGround) }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(start = Spacing.ml, top = Spacing.m, end = Spacing.ml),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CircleButton(
                    iconRes = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.camera_close),
                    onClick = onClose,
                    tint = colors.onAccent,
                    ground = colors.photoButton,
                )
                if (state.shot == null) {
                    CircleButton(
                        iconRes = R.drawable.ic_flash,
                        contentDescription = stringResource(R.string.camera_flash),
                        onClick = onFlash,
                        tint = if (state.flash) colors.textPrimary else colors.onAccent,
                        ground = if (state.flash) colors.surface else colors.photoButton,
                    )
                }
            }
            when {
                state.identifying -> {
                    Spacer(Modifier.weight(1f))
                    IdentifyingCard()
                }
                state.shot != null -> {
                    Spacer(Modifier.weight(1f))
                    KindAsk(onKind)
                }
                else -> {
                    Spacer(Modifier.height(Spacing.l))
                    Hint(if (state.noPlace) R.string.camera_no_place else R.string.camera_hint)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { FocusFrame() }
                    BottomBar(state.zoom, state.saving, onZoom, onShutter, onFlip)
                }
            }
        }
    }
}

@Composable
private fun Hint(textRes: Int) {
    Box(
        modifier = Modifier
            .padding(horizontal = HintEdge)
            .fillMaxWidth()
            .height(HintHeight)
            .background(AppTheme.colors.photoHint, PillShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.onAccent,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// Four corners of a square: where to keep the subject.
@Composable
private fun FocusFrame() {
    val color = AppTheme.colors.onAccent
    Canvas(Modifier.size(FocusSize)) {
        val arm = FocusArm.toPx()
        val line = Stroke(FocusLine.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        listOf(0f to 0f, size.width to 0f, size.width to size.height, 0f to size.height).forEach { (x, y) ->
            val inward = Offset(if (x == 0f) arm else -arm, if (y == 0f) arm else -arm)
            val corner = Path().apply {
                moveTo(x + inward.x, y)
                lineTo(x, y)
                lineTo(x, y + inward.y)
            }
            drawPath(corner, color, style = line)
        }
    }
}

@Composable
private fun BottomBar(
    zoom: Int,
    busy: Boolean,
    onZoom: () -> Unit,
    onShutter: () -> Unit,
    onFlip: () -> Unit,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.photoBar)
            .navigationBarsPadding()
            .padding(top = BarTop, bottom = BarBottom),
        contentAlignment = Alignment.Center,
    ) {
        val times = stringResource(R.string.camera_zoom_times, zoom)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = FlipEdge)
                .size(FlipSize)
                .background(colors.photoButton, CircleShape)
                .clip(CircleShape)
                .clickable(onClickLabel = stringResource(R.string.camera_zoom, times), onClick = onZoom),
            contentAlignment = Alignment.Center,
        ) {
            Text(times, style = MaterialTheme.typography.labelMedium, color = colors.onAccent)
        }
        val press = remember { MutableInteractionSource() }
        val pressed by press.collectIsPressedAsState()
        val inner by animateFloatAsState(if (pressed || busy) SHUTTER_PRESSED else 1f)
        Box(
            modifier = Modifier
                .size(Shutter)
                .border(ShutterRing, colors.onAccent, CircleShape)
                .clickable(
                    interactionSource = press,
                    indication = null,
                    enabled = !busy,
                    onClickLabel = stringResource(R.string.camera_shutter),
                    role = Role.Button,
                    onClick = onShutter,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(ShutterInner)
                    .scale(inner)
                    .background(colors.onAccent, CircleShape),
            )
        }
        CircleButton(
            iconRes = R.drawable.ic_flip,
            contentDescription = stringResource(R.string.camera_flip),
            onClick = onFlip,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = FlipEdge),
            tint = colors.onAccent,
            ground = colors.photoButton,
            size = FlipSize,
        )
    }
}

@Composable
private fun IdentifyingCard() {
    val colors = AppTheme.colors
    val type = MaterialTheme.typography
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = Spacing.ml, end = Spacing.ml, bottom = Spacing.xl)
            .fillMaxWidth()
            .height(CardHeight)
            .background(colors.surface, CardShape)
            .padding(horizontal = Spacing.ml),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val turn by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = FULL_TURN,
            animationSpec = infiniteRepeatable(tween(TURN_MS, easing = LinearEasing)),
        )
        Canvas(Modifier.size(Spinner)) {
            val edge = (Spinner - SpinnerRing).toPx() / 2
            val ring = Size(SpinnerRing.toPx(), SpinnerRing.toPx())
            val line = Stroke(SpinnerLine.toPx(), cap = StrokeCap.Round)
            val corner = Offset(edge, edge)
            drawArc(colors.surfaceTint, 0f, FULL_TURN, false, corner, ring, style = line)
            // The arc starts at the top, as in the design.
            drawArc(colors.accent, turn - SPINNER_ARC, SPINNER_ARC, false, corner, ring, style = line)
        }
        Spacer(Modifier.width(Spacing.m))
        Column {
            val title = stringResource(R.string.camera_identifying)
            Text(title, style = type.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(Spacing.xs))
            val saved = stringResource(R.string.camera_photo_saved)
            Text(saved, style = type.bodyMedium, color = colors.textSecondary)
        }
    }
}
