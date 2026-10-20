package com.walkpick.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppButtonStyle
import com.walkpick.app.ui.components.AppSheet
import com.walkpick.app.ui.components.Mood
import com.walkpick.app.ui.components.MoodFace
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.walk.WalkEnding

private val FaceSize = 44.dp

/** Five faces from the saddest to the happiest; a tap says the mood, 1 (awful) to 5 (great). */
@Composable
private fun MoodRow(onMood: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Mood.entries.forEach { mood ->
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

/** Start walk asks first, as Stop does: a face starts the walk in that mood, the button without one. */
@Composable
internal fun StartWalkSheet(onStart: (Int?) -> Unit, onDismiss: () -> Unit) {
    AppSheet(stringResource(R.string.walk_mood_before), onDismiss) {
        Spacer(Modifier.height(Spacing.l))
        MoodRow(onMood = { onStart(it) })
        Spacer(Modifier.height(Spacing.l))
        val start = stringResource(R.string.today_start_walk)
        AppButton(start, onClick = { onStart(null) }, iconRes = R.drawable.ic_play)
        Spacer(Modifier.height(Spacing.sm))
        AppButton(stringResource(R.string.settings_cancel), onDismiss, style = AppButtonStyle.Secondary)
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
