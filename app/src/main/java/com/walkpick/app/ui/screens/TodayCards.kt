package com.walkpick.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.Bar
import com.walkpick.app.ui.components.BarChart
import com.walkpick.app.ui.components.IconBadge
import com.walkpick.app.ui.components.ProgressRing
import com.walkpick.app.ui.components.RowChevron
import com.walkpick.app.ui.counted
import com.walkpick.app.ui.distanceText
import com.walkpick.app.ui.grouped
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.today.TodayState

private val RingSize = 176.dp
private val StatsBottom = 28.dp
private val StatDividerHeight = 40.dp
private val NoticeTextTop = 14.dp
private val NoticeTextGap = 2.dp
private val WeekBarWidth = 10.dp
private val WeekTrackHeight = 48.dp

@Composable
internal fun StepsCard(state: TodayState) {
    val colors = AppTheme.colors
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.l, bottom = StatsBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProgressRing(state.steps.toFloat() / state.dailySteps, Modifier.size(RingSize)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = grouped(state.steps),
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.today_steps, grouped(state.dailySteps)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.padding(horizontal = Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Stat(distanceText(state.distanceM, state.distanceUnit), R.string.today_distance)
                Box(Modifier.width(Spacing.m), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(Sizes.hairline, StatDividerHeight)
                            .background(colors.divider),
                    )
                }
                Stat(stringResource(R.string.today_min, state.activeMinutes), R.string.today_active_time)
            }
        }
    }
}

@Composable
private fun RowScope.Stat(value: String, labelRes: Int) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.textPrimary)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun Badge(@DrawableRes iconRes: Int) =
    IconBadge(iconRes, AppTheme.colors.streakTint, AppTheme.colors.streak)

@Composable
internal fun StreakCard(streak: Int) {
    AppCard {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Badge(R.drawable.ic_flame)
            Column {
                Text(
                    text = if (streak > 0) {
                        counted(streak, R.string.today_streak_one, R.string.today_streak_many)
                    } else {
                        stringResource(R.string.today_streak_none)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = AppTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.today_streak_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}

/** Why the ring stands at zero; [sensor] is false on a phone that cannot count steps at all. */
@Composable
internal fun NoStepsCard(sensor: Boolean, onAllow: () -> Unit) {
    val reason = if (sensor) R.string.today_no_permission else R.string.today_no_sensor
    NoticeCard(
        title = stringResource(R.string.today_no_steps),
        text = stringResource(reason),
        action = stringResource(R.string.today_allow).takeIf { sensor },
        onAction = onAllow,
    )
}

/** Shown until the first walk: what the ring counts and what the app is for. */
@Composable
internal fun FirstWalkCard() {
    NoticeCard(stringResource(R.string.today_first_title), stringResource(R.string.today_first_text))
}

@Composable
private fun NoticeCard(title: String, text: String, action: String? = null, onAction: () -> Unit = {}) {
    val colors = AppTheme.colors
    AppCard {
        Row(
            modifier = Modifier.padding(Spacing.m, NoticeTextTop, Spacing.m, Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(Modifier.padding(top = NoticeTextGap)) { Badge(R.drawable.ic_alert) }
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                Spacer(Modifier.height(NoticeTextGap))
                Text(text, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                if (action != null) {
                    Spacer(Modifier.height(Spacing.ml))
                    Text(
                        text = action,
                        modifier = Modifier.clickable(role = Role.Button, onClick = onAction),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent,
                    )
                }
            }
        }
    }
}

@Composable
internal fun WeekCard(week: List<Bar>, onOpen: () -> Unit) {
    AppCard {
        Column(
            Modifier
                .clickable(onClick = onOpen)
                .padding(top = Spacing.m, bottom = Spacing.sm),
        ) {
            Row(
                modifier = Modifier.padding(start = Spacing.ml, end = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.today_week),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary,
                )
                RowChevron()
            }
            Spacer(Modifier.height(Spacing.s))
            BarChart(week, WeekBarWidth, WeekTrackHeight, Modifier.padding(horizontal = Spacing.ml))
        }
    }
}
