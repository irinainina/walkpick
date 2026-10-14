package com.walkpick.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.walkpick.app.R
import com.walkpick.app.data.health.HealthLimits
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.Profile
import com.walkpick.app.data.profile.Sex
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.data.settings.GlucoseUnit
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppSheet
import com.walkpick.app.ui.components.AppTextField
import com.walkpick.app.ui.components.ListRow
import com.walkpick.app.ui.glucoseNumber
import com.walkpick.app.ui.oneDecimal
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.ParagraphStyle
import com.walkpick.app.ui.theme.Spacing
import java.time.DateTimeException
import java.time.LocalDate

// What a person can be: the limits only keep slips of the finger out.
private val HeightRange = 50.0..260.0
private val WeightRange = HealthLimits.weightKg
private val SystolicRange = HealthLimits.systolic
private val DiastolicRange = HealthLimits.diastolic
private val GlucoseRange = HealthLimits.glucoseMmol
private const val OLDEST_YEARS = 120L

@Composable
internal fun ProfileSheet(
    field: ProfileField,
    profile: Profile,
    units: AppSettings,
    onSave: ((Profile) -> Profile) -> Unit,
    onDismiss: () -> Unit,
) {
    val save: ((Profile) -> Profile) -> Unit = {
        onSave(it)
        onDismiss()
    }
    when (field) {
        ProfileField.Height -> NumberSheet(
            title = stringResource(R.string.profile_height_cm),
            initial = profile.heightCm?.toString().orEmpty(),
            range = HeightRange,
            onDismiss = onDismiss,
        ) { save { old -> old.copy(heightCm = it.toInt()) } }
        // Entered in the unit the user reads, kept in kilograms and mmol/L.
        ProfileField.TargetWeight -> units.weight.perMetric.let { scale ->
            NumberSheet(
                title = stringResource(R.string.profile_target_weight_in, units.weight.symbol),
                initial = profile.ownTargetWeightKg?.let { oneDecimal(it * scale) }.orEmpty(),
                range = WeightRange.start * scale..WeightRange.endInclusive * scale,
                onDismiss = onDismiss,
                decimal = true,
            ) { save { old -> old.copy(ownTargetWeightKg = it / scale) } }
        }
        ProfileField.TargetGlucose -> units.glucose.perMetric.let { scale ->
            NumberSheet(
                title = stringResource(R.string.profile_target_glucose_in, units.glucose.symbol),
                initial = profile.targetGlucose?.let { glucoseNumber(it, units.glucose) }.orEmpty(),
                range = GlucoseRange.start * scale..GlucoseRange.endInclusive * scale,
                onDismiss = onDismiss,
                decimal = units.glucose == GlucoseUnit.MMOL,
            ) { save { old -> old.copy(targetGlucose = it / scale) } }
        }
        ProfileField.TargetPressure -> PressureSheet(profile, onDismiss) { systolic, diastolic ->
            save { it.copy(targetSystolic = systolic, targetDiastolic = diastolic) }
        }
        ProfileField.Sex -> AppSheet(stringResource(R.string.profile_sex), onDismiss) {
            Spacer(Modifier.height(Spacing.s))
            Sex.entries.forEach { sex ->
                ListRow(
                    label = stringResource(sex.titleRes()),
                    divider = sex != Sex.entries.last(),
                    onClick = { save { it.copy(sex = sex) } },
                    trailing = {},
                )
            }
        }
        ProfileField.BirthDate -> BirthDateSheet(profile.birthDate, onDismiss) { date ->
            save { it.copy(birthDate = date) }
        }
    }
}

internal fun parse(text: String, range: ClosedFloatingPointRange<Double>, decimal: Boolean): Double? {
    val number = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return number.takeIf { it in range && (decimal || it % 1 == 0.0) }
}

@Composable
internal fun rangeHint(range: ClosedFloatingPointRange<Double>) =
    stringResource(R.string.profile_range, range.start.toInt(), range.endInclusive.toInt())

@Composable
private fun NumberSheet(
    title: String,
    initial: String,
    range: ClosedFloatingPointRange<Double>,
    onDismiss: () -> Unit,
    decimal: Boolean = false,
    onSave: (Double) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val number = parse(text, range, decimal)
    val submit = { number?.let(onSave) ?: Unit }
    AppSheet(title, onDismiss) {
        Spacer(Modifier.height(Spacing.m))
        AppTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = rangeHint(range),
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            onDone = submit,
            error = rangeHint(range).takeIf { text.isNotBlank() && number == null },
        )
        Spacer(Modifier.height(Spacing.xl))
        AppButton(stringResource(R.string.profile_save), onClick = submit)
    }
}

