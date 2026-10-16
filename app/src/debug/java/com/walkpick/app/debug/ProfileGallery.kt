package com.walkpick.app.debug

import androidx.compose.runtime.Composable
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.Profile
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.ui.profile.ProfileState
import com.walkpick.app.ui.profile.Totals
import com.walkpick.app.ui.screens.ConsentSheet
import com.walkpick.app.ui.screens.ProfileScreen
import com.walkpick.app.ui.screens.SettingsActions
import com.walkpick.app.ui.settings.SettingsState
import java.time.DayOfWeek

internal val NoSettingsActions = SettingsActions({}, {}, {}, {}, {}, {}, {})

/** Profile with the data of the Figma frames, for the comparison with the design. */
@Composable
fun ProfileGallery(
    steps: StepsAccess = StepsAccess.COUNTING,
    consent: Boolean = false,
    tabs: Set<HealthKind> = HealthKind.entries.toSet(),
) {
    ProfileScreen(
        state = ProfileState(
            name = "Ira",
            email = "ira@example.com",
            profile = Profile(heightCm = 168, ownTargetWeightKg = 62.0, healthTabs = tabs),
            totals = Totals(walks = 312, metres = 1_209_000, photos = 540),
        ),
        settings = SettingsState(
            settings = AppSettings(remindersOn = true, reminderDays = DayOfWeek.entries.take(5).toSet()),
            steps = steps,
            tabs = tabs,
            version = "1.0.0",
        ),
        actions = NoSettingsActions,
        onChange = {},
        onBack = {},
    )
    if (consent) ConsentSheet(HealthKind.PRESSURE, onTurnOn = {}, onDismiss = {})
}
