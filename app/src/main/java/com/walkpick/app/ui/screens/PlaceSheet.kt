package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.places.NearPlace
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppSheet
import com.walkpick.app.ui.components.AppTextField
import com.walkpick.app.ui.distanceText
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.ParagraphStyle
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import kotlin.math.roundToInt

private val RowHeight = 56.dp

// More rows would push Save out of the sheet; the rest still wait under "Not this?".
private const val SHOWN = 5

/**
 * "Which one is it?": the places near a photo of a place, the walker's own names first, and a field
 * for a name of their own. The first place is picked until another one is tapped or a name typed.
 */
@Composable
internal fun PlaceSheet(
    places: List<NearPlace>,
    unit: DistanceUnit,
    onSave: (NearPlace, List<NearPlace>) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by rememberSaveable { mutableIntStateOf(0) }
    var typed by rememberSaveable { mutableStateOf("") }
    val save = {
        val own = typed.trim().takeIf { it.isNotEmpty() }?.let { NearPlace(it, distanceM = 0) }
        (own ?: places.getOrNull(picked))?.let { onSave(it, places) }
        Unit
    }
    AppSheet(stringResource(R.string.place_which), onDismiss) {
        val colors = AppTheme.colors
        val type = MaterialTheme.typography
        Spacer(Modifier.height(Spacing.xs))
        val note = if (places.isEmpty()) R.string.place_none else R.string.place_near
        Text(stringResource(note), style = type.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(Spacing.sm))
        places.take(SHOWN).forEachIndexed { index, place ->
            val chosen = index == picked && typed.isBlank()
            Row(
                modifier = Modifier
                    .padding(bottom = Spacing.xs)
                    .fillMaxWidth()
                    .height(RowHeight)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (chosen) colors.surfaceTint else colors.surface)
                    .selectable(chosen, role = Role.RadioButton) {
                        picked = index
                        typed = ""
                    }
                    .padding(horizontal = Spacing.m, vertical = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaceText(place, chosen, unit, Modifier.weight(1f))
                if (chosen) {
                    val check = painterResource(R.drawable.ic_check)
                    Icon(check, null, Modifier.size(Sizes.iconSmall), colors.accent)
                }
            }
        }
        Spacer(Modifier.height(Spacing.s))
        AppTextField(
            value = typed,
            onValueChange = { typed = it },
            placeholder = stringResource(R.string.place_other),
            onDone = save,
            autoFocus = false,
            textStyle = type.bodyLarge,
        )
        Spacer(Modifier.height(Spacing.sm))
        AppButton(stringResource(R.string.profile_save), onClick = save)
    }
}

@Composable
private fun PlaceText(place: NearPlace, chosen: Boolean, unit: DistanceUnit, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    Column(modifier) {
        Text(
            text = place.name,
            style = if (chosen) type.titleSmall else ParagraphStyle,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val kind = place.kind ?: stringResource(R.string.place_yours)
        Text(
            text = "$kind · ${nearText(place.distanceM, unit)}",
            style = type.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

// A place a few steps away is "40 m", not "0.0 km"; in miles it is feet.
internal fun nearText(metres: Int, unit: DistanceUnit): String = when {
    metres >= METRES_IN_KM -> distanceText(metres, unit)
    unit == DistanceUnit.KM -> "$metres m"
    else -> "${(metres * FEET_IN_METRE).roundToInt()} ft"
}

private const val METRES_IN_KM = 1000
private const val FEET_IN_METRE = 3.28084
