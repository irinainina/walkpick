package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import com.walkspot.app.R
import com.walkspot.app.data.profile.Profile
import com.walkspot.app.ui.compact
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.GradientHeader
import com.walkspot.app.ui.components.IconBadge
import com.walkspot.app.ui.components.ListRow
import com.walkspot.app.ui.grouped
import com.walkspot.app.ui.profile.ProfileState
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing

// Below the status bar: the frame draws 232 with a status bar of 24.
private val HeaderHeight = 208.dp

// The totals card starts on the header, this far below the status bar.
private val CardsTop = 144.dp
private val AvatarSize = 72.dp
private val AvatarBorder = 3.dp
private val NameGap = 2.dp
private val TotalsHeight = 88.dp
private val TotalDividerHeight = 40.dp

@Composable
fun ProfileScreen(
    state: ProfileState,
    onChange: ((Profile) -> Profile) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var editing by rememberSaveable { mutableStateOf<ProfileField?>(null) }
    Box(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .verticalScroll(rememberScrollState()),
    ) {
        Header(statusBar, state.name, state.email)
        Column(Modifier.padding(Spacing.ml, statusBar + CardsTop, Spacing.ml, Spacing.l)) {
            TotalsCard(state)
            Spacer(Modifier.height(Spacing.ml))
            AppCard {
                ListRow(
                    label = stringResource(R.string.profile_goal),
                    value = grouped(state.profile.dailyGoal),
                    divider = false,
                    onClick = { editing = ProfileField.Goal },
                    leading = {
                        val colors = AppTheme.colors
                        IconBadge(R.drawable.ic_flame, colors.streakTint, colors.streak, Sizes.iconSmall)
                    },
                )
            }
            ProfileBody(state, onEdit = { editing = it })
            ProfileTabs(state.profile, onChange)
            Spacer(Modifier.height(Spacing.ml))
            AppCard {
                ListRow(
                    label = stringResource(R.string.screen_settings),
                    divider = false,
                    onClick = onSettings,
                    leading = {
                        val colors = AppTheme.colors
                        IconBadge(R.drawable.ic_settings, colors.surfaceTint, colors.accent, Sizes.iconSmall)
                    },
                )
            }
        }
    }
    editing?.let { field ->
        ProfileSheet(field, state.profile, state.units, onSave = onChange, onDismiss = { editing = null })
    }
}

@Composable
private fun Header(statusBar: Dp, name: String?, email: String) {
    val colors = AppTheme.colors
    GradientHeader(statusBar + HeaderHeight) {
        Row(
            modifier = Modifier.padding(start = Spacing.ml, top = statusBar + Spacing.xl, end = Spacing.ml),
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .background(colors.surfaceTint, CircleShape)
                    .border(AvatarBorder, colors.onAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (name ?: email).take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.accent,
                )
            }
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
            Total(compact(state.totals.steps), R.string.profile_steps)
            TotalDivider()
            Total(grouped(state.totals.walks), R.string.profile_walks)
            TotalDivider()
            Total(grouped(state.totals.photos), R.string.profile_photos)
        }
    }
}

@Composable
private fun RowScope.Total(value: String, labelRes: Int) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(top = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.textPrimary)
        Spacer(Modifier.height(NameGap))
        Text(
            text = stringResource(labelRes),
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
