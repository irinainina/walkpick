package com.walkspot.app.ui.find

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.walkspot.app.R
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.ui.theme.AppTheme

/** How a kind of find is shown everywhere: its icon, its word and its two colours. */
data class KindLook(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val color: Color,
    /** The pale ground under the icon and the word. */
    val tint: Color,
)

@Composable
fun FindKind.look(): KindLook {
    val colors = AppTheme.colors
    val pale = colors.surfaceTint
    val green = colors.positive
    return when (this) {
        FindKind.PLANT -> KindLook(R.drawable.ic_leaf, R.string.find_plant, green, colors.positiveTint)
        FindKind.BIRD -> KindLook(R.drawable.ic_bird, R.string.find_bird, colors.streak, colors.streakTint)
        FindKind.PLACE -> KindLook(R.drawable.ic_globe, R.string.find_place, colors.accent, pale)
        FindKind.PHOTO -> KindLook(R.drawable.ic_gallery, R.string.find_photo, colors.textInactive, pale)
        FindKind.UNKNOWN -> KindLook(R.drawable.ic_question, R.string.find_unknown, colors.textInactive, pale)
    }
}

/** What a photo can be said to show, in the order the walker is asked. */
val AskedKinds = listOf(FindKind.PLANT, FindKind.BIRD, FindKind.PLACE, FindKind.PHOTO)

/** The word of a kind as an answer to "What is it?". */
@Composable
fun FindKind.answer(): String =
    stringResource(if (this == FindKind.PHOTO) R.string.find_just_photo else look().label)
