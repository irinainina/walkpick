package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.walkspot.app.R
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.AppSheet
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
    /** Takes the id of the entry to remove. */
    onDelete: (String) -> Unit,
    onTab: (HealthKind) -> Unit,
    onPeriod: (StatsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
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
        // A slip of the finger in a number would stay in the chart for good without this.
        if (state.current != null) {
            Text(
                text = stringResource(R.string.health_delete_last),
                modifier = Modifier
                    .padding(top = Spacing.s)
                    .align(Alignment.CenterHorizontally)
                    .clickable(role = Role.Button) { deleting = true }
                    .padding(Spacing.sm),
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.textSecondary,
            )
        }
        Spacer(Modifier.height(Spacing.l))
    }
    if (adding) NewEntrySheet(state, onSave, onDismiss = { adding = false })
    val last = state.current?.latest
    if (deleting && last != null) {
        DeleteEntrySheet(
            moment = momentText(last.measuredAt, state.today),
            onDelete = {
                deleting = false
                onDelete(last.id)
            },
            onDismiss = { deleting = false },
        )
    }
}

@Composable
private fun DeleteEntrySheet(moment: String, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AppSheet(stringResource(R.string.health_delete_title), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(R.string.health_delete_text, moment),
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.l))
        AppButton(stringResource(R.string.health_delete), onClick = onDelete, style = AppButtonStyle.Danger)
        Spacer(Modifier.height(Spacing.sm))
        AppButton(stringResource(R.string.settings_cancel), onDismiss, style = AppButtonStyle.Secondary)
    }
}
