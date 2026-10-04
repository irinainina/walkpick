package com.walkspot.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.walkspot.app.R

val PlusJakartaSans = FontFamily(Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold))

val Cabin = FontFamily(
    Font(R.font.cabin_regular, FontWeight.Normal),
    Font(R.font.cabin_bold, FontWeight.Bold),
)

// Trim.None keeps every line exactly lineHeight tall, glyphs centred - the way Figma lays text out.
private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(family: FontFamily, weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    lineHeightStyle = Trim,
    // Android adds font padding above and below by default, Figma does not
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private fun heading(size: Int, lineHeight: Int) =
    style(PlusJakartaSans, FontWeight.ExtraBold, size, lineHeight)

private fun bold(size: Int, lineHeight: Int) = style(Cabin, FontWeight.Bold, size, lineHeight)

private fun regular(size: Int, lineHeight: Int) = style(Cabin, FontWeight.Normal, size, lineHeight)

// Names follow Material 3 slots, values come from the Figma frames
val AppTypography = Typography(
    displayMedium = heading(36, 44),
    displaySmall = heading(32, 40),
    headlineLarge = heading(28, 36),
    headlineMedium = heading(24, 32),
    headlineSmall = heading(20, 28),
    titleLarge = bold(20, 28),
    titleMedium = bold(18, 24),
    titleSmall = bold(16, 24),
    bodyLarge = regular(16, 20),
    bodyMedium = regular(14, 20),
    bodySmall = regular(12, 16),
    labelLarge = bold(16, 20),
    labelMedium = bold(14, 20),
    labelSmall = bold(12, 16),
)
