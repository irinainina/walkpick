package com.walkpick.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.walkpick.app.R
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.Profile
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.ui.components.AppSwitch
import com.walkpick.app.ui.components.ListRow
import com.walkpick.app.ui.glucoseText
import com.walkpick.app.ui.weightText

/** What a row of Profile opens for editing. */
enum class ProfileField { Height, TargetWeight, TargetPressure, TargetGlucose }

@Composable
internal fun HeightRow(profile: Profile, onEdit: (ProfileField) -> Unit) {
    ListRow(
        label = stringResource(R.string.profile_height),
        value = profile.heightCm?.let { stringResource(R.string.profile_cm, it) }
            ?: stringResource(R.string.profile_not_set),
        onClick = { onEdit(ProfileField.Height) },
    )
}

@Composable
internal fun HealthTabs(profile: Profile, onChange: ((Profile) -> Profile) -> Unit) {
    // Pressure and glucose are health data: they are turned on only after the walker has read so.
    var asking by rememberSaveable { mutableStateOf<HealthKind?>(null) }
    Section(R.string.profile_tabs, note = stringResource(R.string.profile_tabs_note)) {
        HealthKind.entries.forEach { tab ->
            val on = tab in profile.healthTabs
            val label = stringResource(tab.titleRes())
            ListRow(
                label = label,
                divider = tab != HealthKind.entries.last(),
                trailing = {
                    AppSwitch(
                        on = on,
                        onToggle = { turnedOn ->
                            val ask = turnedOn && tab in AskedTabs
                            if (ask) asking = tab else onChange { it.withTab(tab, turnedOn) }
                        },
                        label = label,
                        // The last tab left on cannot be turned off.
                        enabled = !on || profile.healthTabs.size > 1,
                    )
                },
            )
        }
    }
    asking?.let { tab ->
        ConsentSheet(
            tab = tab,
            onTurnOn = {
                asking = null
                onChange { it.withTab(tab, true) }
            },
            onDismiss = { asking = null },
        )
    }
}

/** A target only for a tab that is shown; Mood has none, so with Mood alone there is no section. */
@Composable
internal fun Targets(profile: Profile, units: AppSettings, onEdit: (ProfileField) -> Unit) {
    val notSet = stringResource(R.string.profile_not_set)
    val rows = TargetKinds.filter { it in profile.healthTabs }
    if (rows.isEmpty()) return
    Section(R.string.profile_targets) {
        rows.forEach { kind ->
            val divider = kind != rows.last()
            when (kind) {
                HealthKind.WEIGHT -> ListRow(
                    label = stringResource(R.string.profile_target_weight),
                    // Only a target of the walker's own: a weight worked out by a formula is not shown.
                    value = profile.ownTargetWeightKg?.let { weightText(it, units.weight) } ?: notSet,
                    divider = divider,
                    onClick = { onEdit(ProfileField.TargetWeight) },
                )
                HealthKind.PRESSURE -> ListRow(
                    label = stringResource(R.string.profile_target_pressure),
                    value = profile.targetSystolic?.let { "$it/${profile.targetDiastolic}" } ?: notSet,
                    divider = divider,
                    onClick = { onEdit(ProfileField.TargetPressure) },
                )
                else -> ListRow(
                    label = stringResource(R.string.profile_target_glucose),
                    value = profile.targetGlucose?.let { glucoseText(it, units.glucose) } ?: notSet,
                    divider = divider,
                    onClick = { onEdit(ProfileField.TargetGlucose) },
                )
            }
        }
    }
}

private val AskedTabs = setOf(HealthKind.PRESSURE, HealthKind.GLUCOSE)
private val TargetKinds = listOf(HealthKind.WEIGHT, HealthKind.PRESSURE, HealthKind.GLUCOSE)

internal fun HealthKind.titleRes() = when (this) {
    HealthKind.MOOD -> R.string.health_mood
    HealthKind.WEIGHT -> R.string.health_weight
    HealthKind.PRESSURE -> R.string.health_pressure
    HealthKind.GLUCOSE -> R.string.health_glucose
}
