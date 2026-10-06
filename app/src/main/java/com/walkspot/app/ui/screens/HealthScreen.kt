package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.walkspot.app.R
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.ui.components.AppTopBar
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.SegmentedControl
import com.walkspot.app.ui.health.HealthState
import com.walkspot.app.ui.stats.StatsTab
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing
import java.time.Instant

@Composable
fun HealthScreen(
    state: HealthState,
    onBack: () -> Unit,
    onSave: (HealthKind, Double, Int?, Instant) -> Unit,
    onTab: (HealthKind) -> Unit,
    onPeriod: (StatsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar(stringResource(R.string.screen_health), onBack, Modifier.padding(top = Spacing.xs)) {
            CircleButton(
                iconRes = R.drawable.ic_plus,
                contentDescription = stringResource(R.string.health_new_entry),
                onClick = { adding = true },
                tint = AppTheme.colors.accent,
                raised = true,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        // A single tab has nothing to switch to.
        if (state.tabs.size > 1) {
            SegmentedControl(
                options = state.tabs.map { stringResource(it.titleRes()) },
                selectedIndex = state.tabs.indexOf(state.tab),
                onSelect = { onTab(state.tabs[it]) },
                modifier = Modifier.padding(horizontal = Spacing.ml),
            )
            Spacer(Modifier.height(Spacing.ml))
        }
        CurrentCard(state, Modifier.padding(horizontal = Spacing.ml))
        Spacer(Modifier.height(Spacing.ml))
        SegmentedControl(
            options = StatsTab.entries.map { stringResource(it.titleRes()) },
            selectedIndex = state.period.ordinal,
            onSelect = { onPeriod(StatsTab.entries[it]) },
            modifier = Modifier.padding(horizontal = Spacing.ml),
        )
        val chart = state.chart
        if (chart != null) {
            Spacer(Modifier.height(Spacing.ml))
            ChartCard(state, chart, Modifier.padding(horizontal = Spacing.ml))
        }
        state.insight?.let {
            Spacer(Modifier.height(Spacing.ml))
            InsightCard(it, state, Modifier.padding(horizontal = Spacing.ml))
        }
        Spacer(Modifier.height(Spacing.l))
    }
    if (adding) NewEntrySheet(state, onSave, onDismiss = { adding = false })
}
