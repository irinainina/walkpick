package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.Profile
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.Avatar
import com.walkpick.app.ui.components.CircleButton
import com.walkpick.app.ui.components.GradientHeader
import com.walkpick.app.ui.grouped
import com.walkpick.app.ui.health.HealthState
import com.walkpick.app.ui.profile.ProfileState
import com.walkpick.app.ui.stats.StatsTab
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import java.time.Instant
import kotlin.math.roundToInt

// Below the status bar: the frame draws 232 with a status bar of 24.
private val HeaderHeight = 252.dp

// The totals card starts on the header, this far below the status bar.
private val CardsTop = 188.dp
private val AvatarTop = 76.dp
private val AvatarSize = 72.dp
private val AvatarBorder = 3.dp
private val NameGap = 2.dp
private val TotalsHeight = 88.dp
private const val METRES_IN_KM = 1000.0
private val TotalDividerHeight = 40.dp

@Composable
fun ProfileScreen(
    state: ProfileState,
    onChange: ((Profile) -> Profile) -> Unit,
    onWeight: (Double, Instant) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var editing by rememberSaveable { mutableStateOf<ProfileField?>(null) }
    var weighing by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .verticalScroll(rememberScrollState()),
    ) {
        Header(statusBar, state.name, state.email, onBack)
        Column(Modifier.padding(Spacing.ml, statusBar + CardsTop, Spacing.ml, Spacing.l)) {
            TotalsCard(state)
            ProfileBody(state, onEdit = { editing = it }, onWeigh = { weighing = true })
            ProfileTabs(state.profile, onChange)
        }
    }
    editing?.let { field ->
        ProfileSheet(field, state.profile, state.units, onSave = onChange, onDismiss = { editing = null })
    }
    if (weighing) {
        // The New entry sheet of Health with the weight alone.
        val weight = HealthKind.WEIGHT
        NewEntrySheet(
            state = HealthState(listOf(weight), weight, StatsTab.Month, state.today, units = state.units),
            onSave = { _, kg, _, at -> onWeight(kg, at) },
            onDismiss = { weighing = false },
        )
    }
}

@Composable
private fun Header(statusBar: Dp, name: String?, email: String, onBack: () -> Unit) {
    val colors = AppTheme.colors
    GradientHeader(statusBar + HeaderHeight) {
        CircleButton(
            iconRes = R.drawable.ic_back,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
            modifier = Modifier.padding(start = Spacing.ml, top = statusBar + Spacing.sm),
            raised = true,
        )
        Row(
            modifier = Modifier.padding(start = Spacing.ml, top = statusBar + AvatarTop, end = Spacing.ml),
        ) {
            Avatar(name ?: email, AvatarSize, AvatarBorder, MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.padding(top = Spacing.s)) {
                if (name != null) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(NameGap))
                }
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TotalsCard(state: ProfileState) {
    AppCard {
        Row(Modifier.height(TotalsHeight)) {
            Total(grouped(state.totals.walks), stringResource(R.string.profile_walks))
            TotalDivider()
            val unit = state.units.distance
            val far = (state.totals.metres / METRES_IN_KM * unit.perMetric).roundToInt()
            Total(grouped(far), unit.symbol)
            TotalDivider()
            Total(grouped(state.totals.photos), stringResource(R.string.profile_photos))
        }
    }
}

@Composable
private fun RowScope.Total(value: String, label: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(top = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.textPrimary)
        Spacer(Modifier.height(NameGap))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun TotalDivider() {
    Box(
        Modifier
            .padding(top = Spacing.l)
            .size(Sizes.hairline, TotalDividerHeight)
            .background(AppTheme.colors.divider),
    )
}

@Composable
internal fun SectionTitle(title: String, note: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.l, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = AppTheme.colors.textPrimary,
        )
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
        }
    }
}
