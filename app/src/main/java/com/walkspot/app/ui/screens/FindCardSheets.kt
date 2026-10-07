package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.find.options
import com.walkspot.app.data.local.FindAlternative
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppSheet
import com.walkspot.app.ui.components.AppTextField
import com.walkspot.app.ui.components.IconBadge
import com.walkspot.app.ui.components.ListRow
import com.walkspot.app.ui.find.look
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing
import kotlin.math.roundToInt

private val OptionHeight = 56.dp
private val PickedBorder = 2.dp
private const val PERCENT = 100
private const val LONGEST_NAME = 60

/**
 * The other names the service offered and a field for a name of the walker's own. Save takes the
 * typed name when there is one, the picked row otherwise.
 */
@Composable
internal fun NotThisSheet(
    find: FindEntity,
    onDismiss: () -> Unit,
    onChoose: (Int) -> Unit,
    onRename: (String) -> Unit,
) {
    var picked by rememberSaveable { mutableIntStateOf(0) }
    var typed by rememberSaveable { mutableStateOf("") }
    val save = { if (typed.isNotBlank()) onRename(typed) else onChoose(picked) }
    AppSheet(stringResource(R.string.find_not_this), onDismiss) {
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.find_not_this_text),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.sm))
        find.options().forEachIndexed { index, option ->
            if (index > 0) Spacer(Modifier.height(Spacing.xs))
            Option(option, picked = index == picked && typed.isBlank()) {
                picked = index
                typed = ""
            }
        }
        Spacer(Modifier.height(Spacing.s))
        AppTextField(
            value = typed,
            onValueChange = { typed = it.take(LONGEST_NAME) },
            placeholder = stringResource(R.string.find_enter_name),
            onDone = save,
            autoFocus = false,
        )
        Spacer(Modifier.height(Spacing.sm))
        AppButton(stringResource(R.string.profile_save), onClick = save)
    }
}

@Composable
private fun Option(option: FindAlternative, picked: Boolean, onPick: () -> Unit) {
    val colors = AppTheme.colors
    val type = MaterialTheme.typography
    val shape = MaterialTheme.shapes.medium
    val frame = if (picked) {
        Modifier
            .background(colors.surfaceTint, shape)
            .border(PickedBorder, colors.accent, shape)
    } else {
        Modifier.border(Sizes.hairline, colors.divider, shape)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(OptionHeight)
            .clip(shape)
            .then(frame)
            .clickable(onClick = onPick)
            .padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(option.name, style = type.labelLarge, color = colors.textPrimary, maxLines = 1)
            // A bird the model offers is known by its Latin name alone: once is enough.
            option.latinName?.takeIf { it != option.name }?.let {
                Spacer(Modifier.height(Spacing.xs / 2))
                Text(it, style = type.bodySmall, color = colors.textSecondary, maxLines = 1)
            }
        }
        // A place and a name typed by hand have no match to show.
        if (option.match > 0) {
            val percent = stringResource(R.string.find_percent, (option.match * PERCENT).roundToInt())
            val shade = if (picked) colors.accent else colors.textSecondary
            Text(percent, style = type.labelMedium, color = shade)
        }
        // The check keeps its room on every row, so that the percents stand in one column.
        Spacer(Modifier.width(Spacing.m))
        if (picked) {
            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(Sizes.icon), colors.accent)
        } else {
            Spacer(Modifier.width(Sizes.icon))
        }
    }
}

/**
 * Under a find that has no name: a field to name it by hand, for one the services did not know, and
 * the way to ask them again.
 */
@Composable
internal fun UnnamedControls(unknown: Boolean, onName: (String) -> Unit, onRetry: () -> Unit) {
    val colors = AppTheme.colors
    var typed by rememberSaveable { mutableStateOf("") }
    Spacer(Modifier.height(Spacing.m))
    if (unknown) {
        AppTextField(
            value = typed,
            onValueChange = { typed = it.take(LONGEST_NAME) },
            placeholder = stringResource(R.string.find_name_it),
            onDone = { onName(typed) },
            autoFocus = false,
        )
        Spacer(Modifier.height(Spacing.m))
    }
    val row = Modifier
        .height(Spacing.l)
        .clickable(onClick = onRetry)
    Row(row, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_retry), null, Modifier.size(Sizes.iconSmall), colors.accent)
        Spacer(Modifier.width(Spacing.s))
        Text(
            text = stringResource(R.string.find_try_again),
            style = MaterialTheme.typography.labelLarge,
            color = colors.accent,
        )
    }
}

/** What an unknown find is, asked before the services are tried again. */
@Composable
internal fun KindSheet(onDismiss: () -> Unit, onKind: (FindKind) -> Unit) {
    val kinds = listOf(FindKind.PLANT, FindKind.BIRD, FindKind.PLACE)
    AppSheet(stringResource(R.string.find_what_is_it), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        kinds.forEachIndexed { index, kind ->
            val look = kind.look()
            ListRow(
                label = stringResource(look.label),
                divider = index < kinds.lastIndex,
                onClick = { onKind(kind) },
                leading = { IconBadge(look.icon, look.tint, look.color, Sizes.iconSmall) },
            )
        }
    }
}
