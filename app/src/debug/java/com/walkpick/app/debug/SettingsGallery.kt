package com.walkpick.app.debug

import androidx.compose.runtime.Composable
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.ui.screens.SettingsActions
import com.walkpick.app.ui.screens.SettingsScreen
import com.walkpick.app.ui.settings.SettingsState
import java.time.DayOfWeek

/** Settings with the data of the Figma frames, for the comparison with the design. */
@Composable
fun SettingsGallery(steps: StepsAccess) {
    SettingsScreen(
        state = SettingsState(
            settings = AppSettings(
                remindersOn = true,
                reminderDays = DayOfWeek.entries.take(5).toSet(),
            ),
            steps = steps,
            glucoseShown = true,
            version = "1.0.0",
        ),
        actions = SettingsActions({}, {}, {}, {}, {}, {}),
    )
}
