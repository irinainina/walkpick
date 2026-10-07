package com.walkspot.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.ui.distanceText
import com.walkspot.app.ui.grouped
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.SheetShape
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing
import com.walkspot.app.ui.walk.ActiveWalkState
import kotlin.math.roundToInt

private val HandleWidth = 40.dp
private val SideButton = 64.dp
private val CameraButton = 80.dp
private val CameraIcon = 32.dp
private const val SECONDS_IN_MINUTE = 60
private const val NOTHING = "–"

/** "32:15", and "1:02:15" from the first hour. */
internal fun clockText(seconds: Long): String {
    val minutes = seconds / SECONDS_IN_MINUTE
    val tail = "%02d:%02d".format(minutes % SECONDS_IN_MINUTE, seconds % SECONDS_IN_MINUTE)
    val hours = minutes / SECONDS_IN_MINUTE
    return if (hours > 0) "$hours:$tail" else tail.removePrefix("0")
}

/** The numbers of the walk and its three buttons, lying over the bottom of the map. */
@Composable
internal fun ActiveWalkPanel(
    state: ActiveWalkState,
    onPause: () -> Unit,
    onCamera: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(SheetShape, AppShadows.panel)
            .background(colors.surface, SheetShape)
            .navigationBarsPadding()
            .padding(top = Spacing.s, bottom = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(HandleWidth, Spacing.xs).background(colors.divider, PillShape))
        Spacer(Modifier.height(Spacing.m))
        Row(Modifier.padding(horizontal = Spacing.ml), verticalAlignment = Alignment.CenterVertically) {
            val unit = state.distanceUnit
            // The pace of a mile is the pace of a kilometre stretched to the mile.
            val pace = state.paceSecondsPerKm?.let { clockText((it / unit.perMetric).roundToInt().toLong()) }
            Stat(grouped(state.steps), stringResource(R.string.walk_steps))
            Divider()
            Stat(distanceText(state.metres.toInt(), unit), stringResource(R.string.walk_distance))
            Divider()
            Stat(pace ?: NOTHING, stringResource(R.string.walk_pace, unit.symbol))
            Divider()
            Stat(grouped(state.calories), stringResource(R.string.walk_kcal))
        }
        Spacer(Modifier.height(Spacing.xl))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.ml * 2),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val pause = stringResource(if (state.paused) R.string.walk_resume else R.string.walk_pause)
            val icon = if (state.paused) R.drawable.ic_play else R.drawable.ic_pause
            SideButton(icon, pause, colors.surfaceTint, colors.accent, Modifier.tapped(onPause))
            Box(
                modifier = Modifier
                    .size(CameraButton)
                    .dropShadow(CircleShape, AppShadows.accentButton)
                    .background(colors.accentGradient, CircleShape)
                    .tapped(onCamera),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = stringResource(R.string.walk_photo),
                    modifier = Modifier.size(CameraIcon),
                    tint = colors.onAccent,
                )
            }
            val stop = stringResource(R.string.walk_stop)
            SideButton(R.drawable.ic_stop, stop, colors.dangerTint, colors.danger, Modifier.tapped(onStop))
        }
    }
}

private fun Modifier.tapped(onTap: () -> Unit) =
    pointerInput(onTap) { detectTapGestures(onTap = { onTap() }) }

@Composable
private fun RowScope.Stat(value: String, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        val type = MaterialTheme.typography
        Text(value, style = type.headlineSmall, color = AppTheme.colors.textPrimary, maxLines = 1)
        Spacer(Modifier.height(Spacing.xs / 2))
        Text(label, style = type.bodyMedium, color = AppTheme.colors.textSecondary)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.size(Sizes.hairline, Sizes.circle).background(AppTheme.colors.divider))
}

// A round button with its name under it; the name may be wider than the button.
@Composable
private fun SideButton(
    @DrawableRes iconRes: Int,
    label: String,
    ground: Color,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(top = (CameraButton - SideButton) / 2)
            .width(SideButton),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(SideButton).background(ground, CircleShape).then(modifier), Alignment.Center) {
            Icon(painterResource(iconRes), contentDescription = null, tint = tint)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            modifier = Modifier.wrapContentWidth(unbounded = true),
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
            softWrap = false,
        )
    }
}
