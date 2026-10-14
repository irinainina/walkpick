package com.walkpick.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walkpick.app.R
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.walk.WalkWeather
import com.walkpick.app.data.walk.sky
import com.walkpick.app.ui.components.Avatar
import com.walkpick.app.ui.components.GradientHeader
import com.walkpick.app.ui.map.DiaryWalk
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.AvatarLetterStyle
import com.walkpick.app.ui.theme.ParagraphStyle
import com.walkpick.app.ui.theme.PillShape
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.today.DARK_FROM
import com.walkpick.app.ui.today.NIGHT_END
import com.walkpick.app.ui.today.TodayState
import com.walkpick.app.ui.today.weatherCall
import com.walkpick.app.ui.walk.WalkEnding
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// Below the status bar: the frame draws 272 with a status bar of 24.
private val HeaderHeight = 248.dp

// The first card starts on the header, this far below the status bar.
private val CardsTop = 192.dp
private val LogoSize = 32.dp

// The author keeps the header as drawn (2026-10-14), odd steps included.
private val GreetingTop = 6.dp
private val DateTop = 2.dp
private val PillTop = 10.dp
private val GreetingMinSize = 16.sp
private val PillHeight = 32.dp

private const val NOON = 12
private const val EVENING = 18
private const val FAHRENHEIT_PER_C = 1.8
private const val FREEZING_F = 32
private val Date = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

/** What the Today tab asks of its owner. */
class TodayActions(
    val onAllowSteps: () -> Unit,
    val onStats: () -> Unit,
    val onHealth: () -> Unit,
    val onAddHealth: (HealthKind) -> Unit,
    val onWalk: (String) -> Unit,
    val onProfile: () -> Unit,
)

@Composable
fun TodayScreen(
    state: TodayState,
    /** The time of the walk being recorded, and then the button leads back to it; null without one. */
    walkSeconds: Long?,
    /** What stopping that walk now would do. */
    walkEnding: WalkEnding,
    /** The weather outside; null until it is known. */
    weather: WalkWeather?,
    /** The latest walk; null before the first one. */
    lastWalk: DiaryWalk?,
    actions: TodayActions,
    onStartWalk: () -> Unit,
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
            Cards(statusBar, state, weather, walkSeconds != null, lastWalk, actions)
            StartBarFade(Modifier.align(Alignment.BottomCenter))
        }
        StartBar(walkSeconds, walkEnding, onStartWalk, onStopWalk)
    }
}

@Composable
private fun Cards(
    statusBar: Dp,
    state: TodayState,
    weather: WalkWeather?,
    walking: Boolean,
    lastWalk: DiaryWalk?,
    actions: TodayActions,
) {
    Box(Modifier.verticalScroll(rememberScrollState())) {
        Header(statusBar, state, weather, walking, actions.onProfile)
        Column(
            Modifier.padding(
                start = Spacing.ml,
                top = statusBar + CardsTop,
                end = Spacing.ml,
                bottom = Spacing.l,
            ),
        ) {
            if (state.access != StepsAccess.COUNTING) {
                NoStepsCard(sensor = state.access != StepsAccess.NO_SENSOR, onAllow = actions.onAllowSteps)
                Spacer(Modifier.height(Spacing.ml))
            } else if (state.newcomer) {
                FirstWalkCard()
                Spacer(Modifier.height(Spacing.ml))
            }
            StepsCard(state)
            Spacer(Modifier.height(Spacing.ml))
            StreakCard(state.streak)
            Spacer(Modifier.height(Spacing.ml))
            WeekCard(state.week, actions.onStats)
            Spacer(Modifier.height(Spacing.l))
            if (lastWalk != null) {
                val today = state.now.toLocalDate()
                LastWalkCard(lastWalk, today, state.distanceUnit) { actions.onWalk(lastWalk.walk.id) }
                Spacer(Modifier.height(Spacing.l))
            }
            val today = state.now.toLocalDate()
            HealthCard(state.health, state.units, today, actions.onHealth, actions.onAddHealth)
        }
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
            val heading = MaterialTheme.typography.headlineLarge
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
                style = ParagraphStyle,
                color = colors.onAccent,
            )
            Spacer(Modifier.height(PillTop))
            WeatherPill(weather, state.distanceUnit, now.hour, walking)
        }
    }
}

@Composable
private fun WeatherPill(weather: WalkWeather?, unit: DistanceUnit, hour: Int, walking: Boolean) {
    val colors = AppTheme.colors
    val call = stringResource(weatherCall(weather, hour, walking))
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.ml)
            .height(PillHeight)
            .background(colors.onAccentOverlay, PillShape)
            .padding(horizontal = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (weather != null) {
            Icon(
                painter = painterResource(weather.iconRes(dark = hour !in NIGHT_END until DARK_FROM)),
                contentDescription = weather.sky(),
                modifier = Modifier.size(Sizes.iconSmall),
                tint = colors.onAccent,
            )
        }
        Text(
            text = weather
                ?.let { stringResource(R.string.today_weather_line, degreesText(it, unit), call) }
                ?: call,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@DrawableRes
private fun WalkWeather.iconRes(dark: Boolean) = when (sky()) {
    "Clear" -> if (dark) R.drawable.ic_weather_night else R.drawable.ic_weather_clear
    "Fog" -> R.drawable.ic_weather_fog
    "Rain" -> R.drawable.ic_weather_rain
    "Snow" -> R.drawable.ic_weather_snow
    "Storm" -> R.drawable.ic_weather_storm
    else -> R.drawable.ic_weather_cloud
}

// Miles go with Fahrenheit: the app has no setting of its own for the temperature.
private fun degreesText(weather: WalkWeather, unit: DistanceUnit): String {
    val fahrenheit = unit == DistanceUnit.MI
    val degrees = if (fahrenheit) weather.celsius * FAHRENHEIT_PER_C + FREEZING_F else weather.celsius
    return "${degrees.roundToInt()}°"
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
