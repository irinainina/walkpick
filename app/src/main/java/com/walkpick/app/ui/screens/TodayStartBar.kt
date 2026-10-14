package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppButtonStyle
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.walk.WalkEnding

private val FadeHeight = 16.dp

/** The cards slide under the start bar: they fade into the canvas instead of being cut by its edge. */
@Composable
internal fun StartBarFade(modifier: Modifier = Modifier) {
    val canvas = AppTheme.colors.canvas
    Box(
        modifier
            .fillMaxWidth()
            .height(FadeHeight)
            .background(Brush.verticalGradient(listOf(Color.Transparent, canvas))),
    )
}

/**
 * The main action of the app, always in sight above the tabs: Start walk; during a walk - Stop walk,
 * and under it the way back to the walk with its time, or the time it was paused at.
 */
@Composable
internal fun StartBar(
    walkSeconds: Long?,
    walkPaused: Boolean,
    walkEnding: WalkEnding,
    walkMoods: Boolean,
    onStartWalk: (Int?) -> Unit,
    onStopWalk: (Int?) -> Unit,
) {
    var starting by remember { mutableStateOf(false) }
    var stopping by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.canvas)
            .padding(horizontal = Spacing.ml, vertical = Spacing.sm),
    ) {
        if (walkSeconds == null) {
            val start = { if (walkMoods) starting = true else onStartWalk(null) }
            AppButton(stringResource(R.string.today_start_walk), start, iconRes = R.drawable.ic_play)
            return@Column
        }
        AppButton(
            text = stringResource(R.string.today_stop_walk),
            onClick = { stopping = true },
            style = AppButtonStyle.Stop,
            iconRes = R.drawable.ic_stop_square,
        )
        val back = if (walkPaused) R.string.today_back_to_walk_paused else R.string.today_back_to_walk
        Text(
            text = stringResource(back, clockText(walkSeconds)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm)
                .clickable(role = Role.Button) { onStartWalk(null) },
            style = MaterialTheme.typography.labelLarge,
            color = AppTheme.colors.accent,
            textAlign = TextAlign.Center,
        )
    }
    if (starting) {
        StartWalkSheet(
            onStart = { mood ->
                starting = false
                onStartWalk(mood)
            },
            onDismiss = { starting = false },
        )
    }
    if (stopping) {
        FinishWalkSheet(
            ending = walkEnding,
            onFinish = { mood ->
                stopping = false
                onStopWalk(mood)
            },
            onDismiss = { stopping = false },
        )
    }
}
