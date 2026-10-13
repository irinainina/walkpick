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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppButton
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
 * The main action of the app, always in sight above the tabs: Start walk; during a walk - the way
 * back to it with its time, and a quiet Stop walk below.
 */
@Composable
internal fun StartBar(
    walkSeconds: Long?,
    walkEnding: WalkEnding,
    onStartWalk: () -> Unit,
    onStopWalk: (Int?) -> Unit,
) {
    var stopping by remember { mutableStateOf(false) }
    val text = when (walkSeconds) {
        null -> stringResource(R.string.today_start_walk)
        else -> stringResource(R.string.today_back_to_walk, clockText(walkSeconds))
    }
    val walking = walkSeconds != null
    Column(
        Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.canvas)
            // The link carries its own space below; together it is the same gap as without it.
            .padding(Spacing.ml, Spacing.sm, Spacing.ml, if (walking) Spacing.xs else Spacing.sm),
    ) {
        AppButton(text, onClick = onStartWalk, iconRes = R.drawable.ic_play)
        if (walking) {
            Text(
                text = stringResource(R.string.today_stop_walk),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs)
                    .clickable { stopping = true }
                    .padding(vertical = Spacing.s),
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.streak,
                textAlign = TextAlign.Center,
            )
        }
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
