package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.AppSheet
import com.walkspot.app.ui.components.Mood
import com.walkspot.app.ui.components.MoodFace
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.CardShape
import com.walkspot.app.ui.theme.Spacing
import com.walkspot.app.ui.walk.WalkEnding

private val FaceSize = 44.dp

/** Five faces, the best first as on the Health sheet; a tap says the mood, 1 (awful) to 5 (great). */
@Composable
private fun MoodRow(onMood: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Mood.entries.reversed().forEach { mood ->
            MoodFace(
                mood = mood,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClickLabel = stringResource(mood.titleRes), role = Role.Button) {
                        onMood(mood.value)
                    },
                size = FaceSize,
            )
        }
    }
}

/** Asked over the map while a walk is young: an answer is one tap, no answer is no trouble. */
@Composable
internal fun MoodAsk(onMood: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .dropShadow(CardShape, AppShadows.card)
            .background(colors.surface, CardShape)
            .padding(horizontal = Spacing.ml, vertical = Spacing.m),
    ) {
        Text(
            text = stringResource(R.string.walk_mood_before),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(Spacing.sm))
        MoodRow(onMood)
    }
}

/**
 * A walk is not ended by a slip of the finger: Stop asks first. A face ends the walk as the button
 * does and says the mood it ended in.
 */
@Composable
internal fun FinishWalkSheet(ending: WalkEnding, onFinish: (Int?) -> Unit, onDismiss: () -> Unit) {
    val kept = ending != WalkEnding.Dropped
    AppSheet(stringResource(R.string.walk_finish_title), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(if (kept) R.string.walk_finish_text else R.string.walk_finish_unkept),
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.textSecondary,
        )
        if (ending == WalkEnding.KeptWithMood) {
            Spacer(Modifier.height(Spacing.l))
            Text(
                text = stringResource(R.string.walk_mood_after),
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(Spacing.sm))
            MoodRow(onMood = { onFinish(it) })
        }
        Spacer(Modifier.height(Spacing.l))
        AppButton(stringResource(R.string.walk_finish), onClick = { onFinish(null) })
        Spacer(Modifier.height(Spacing.sm))
        AppButton(stringResource(R.string.walk_keep_walking), onDismiss, style = AppButtonStyle.Secondary)
    }
}
