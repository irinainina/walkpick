package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.walk.Season
import com.walkpick.app.ui.PRIVACY_URL
import com.walkpick.app.ui.TERMS_URL
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.AppSwitch
import com.walkpick.app.ui.components.IconBadge
import com.walkpick.app.ui.components.ListRow
import com.walkpick.app.ui.components.SegmentedControl
import com.walkpick.app.ui.components.TabTitle
import com.walkpick.app.ui.grouped
import com.walkpick.app.ui.settings.SettingsState
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val AllowButtonHeight = 48.dp
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** What a row of Settings opens. */
enum class SettingsSheet { Time, Days, Distance, Weight, Glucose, Steps, Delete }

class SettingsActions(
    val onChange: ((AppSettings) -> AppSettings) -> Unit,
    val onDailySteps: (Int) -> Unit,
    /** Asks Android for the right to show notifications first. */
    val onReminders: (Boolean) -> Unit,
    val onAllowSteps: () -> Unit,
    val onSignOut: () -> Unit,
    val onDelete: () -> Unit,
)

@Composable
fun SettingsScreen(state: SettingsState, actions: SettingsActions, modifier: Modifier = Modifier) {
    var sheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    val settings = state.settings
    Column(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.l),
    ) {
        TabTitle(stringResource(R.string.screen_settings))
        Spacer(Modifier.height(Spacing.s))
        Section(R.string.settings_reminders) {
            val reminder = stringResource(R.string.settings_walk_reminder)
            ListRow(reminder, trailing = { AppSwitch(settings.remindersOn, actions.onReminders, reminder) })
            ListRow(
                label = stringResource(R.string.settings_time),
                value = settings.reminderTime.format(Clock),
                onClick = { sheet = SettingsSheet.Time },
            )
            ListRow(
                label = stringResource(R.string.settings_days),
                value = daysLabel(settings.reminderDays),
                divider = false,
                onClick = { sheet = SettingsSheet.Days },
            )
        }
        Section(R.string.settings_theme, inCard = false) {
            SegmentedControl(
                options = Season.entries.map { stringResource(it.title) },
                selectedIndex = AppTheme.season.ordinal,
                onSelect = { index -> actions.onChange { it.copy(theme = Season.entries[index]) } },
            )
        }
        Section(R.string.settings_units) {
            ListRow(
                label = stringResource(R.string.today_distance),
                value = settings.distance.symbol,
                onClick = { sheet = SettingsSheet.Distance },
            )
            ListRow(
                label = stringResource(R.string.health_weight),
                value = settings.weight.symbol,
                divider = state.glucoseShown,
                onClick = { sheet = SettingsSheet.Weight },
            )
            if (state.glucoseShown) {
                ListRow(
                    label = stringResource(R.string.health_glucose),
                    value = settings.glucose.symbol,
                    divider = false,
                    onClick = { sheet = SettingsSheet.Glucose },
                )
            }
        }
        Section(R.string.settings_data) {
            ListRow(
                label = stringResource(R.string.settings_daily_steps),
                value = grouped(state.dailySteps),
                onClick = { sheet = SettingsSheet.Steps },
            )
            val mood = stringResource(R.string.settings_walk_mood)
            val setMood = { on: Boolean -> actions.onChange { it.copy(walkMood = on) } }
            ListRow(mood, trailing = { AppSwitch(settings.walkMood, setMood, mood) })
            StepCounting(state.steps, actions.onAllowSteps)
        }
        Section(R.string.settings_about) {
            val links = LocalUriHandler.current
            ListRow(stringResource(R.string.settings_privacy), onClick = { links.openUri(PRIVACY_URL) })
            ListRow(stringResource(R.string.settings_terms), onClick = { links.openUri(TERMS_URL) })
            ListRow(stringResource(R.string.settings_version), value = state.version, divider = false) {}
        }
        Section(R.string.settings_account) {
            val colors = AppTheme.colors
            ListRow(
                label = stringResource(R.string.settings_sign_out),
                onClick = actions.onSignOut,
                leading = { Badge(R.drawable.ic_sign_out, colors.surfaceTint, colors.accent) },
                trailing = {},
            )
            ListRow(
                label = stringResource(R.string.settings_delete),
                divider = false,
                onClick = { sheet = SettingsSheet.Delete },
                labelColor = colors.danger,
                leading = { Badge(R.drawable.ic_trash, colors.dangerTint, colors.danger) },
                trailing = {},
            )
        }
    }
    sheet?.let { SettingsSheets(it, state, actions, onDismiss = { sheet = null }) }
}

@Composable
private fun Badge(iconRes: Int, ground: Color, tint: Color) =
    IconBadge(iconRes, ground, tint, Sizes.iconSmall)

@Composable
private fun Section(titleRes: Int, inCard: Boolean = true, rows: @Composable () -> Unit) {
    Column(Modifier.padding(start = Spacing.ml, top = Spacing.sm, end = Spacing.ml, bottom = Spacing.sm)) {
        Text(
            text = stringResource(titleRes).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.s))
        if (inCard) AppCard { rows() } else rows()
    }
}

@Composable
private fun StepCounting(access: StepsAccess, onAllow: () -> Unit) {
    val counting = access == StepsAccess.COUNTING
    ListRow(
        label = stringResource(R.string.settings_step_counting),
        value = stringResource(if (counting) R.string.settings_on else R.string.settings_off),
        divider = false,
        valueColor = with(AppTheme.colors) { if (counting) textSecondary else streak },
        trailing = {},
    )
    if (counting) return
    Column(Modifier.padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.m)) {
        val sensor = access != StepsAccess.NO_SENSOR
        Text(
            text = stringResource(if (sensor) R.string.settings_steps_off else R.string.today_no_sensor),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
        if (sensor) {
            Spacer(Modifier.height(Spacing.sm))
            AppButton(stringResource(R.string.today_allow), onAllow, Modifier.height(AllowButtonHeight))
        }
    }
}

private val Season.title: Int
    get() = when (this) {
        Season.SPRING -> R.string.settings_theme_spring
        Season.SUMMER -> R.string.settings_theme_summer
        Season.AUTUMN -> R.string.settings_theme_autumn
        Season.WINTER -> R.string.settings_theme_winter
    }

/** "Every day", "Mon – Fri" for days in a row, otherwise the days by name. */
@Composable
internal fun daysLabel(days: Set<DayOfWeek>): String {
    val sorted = days.sortedBy { it.value }
    val names = sorted.map { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
    val inARow = sorted.size > 2 && sorted.last().value - sorted.first().value == sorted.size - 1
    return when {
        sorted.size == DayOfWeek.entries.size -> stringResource(R.string.settings_every_day)
        inARow -> "${names.first()} – ${names.last()}"
        else -> names.joinToString(", ")
    }
}
