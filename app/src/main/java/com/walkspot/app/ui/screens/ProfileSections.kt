package com.walkspot.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.walkspot.app.R
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.profile.Profile
import com.walkspot.app.data.profile.Sex
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.AppSwitch
import com.walkspot.app.ui.components.ListRow
import com.walkspot.app.ui.glucoseText
import com.walkspot.app.ui.profile.ProfileState
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing
import com.walkspot.app.ui.weightText

/** What a row of Profile opens for editing. */
enum class ProfileField { Goal, Sex, BirthDate, Height, TargetWeight, TargetPressure, TargetGlucose }

@Composable
internal fun ProfileBody(state: ProfileState, onEdit: (ProfileField) -> Unit) {
    val profile = state.profile
    val notSet = stringResource(R.string.profile_not_set)
    SectionTitle(stringResource(R.string.profile_body), stringResource(R.string.profile_body_note))
    AppCard {
        ListRow(
            label = stringResource(R.string.profile_sex),
            value = profile.sex?.let { stringResource(it.titleRes()) } ?: notSet,
            onClick = { onEdit(ProfileField.Sex) },
        )
        ListRow(
            label = stringResource(R.string.profile_age),
            value = profile.age(state.today)?.toString() ?: notSet,
            onClick = { onEdit(ProfileField.BirthDate) },
        )
        ListRow(
            label = stringResource(R.string.profile_height),
            value = profile.heightCm?.let { stringResource(R.string.profile_cm, it) } ?: notSet,
            onClick = { onEdit(ProfileField.Height) },
        )
        // The weight is the latest entry of Health: entered in one place, it cannot disagree.
        ListRow(
            label = stringResource(R.string.profile_weight),
            value = state.weightKg?.let { weightText(it, state.units.weight) } ?: notSet,
            trailing = {},
        )
        val pressure = HealthKind.PRESSURE in profile.healthTabs
        val glucose = HealthKind.GLUCOSE in profile.healthTabs
        ListRow(
            label = stringResource(R.string.profile_target_weight),
            value = profile.targetWeightKg?.let { weightText(it, state.units.weight) } ?: notSet,
            divider = pressure || glucose,
            onClick = { onEdit(ProfileField.TargetWeight) },
        )
        if (pressure) {
            ListRow(
                label = stringResource(R.string.profile_target_pressure),
                value = "${profile.targetSystolic}/${profile.targetDiastolic}",
                divider = glucose,
                onClick = { onEdit(ProfileField.TargetPressure) },
            )
        }
        if (glucose) {
            ListRow(
                label = stringResource(R.string.profile_target_glucose),
                value = glucoseText(profile.targetGlucose, state.units.glucose),
                divider = false,
                onClick = { onEdit(ProfileField.TargetGlucose) },
            )
        }
    }
}

@Composable
internal fun ProfileTabs(profile: Profile, onChange: ((Profile) -> Profile) -> Unit) {
    SectionTitle(stringResource(R.string.profile_tabs))
    AppCard {
        HealthKind.entries.forEach { tab ->
            val on = tab in profile.healthTabs
            val label = stringResource(tab.titleRes())
            ListRow(
                label = label,
                divider = tab != HealthKind.entries.last(),
                trailing = {
                    AppSwitch(
                        on = on,
                        onToggle = { turnedOn -> onChange { it.withTab(tab, turnedOn) } },
                        label = label,
                        // The last tab left on cannot be turned off.
                        enabled = !on || profile.healthTabs.size > 1,
                    )
                },
            )
        }
    }
    Spacer(Modifier.height(Spacing.s))
    Text(
        text = stringResource(R.string.profile_tabs_note),
        style = MaterialTheme.typography.bodySmall,
        color = AppTheme.colors.textSecondary,
    )
}

internal fun Sex.titleRes() = if (this == Sex.FEMALE) R.string.profile_female else R.string.profile_male

private fun HealthKind.titleRes() = when (this) {
    HealthKind.MOOD -> R.string.health_mood
    HealthKind.WEIGHT -> R.string.health_weight
    HealthKind.PRESSURE -> R.string.health_pressure
    HealthKind.GLUCOSE -> R.string.health_glucose
}
