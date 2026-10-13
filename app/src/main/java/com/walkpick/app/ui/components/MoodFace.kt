package com.walkpick.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.theme.AppShadows

/** The moods as they are stored: 1 is awful, 5 is great. */
enum class Mood(@StringRes val titleRes: Int, @DrawableRes val faceRes: Int) {
    Awful(R.string.mood_awful, R.drawable.mood_awful),
    Bad(R.string.mood_bad, R.drawable.mood_bad),
    Neutral(R.string.mood_neutral, R.drawable.mood_neutral),
    Good(R.string.mood_good, R.drawable.mood_good),
    Great(R.string.mood_great, R.drawable.mood_great),
    ;

    val value: Int get() = ordinal + 1

    companion object {
        fun of(value: Int): Mood = entries[(value - 1).coerceIn(entries.indices)]
    }
}

@Composable
fun MoodFace(mood: Mood, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Image(
        painter = painterResource(mood.faceRes),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .dropShadow(CircleShape, AppShadows.segment),
    )
}
