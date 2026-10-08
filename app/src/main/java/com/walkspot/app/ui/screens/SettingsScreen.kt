package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.walkspot.app.R
import com.walkspot.app.data.settings.AppSettings
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.ui.PRIVACY_URL
import com.walkspot.app.ui.TERMS_URL
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.AppSwitch
import com.walkspot.app.ui.components.AppTopBar
import com.walkspot.app.ui.components.IconBadge
import com.walkspot.app.ui.components.ListRow
import com.walkspot.app.ui.settings.SettingsState
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val AllowButtonHeight = 48.dp
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** What a row of Settings opens. */
enum class SettingsSheet { Time, Days, Distance, Weight, Glucose, Delete }

class SettingsActions(
    val onBack: () -> Unit,
    val onChange: ((AppSettings) -> AppSettings) -> Unit,
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
            .navigationBarsPadding()
            .padding(bottom = Spacing.l),
    ) {
        val title = stringResource(R.string.screen_settings)
        AppTopBar(title, actions.onBack, Modifier.padding(top = Spacing.xs))
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
        Section(R.string.settings_data) { StepCounting(state.steps, actions.onAllowSteps) }
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
private fun Section(titleRes: Int, rows: @Composable () -> Unit) {
    Column(Modifier.padding(start = Spacing.ml, top = Spacing.sm, end = Spacing.ml, bottom = Spacing.sm)) {
        Text(
            text = stringResource(titleRes).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.s))
        AppCard { rows() }
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
