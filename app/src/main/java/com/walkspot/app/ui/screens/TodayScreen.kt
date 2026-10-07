package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walkspot.app.R
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.data.walk.WalkWeather
import com.walkspot.app.data.walk.sky
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.GradientHeader
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.ParagraphStyle
import com.walkspot.app.ui.theme.Spacing
import com.walkspot.app.ui.today.TodayState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// Below the status bar: the frame draws 248 with a status bar of 24.
private val HeaderHeight = 224.dp

// The first card starts on the header, this far below the status bar.
private val CardsTop = 168.dp
private val LogoSize = 32.dp
private val GreetingTop = 20.dp
private val GreetingMinSize = 16.sp

private const val NOON = 12
private const val EVENING = 18
private const val NIGHT_END = 5
private const val FAHRENHEIT_PER_C = 1.8
private const val FREEZING_F = 32
private val Date = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

@Composable
fun TodayScreen(
    state: TodayState,
    /** A walk is being recorded: the button leads back to it. */
    walking: Boolean,
    /** The weather outside; null until it is known. */
    weather: WalkWeather?,
    onAllowSteps: () -> Unit,
    onStartWalk: () -> Unit,
    onStats: () -> Unit,
    onHealth: () -> Unit,
    onReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .verticalScroll(rememberScrollState()),
    ) {
        val sky = weather?.let { weatherText(it, state.distanceUnit) }
        Header(statusBar, state.now, state.name, sky, onReminders)
        Column(
            Modifier.padding(
                start = Spacing.ml,
                top = statusBar + CardsTop,
                end = Spacing.ml,
                bottom = Spacing.l,
            ),
        ) {
            if (state.access != StepsAccess.COUNTING) {
                NoStepsCard(sensor = state.access != StepsAccess.NO_SENSOR, onAllow = onAllowSteps)
                Spacer(Modifier.height(Spacing.ml))
            }
            StepsCard(state)
            Spacer(Modifier.height(Spacing.ml))
            StreakCard(state.streak)
            Spacer(Modifier.height(Spacing.ml))
            AppButton(
                text = stringResource(
                    if (walking) R.string.today_back_to_walk else R.string.today_start_walk,
                ),
                onClick = onStartWalk,
                iconRes = R.drawable.ic_play,
            )
            Spacer(Modifier.height(Spacing.l))
            WeekCard(state.week, onStats)
            Spacer(Modifier.height(Spacing.ml))
            HealthCard(state.health, state.units, state.now.toLocalDate(), onHealth)
        }
    }
}

@Composable
private fun Header(statusBar: Dp, now: LocalDateTime, name: String?, sky: String?, onReminders: () -> Unit) {
    val colors = AppTheme.colors
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
                CircleButton(
                    iconRes = R.drawable.ic_bell,
                    contentDescription = stringResource(R.string.today_reminders),
                    onClick = onReminders,
                    modifier = Modifier.background(colors.onAccentOverlay, CircleShape),
                    tint = colors.onAccent,
                )
            }
            Spacer(Modifier.height(GreetingTop))
            val heading = MaterialTheme.typography.headlineLarge
            Text(
                text = greeting(now.hour, name),
                modifier = Modifier.padding(horizontal = Spacing.ml),
                style = heading,
                color = colors.onAccent,
                maxLines = 1,
                // A long name or a phone set to large text: the line shrinks instead of losing the name.
                autoSize = TextAutoSize.StepBased(GreetingMinSize, heading.fontSize),
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = listOfNotNull(now.format(Date), sky).joinToString(" · "),
                modifier = Modifier.padding(horizontal = Spacing.ml),
                style = ParagraphStyle,
                color = colors.onAccent,
            )
        }
    }
}

// Miles go with Fahrenheit: the app has no setting of its own for the temperature.
private fun weatherText(weather: WalkWeather, unit: DistanceUnit): String {
    val fahrenheit = unit == DistanceUnit.MI
    val degrees = if (fahrenheit) weather.celsius * FAHRENHEIT_PER_C + FREEZING_F else weather.celsius
    return "${degrees.roundToInt()}° ${weather.sky()}".trim()
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
