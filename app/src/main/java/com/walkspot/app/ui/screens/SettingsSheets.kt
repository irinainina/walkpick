package com.walkspot.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.walkspot.app.R
import com.walkspot.app.data.settings.AppSettings
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.GlucoseUnit
import com.walkspot.app.data.settings.WeightUnit
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.AppSheet
import com.walkspot.app.ui.components.AppSwitch
import com.walkspot.app.ui.components.AppTextField
import com.walkspot.app.ui.components.ListRow
import com.walkspot.app.ui.settings.SettingsState
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.ParagraphStyle
import com.walkspot.app.ui.theme.Spacing
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun SettingsSheets(
    sheet: SettingsSheet,
    state: SettingsState,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    val settings = state.settings
    val choose: ((AppSettings) -> AppSettings) -> Unit = {
        actions.onChange(it)
        onDismiss()
    }
    when (sheet) {
        SettingsSheet.Time -> TimeSheet(settings.reminderTime, onDismiss) { time ->
            choose { it.copy(reminderTime = time) }
        }
        SettingsSheet.Days -> AppSheet(stringResource(R.string.settings_days), onDismiss) {
            Spacer(Modifier.height(Spacing.s))
            DayOfWeek.entries.forEach { day ->
                val on = day in settings.reminderDays
                val name = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                ListRow(
                    label = name,
                    divider = day != DayOfWeek.SUNDAY,
                    trailing = {
                        AppSwitch(
                            on = on,
                            onToggle = { turnedOn -> actions.onChange { it.withDay(day, turnedOn) } },
                            label = name,
                            // The last day left on cannot be turned off.
                            enabled = !on || settings.reminderDays.size > 1,
                        )
                    },
                )
            }
        }
        SettingsSheet.Distance -> DistanceUnit.entries.let { units ->
            Choice(R.string.today_distance, units.map { it.symbol }, onDismiss) {
                choose { old -> old.copy(distance = units[it]) }
            }
        }
        SettingsSheet.Weight -> WeightUnit.entries.let { units ->
            Choice(R.string.health_weight, units.map { it.symbol }, onDismiss) {
                choose { old -> old.copy(weight = units[it]) }
            }
        }
        SettingsSheet.Glucose -> GlucoseUnit.entries.let { units ->
            Choice(R.string.health_glucose, units.map { it.symbol }, onDismiss) {
                choose { old -> old.copy(glucose = units[it]) }
            }
        }
        SettingsSheet.Delete -> DeleteSheet(state, actions.onDelete, onDismiss)
    }
}

// [onChoose] takes the place of the option in [options].
@Composable
private fun Choice(titleRes: Int, options: List<String>, onDismiss: () -> Unit, onChoose: (Int) -> Unit) {
    AppSheet(stringResource(titleRes), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        options.forEachIndexed { index, option ->
            ListRow(option, divider = index != options.lastIndex, onClick = { onChoose(index) }) {}
        }
    }
}

/** A time of day from what was typed into two fields, or null. */
internal fun timeOfDay(hour: String, minute: String): LocalTime? = try {
    LocalTime.of(hour.trim().toInt(), minute.trim().toInt())
} catch (_: NumberFormatException) {
    null
} catch (_: DateTimeException) {
    null
}

@Composable
private fun TimeSheet(current: LocalTime, onDismiss: () -> Unit, onSave: (LocalTime) -> Unit) {
    var hour by rememberSaveable { mutableStateOf(current.hour.toString()) }
    var minute by rememberSaveable { mutableStateOf("%02d".format(Locale.ENGLISH, current.minute)) }
    val time = timeOfDay(hour, minute)
    val submit = { time?.let(onSave) ?: Unit }
    AppSheet(stringResource(R.string.settings_time), onDismiss) {
        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            val part = Modifier.weight(1f)
            val number = KeyboardType.Number
            AppTextField(hour, { hour = it }, stringResource(R.string.settings_hour), part, number)
            AppTextField(
                value = minute,
                onValueChange = { minute = it },
                placeholder = stringResource(R.string.settings_minute),
                modifier = part,
                keyboardType = number,
                onDone = submit,
                error = stringResource(R.string.settings_time_wrong).takeIf {
                    hour.isNotBlank() && minute.isNotBlank() && time == null
                },
            )
        }
        Spacer(Modifier.height(Spacing.l))
        AppButton(stringResource(R.string.profile_save), onClick = submit)
    }
}

@Composable
private fun DeleteSheet(state: SettingsState, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    AppSheet(stringResource(R.string.settings_delete_title), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(R.string.settings_delete_text),
            style = ParagraphStyle,
            color = colors.textSecondary,
        )
        if (state.deleteFailed) {
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.settings_delete_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.danger,
            )
        }
        Spacer(Modifier.height(Spacing.l))
        AppButton(stringResource(R.string.settings_delete), onDelete, style = AppButtonStyle.Danger)
        Spacer(Modifier.height(Spacing.sm))
        AppButton(stringResource(R.string.settings_cancel), onDismiss, style = AppButtonStyle.Secondary)
    }
}
