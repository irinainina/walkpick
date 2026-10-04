package com.walkspot.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.walkspot.app.R
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.AppChip
import com.walkspot.app.ui.components.AppNavBar
import com.walkspot.app.ui.components.AppSwitch
import com.walkspot.app.ui.components.AppTopBar
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.ListRow
import com.walkspot.app.ui.components.SegmentedControl
import com.walkspot.app.ui.navigation.bottomTabs
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing

private val periods = listOf("Week", "Month", "Year", "All")
private val kinds = listOf("All", "Plants", "Birds", "Places")
private val healthTabs = listOf("Mood", "Weight", "Pressure", "Glucose")

/** Every shared component in the state the Figma frames draw it, for the comparison with the design. */
@Composable
fun ComponentGallery() {
    var period by rememberSaveable { mutableIntStateOf(0) }
    var kind by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableStateOf(bottomTabs.first().route) }
    var switchedOff by rememberSaveable { mutableStateOf(emptySet<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        AppTopBar(title = "Title", onBack = {}) {
            CircleButton(R.drawable.ic_plus, "Add", {}, tint = AppTheme.colors.accent, raised = true)
        }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.ml),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            SegmentedControl(periods, period, onSelect = { period = it })
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                kinds.forEachIndexed { index, name ->
                    AppChip(name, selected = index == kind, onClick = { kind = index })
                }
            }
            AppCard {
                healthTabs.forEachIndexed { index, name ->
                    ListRow(label = name, divider = index != healthTabs.lastIndex) {
                        AppSwitch(
                            on = name !in switchedOff,
                            onToggle = { switchedOff = if (it) switchedOff - name else switchedOff + name },
                            label = name,
                        )
                    }
                }
            }
            AppButton("Start walk", onClick = {}, iconRes = R.drawable.ic_play)
            AppButton(
                text = "Continue with Google",
                onClick = {},
                style = AppButtonStyle.Secondary,
                iconRes = R.drawable.ic_google,
            )
        }
        AppNavBar(bottomTabs, selectedRoute = tab, onSelect = { tab = it.route })
    }
}
