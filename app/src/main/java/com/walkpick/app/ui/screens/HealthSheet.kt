package com.walkpick.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.health.HealthLimits
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.settings.GlucoseUnit
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppSheet
import com.walkpick.app.ui.components.AppTextField
import com.walkpick.app.ui.components.Mood
import com.walkpick.app.ui.components.MoodFace
import com.walkpick.app.ui.components.PickerField
import com.walkpick.app.ui.components.SegmentedControl
import com.walkpick.app.ui.health.HealthState
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val MoodOptionHeight = 88.dp

/** A new entry of any tab turned on; [onSave] takes the kind, the value, the lower pressure, the moment. */
@Composable
internal fun NewEntrySheet(
    state: HealthState,
    onSave: (HealthKind, Double, Int?, Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(state.tab) }
    var mood by rememberSaveable { mutableStateOf(Mood.Good) }
    var number by rememberSaveable(kind) { mutableStateOf("") }
    var lowerNumber by rememberSaveable(kind) { mutableStateOf("") }
    var moment by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    // Typed in the unit the user reads, kept in kilograms and mmol/L.
    val units = state.units
    val mmol = units.glucose == GlucoseUnit.MMOL
    val (range, scale, decimal) = when (kind) {
        HealthKind.WEIGHT -> Triple(HealthLimits.weightKg, units.weight.perMetric, true)
        HealthKind.GLUCOSE -> Triple(HealthLimits.glucoseMmol, units.glucose.perMetric, mmol)
        else -> Triple(HealthLimits.systolic, 1.0, false)
    }
    val shown = range.start * scale..range.endInclusive * scale
    val typed = parse(number, shown, decimal)?.div(scale)
    val value = if (kind == HealthKind.MOOD) mood.value.toDouble() else typed
    val lower = parse(lowerNumber, HealthLimits.diastolic, decimal = false)?.toInt()
    val lowerFits = kind != HealthKind.PRESSURE || (lower != null && value != null && lower < value)
    val save = {
        if (value != null && lowerFits) {
            onSave(kind, value, lower.takeIf { kind == HealthKind.PRESSURE }, Instant.ofEpochMilli(moment))
            onDismiss()
        }
    }

    AppSheet(stringResource(R.string.health_new_entry), onDismiss) {
        Spacer(Modifier.height(Spacing.m))
        if (state.tabs.size > 1) {
            SegmentedControl(
                options = state.tabs.map { stringResource(it.titleRes()) },
                selectedIndex = state.tabs.indexOf(kind),
                onSelect = { kind = state.tabs[it] },
            )
            Spacer(Modifier.height(Spacing.m))
        }
        val numberError = rangeHint(shown).takeIf { number.isNotBlank() && value == null }
        when (kind) {
            HealthKind.MOOD -> MoodPicker(mood, onPick = { mood = it })
            HealthKind.PRESSURE -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                val unit = stringResource(R.string.profile_mmhg)
                AppTextField(
                    value = number,
                    onValueChange = { number = it },
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.profile_systolic),
                    suffix = unit,
                    keyboardType = KeyboardType.Number,
                    error = numberError,
                )
                AppTextField(
                    value = lowerNumber,
                    onValueChange = { lowerNumber = it },
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.profile_diastolic),
                    suffix = unit,
                    keyboardType = KeyboardType.Number,
                    onDone = save,
                    error = when {
                        lowerNumber.isBlank() -> null
                        lower == null -> rangeHint(HealthLimits.diastolic)
                        value != null && lower >= value -> stringResource(R.string.health_lower_wrong)
                        else -> null
                    },
                    autoFocus = false,
                )
            }
            else -> AppTextField(
                value = number,
                onValueChange = { number = it },
                label = stringResource(kind.titleRes()),
                suffix = if (kind == HealthKind.WEIGHT) units.weight.symbol else units.glucose.symbol,
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                onDone = save,
                error = numberError,
            )
        }
        Spacer(Modifier.height(Spacing.m))
        PickerField(
            text = momentText(Instant.ofEpochMilli(moment), state.today),
            label = stringResource(R.string.health_date_time),
            iconRes = R.drawable.ic_calendar,
            onClick = { pickMoment(context, moment) { moment = it } },
        )
        Spacer(Modifier.height(Spacing.xl))
        AppButton(stringResource(R.string.profile_save), onClick = save)
    }
}

@Composable
private fun MoodPicker(selected: Mood, onPick: (Mood) -> Unit) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val shape = MaterialTheme.shapes.medium
    Text(stringResource(R.string.health_how_feel), style = type.bodySmall, color = colors.textSecondary)
    Spacer(Modifier.height(Spacing.s))
    Row(Modifier.fillMaxWidth()) {
        // Great comes first, as the design draws it.
        Mood.entries.reversed().forEach { mood ->
            val picked = mood == selected
            val ground = if (picked) {
                Modifier
                    .background(colors.surfaceTint, shape)
                    .border(Sizes.focusBorder, colors.accent, shape)
            } else {
                Modifier
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(MoodOptionHeight)
                    .clip(shape)
                    .then(ground)
                    .selectable(picked, role = Role.RadioButton, onClick = { onPick(mood) })
                    .padding(vertical = Spacing.s),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MoodFace(mood)
                Text(
                    text = stringResource(mood.titleRes),
                    style = if (picked) type.labelSmall else type.bodySmall,
                    color = if (picked) colors.accent else colors.textSecondary,
                )
            }
        }
    }
}

// The system pickers, the date first; a moment later than now becomes now.
private fun pickMoment(context: Context, current: Long, onPicked: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(current).atZone(zone)
    val dates = DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val picked = LocalDateTime.of(year, month + 1, day, hour, minute).atZone(zone).toInstant()
                    onPicked(minOf(picked.toEpochMilli(), System.currentTimeMillis()))
                },
                start.hour,
                start.minute,
                true,
            ).show()
        },
        start.year,
        start.monthValue - 1,
        start.dayOfMonth,
    )
    dates.datePicker.maxDate = System.currentTimeMillis()
    dates.show()
}
