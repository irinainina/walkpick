package com.walkpick.app.debug

import androidx.compose.runtime.Composable
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.Profile
import com.walkpick.app.data.profile.Sex
import com.walkpick.app.ui.profile.ProfileState
import com.walkpick.app.ui.profile.Totals
import com.walkpick.app.ui.screens.ProfileScreen
import java.time.LocalDate

/** Profile with the data of the Figma frame, for the comparison with the design. */
@Composable
fun ProfileGallery() {
    ProfileScreen(
        state = ProfileState(
            today = LocalDate.of(2026, 9, 30),
            name = "Ira",
            email = "ira@example.com",
            profile = Profile(
                sex = Sex.FEMALE,
                birthDate = LocalDate.of(1992, 3, 14),
                heightCm = 168,
                ownTargetWeightKg = 62.0,
                healthTabs = HealthKind.entries.toSet(),
            ),
            totals = Totals(steps = 2_790_000, walks = 312, photos = 540),
            weightKg = 64.2,
        ),
        onChange = {},
        onWeight = { _, _ -> },
        onSettings = {},
    )
}
