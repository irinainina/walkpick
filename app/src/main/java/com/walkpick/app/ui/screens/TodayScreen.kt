package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walkpick.app.R
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.walk.WalkWeather
import com.walkpick.app.ui.components.Avatar
import com.walkpick.app.ui.components.EmptyState
import com.walkpick.app.ui.components.GradientHeader
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.AvatarLetterStyle
import com.walkpick.app.ui.theme.HeroDateStyle
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.today.NIGHT_END
import com.walkpick.app.ui.today.TodayState
import com.walkpick.app.ui.walk.WalkEnding
import java.time.format.DateTimeFormatter
import java.util.Locale

// Below the status bar: the frame draws 300 with a status bar of 24.
private val HeaderHeight = 276.dp

// The first card starts on the header, this far below the status bar.
private val CardsTop = 220.dp
private val LogoSize = 32.dp

private val GreetingTop = 16.dp
private val DateTop = 4.dp
private val PillTop = 12.dp
private val GreetingMinSize = 16.sp

// Under the notice, as the frame of the first walk draws it.
private val EmptyTop = 58.dp

private const val NOON = 12
private const val EVENING = 18
private val Date = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

/** What the Home tab asks of its owner. */
class TodayActions(
    val onAllowSteps: () -> Unit,
    val onProfile: () -> Unit,
    /** Opens this week on the Walk tab. */
    val onWeek: () -> Unit,
    /** A bar of the week is tapped, by its index from Monday. */
    val onDay: (Int) -> Unit,
)

@Composable
fun TodayScreen(
    state: TodayState,
    /** The time of the walk being recorded, and then the button leads back to it; null without one. */
    walkSeconds: Long?,
    /** The walk under way is paused: the link under Stop walk says when. */
    walkPaused: Boolean,
    /** What stopping that walk now would do. */
    walkEnding: WalkEnding,
    /** Settings: whether a walk is started with the question how the walker feels. */
    walkMoods: Boolean,
    /** The weather outside; null until it is known. */
    weather: WalkWeather?,
    actions: TodayActions,
    /** Starts a walk with the mood it begins in, or leads back to the walk under way. */
    onStartWalk: (Int?) -> Unit,
    onStopWalk: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas),
    ) {
        Box(Modifier.weight(1f)) {
            Cards(statusBar, state, weather, walkSeconds != null, actions)
            StartBarFade(Modifier.align(Alignment.BottomCenter))
        }
        StartBar(walkSeconds, walkPaused, walkEnding, walkMoods, onStartWalk, onStopWalk)
    }
}

@Composable
private fun Cards(
    statusBar: Dp,
    state: TodayState,
    weather: WalkWeather?,
    walking: Boolean,
    actions: TodayActions,
) {
    Box(Modifier.verticalScroll(rememberScrollState())) {
        Header(statusBar, state, weather, walking, actions.onProfile)
        Column(Modifier.padding(top = statusBar + CardsTop, bottom = Spacing.l)) {
            val lastWalk = state.lastWalk
            Column(Modifier.padding(horizontal = Spacing.ml)) {
                Notes(state, actions)
                if (lastWalk != null) {
                    StepsCard(state, lastWalk)
                    Spacer(Modifier.height(Spacing.m))
                    StreakCard(state.streak)
                    Spacer(Modifier.height(Spacing.m))
                    WeekCard(state.week, actions.onWeek, actions.onDay)
                }
            }
            if (lastWalk == null && state.newcomer) {
                // Only the database may say there is no walk: while the last one is read, nothing shows.
                EmptyState(
                    iconRes = R.drawable.ic_route,
                    title = stringResource(R.string.walks_empty_title),
                    text = stringResource(R.string.walks_empty_text),
                    modifier = Modifier.padding(top = EmptyTop),
                )
            }
        }
    }
}

// A word on top when the steps are not counted or nobody has walked yet.
@Composable
private fun Notes(state: TodayState, actions: TodayActions) {
    if (state.access != StepsAccess.COUNTING) {
        NoStepsCard(sensor = state.access != StepsAccess.NO_SENSOR, onAllow = actions.onAllowSteps)
        Spacer(Modifier.height(Spacing.ml))
    } else if (state.newcomer) {
        FirstWalkCard()
        Spacer(Modifier.height(Spacing.ml))
    }
}

@Composable
private fun Header(
    statusBar: Dp,
    state: TodayState,
    weather: WalkWeather?,
    walking: Boolean,
    onProfile: () -> Unit,
) {
    val colors = AppTheme.colors
    val now = state.now
    GradientHeader(statusBar + HeaderHeight) {
        Column(Modifier.padding(top = statusBar + Spacing.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.m, end = Spacing.ml),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(LogoSize),
                    tint = colors.onAccent,
                )
                Spacer(Modifier.width(Spacing.s))
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onAccent,
                )
                val profile = stringResource(R.string.tab_profile)
                Avatar(
                    name = state.name.orEmpty(),
                    size = Sizes.circle,
                    ring = Sizes.focusBorder,
                    style = AvatarLetterStyle,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = profile, onClick = onProfile)
                        .semantics { contentDescription = profile },
                )
            }
            Spacer(Modifier.height(GreetingTop))
            val heading = MaterialTheme.typography.displaySmall
            Text(
                text = greeting(now.hour, state.name),
                modifier = Modifier.padding(horizontal = Spacing.ml),
                style = heading,
                color = colors.onAccent,
                maxLines = 1,
                // A long name or a phone set to large text: the line shrinks instead of losing the name.
                autoSize = TextAutoSize.StepBased(GreetingMinSize, heading.fontSize),
            )
            Spacer(Modifier.height(DateTop))
            Text(
                text = now.format(Date),
                modifier = Modifier.padding(horizontal = Spacing.ml),
                style = HeroDateStyle,
                color = colors.onAccent,
            )
            Spacer(Modifier.height(PillTop))
            WeatherPill(weather, state.distanceUnit, now.hour, walking)
        }
    }
}

@Composable
private fun greeting(hour: Int, name: String?): String {
    val partOfDay = stringResource(
        when (hour) {
            in NIGHT_END until NOON -> R.string.today_morning
            in NOON until EVENING -> R.string.today_afternoon
            else -> R.string.today_evening
        },
    )
    return if (name == null) partOfDay else stringResource(R.string.today_greeting_name, partOfDay, name)
}