@Composable
private fun PressureSheet(profile: Profile, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var upper by rememberSaveable { mutableStateOf(profile.targetSystolic?.toString().orEmpty()) }
    var lower by rememberSaveable { mutableStateOf(profile.targetDiastolic?.toString().orEmpty()) }
    val systolic = parse(upper, SystolicRange, decimal = false)
    val diastolic = parse(lower, DiastolicRange, decimal = false)
    val submit = { if (systolic != null && diastolic != null) onSave(systolic.toInt(), diastolic.toInt()) }
    AppSheet(stringResource(R.string.profile_target_pressure), onDismiss) {
        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val unit = stringResource(R.string.profile_mmhg)
            AppTextField(
                value = upper,
                onValueChange = { upper = it },
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.profile_systolic),
                suffix = unit,
                keyboardType = KeyboardType.Number,
                error = rangeHint(SystolicRange).takeIf { upper.isNotBlank() && systolic == null },
            )
            AppTextField(
                value = lower,
                onValueChange = { lower = it },
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.profile_diastolic),
                suffix = unit,
                keyboardType = KeyboardType.Number,
                onDone = submit,
                error = rangeHint(DiastolicRange).takeIf { lower.isNotBlank() && diastolic == null },
                autoFocus = false,
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        AppButton(stringResource(R.string.profile_save), onClick = submit)
    }
}

/** A date somebody alive today could have been born on, or null. */
internal fun birthDate(day: String, month: String, year: String, today: LocalDate): LocalDate? {
    val date = try {
        LocalDate.of(year.trim().toInt(), month.trim().toInt(), day.trim().toInt())
    } catch (_: NumberFormatException) {
        null
    } catch (_: DateTimeException) {
        null
    }
    return date?.takeIf { it <= today && it > today.minusYears(OLDEST_YEARS) }
}

@Composable
private fun BirthDateSheet(current: LocalDate?, onDismiss: () -> Unit, onSave: (LocalDate) -> Unit) {
    var day by rememberSaveable { mutableStateOf(current?.dayOfMonth?.toString().orEmpty()) }
    var month by rememberSaveable { mutableStateOf(current?.monthValue?.toString().orEmpty()) }
    var year by rememberSaveable { mutableStateOf(current?.year?.toString().orEmpty()) }
    val date = birthDate(day, month, year, LocalDate.now())
    val submit = { date?.let(onSave) ?: Unit }
    val filled = day.isNotBlank() && month.isNotBlank() && year.isNotBlank()
    val focus = LocalFocusManager.current
    // Two digits fill a day or a month: the cursor goes on by itself.
    val typed = { text: String, keep: (String) -> Unit ->
        keep(text)
        if (text.trim().length == 2) focus.moveFocus(FocusDirection.Next)
    }
    AppSheet(stringResource(R.string.profile_birth_date), onDismiss) {
        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val part = Modifier.weight(1f)
            val number = KeyboardType.Number
            AppTextField(
                value = day,
                onValueChange = { typed(it) { day = it } },
                modifier = part,
                label = stringResource(R.string.profile_day),
                keyboardType = number,
            )
            AppTextField(
                value = month,
                onValueChange = { typed(it) { month = it } },
                modifier = part,
                label = stringResource(R.string.profile_month),
                keyboardType = number,
                autoFocus = false,
            )
            AppTextField(
                value = year,
                onValueChange = { year = it },
                modifier = part,
                label = stringResource(R.string.profile_year),
                keyboardType = number,
                onDone = submit,
                error = stringResource(R.string.profile_date_wrong).takeIf { filled && date == null },
                autoFocus = false,
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        AppButton(stringResource(R.string.profile_save), onClick = submit)
    }
}

/** Asked before Pressure or Glucose is turned on: what is kept, where and who sees it. */
@Composable
internal fun ConsentSheet(tab: HealthKind, onTurnOn: () -> Unit, onDismiss: () -> Unit) {
    val pressure = tab == HealthKind.PRESSURE
    val title = if (pressure) R.string.consent_title_pressure else R.string.consent_title_glucose
    AppSheet(stringResource(title), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        val text = stringResource(R.string.consent_text)
        Text(text, style = ParagraphStyle, color = AppTheme.colors.textPrimary)
        Spacer(Modifier.height(Spacing.l))
        AppButton(stringResource(R.string.consent_turn_on), onClick = onTurnOn)
        Spacer(Modifier.height(Spacing.m))
        Text(
            text = stringResource(R.string.consent_not_now),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onDismiss),
            style = MaterialTheme.typography.titleSmall,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
