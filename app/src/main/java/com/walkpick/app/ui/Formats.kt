package com.walkpick.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.settings.GlucoseUnit
import com.walkpick.app.data.settings.WeightUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

private const val METRES_IN_KM = 1000.0
private const val WHOLE_FROM = 100

/**
 * "1 day" or "21 days". Android plurals follow the language of the phone, and in Russian
 * 21 takes the singular; the texts of the app are English whatever the phone speaks.
 */
@Composable
fun counted(count: Int, @StringRes one: Int, @StringRes many: Int): String =
    stringResource(if (count == 1) one else many, count)

/** 6420 -> "6,420". */
fun grouped(number: Int): String = String.format(Locale.ENGLISH, "%,d", number)

/** "0 km", "4.8 km", "1,209 km" - or miles, when the user chose them. */
fun distanceText(metres: Int, unit: DistanceUnit): String {
    val value = metres / METRES_IN_KM * unit.perMetric
    val number = when {
        metres == 0 -> "0"
        value < WHOLE_FROM -> oneDecimal(value)
        else -> grouped(value.toInt())
    }
    return "$number ${unit.symbol}"
}

fun weightText(kg: Double, unit: WeightUnit): String = "${oneDecimal(kg * unit.perMetric)} ${unit.symbol}"

/** mmol/L keeps a decimal, mg/dL is written whole. */
fun glucoseNumber(mmol: Double, unit: GlucoseUnit): String =
    if (unit == GlucoseUnit.MMOL) oneDecimal(mmol) else (mmol * unit.perMetric).roundToInt().toString()

fun glucoseText(mmol: Double, unit: GlucoseUnit): String = "${glucoseNumber(mmol, unit)} ${unit.symbol}"

fun oneDecimal(value: Double): String = String.format(Locale.ENGLISH, "%.1f", value)

fun Instant.localDay(zone: ZoneId = ZoneId.systemDefault()): LocalDate = atZone(zone).toLocalDate()
