package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.AppSheet
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.PhotoBox
import com.walkspot.app.ui.find.FindCardState
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.ParagraphStyle
import com.walkspot.app.ui.theme.SheetShape
import com.walkspot.app.ui.theme.Spacing

private val PhotoHeight = 380.dp

// The white sheet with the text rises this far over the bottom of the photo.
private val SheetOverlap = 32.dp

/** One find: its photo, what it is, where it was taken and the walk it belongs to. */
@Composable
fun FindCardScreen(
    state: FindCardState,
    onBack: () -> Unit,
    onBest: () -> Unit,
    onDelete: () -> Unit,
    onWalk: (String) -> Unit,
    onChoose: (Int) -> Unit,
    onRename: (String) -> Unit,
    /** Asks the services again, as the given kind when the find was unknown. */
    onRetry: (FindKind?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    var deleting by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var asking by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        PhotoBox(state.photo, Modifier.fillMaxWidth().height(PhotoHeight))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.ml, vertical = Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            CircleButton(R.drawable.ic_back, stringResource(R.string.back), onBack, raised = true)
            Spacer(Modifier.weight(1f))
            val best = state.find.isBest
            CircleButton(
                iconRes = R.drawable.ic_star,
                contentDescription = stringResource(if (best) R.string.find_not_best else R.string.find_best),
                onClick = onBest,
                // A find among the best wears a filled star button.
                tint = if (best) colors.onAccent else colors.streak,
                raised = !best,
                ground = colors.streak,
            )
            val more = stringResource(R.string.walk_more)
            CircleButton(R.drawable.ic_more, more, { deleting = true }, raised = true)
        }
        FindCardBody(
            state = state,
            onWalk = onWalk,
            onNotThis = { renaming = true },
            onName = onRename,
            // An unknown find has lost its kind: the walker says again what it is.
            onRetry = { if (state.find.kind == FindKind.UNKNOWN) asking = true else onRetry(null) },
            modifier = Modifier
                .padding(top = PhotoHeight - SheetOverlap)
                .fillMaxWidth()
                .background(colors.surface, SheetShape)
                .navigationBarsPadding(),
        )
    }
    if (renaming) {
        NotThisSheet(
            find = state.find,
            onDismiss = { renaming = false },
            onChoose = {
                onChoose(it)
                renaming = false
            },
            onRename = {
                onRename(it)
                renaming = false
            },
        )
    }
    if (asking) {
        KindSheet(onDismiss = { asking = false }) {
            onRetry(it)
            asking = false
        }
    }
    if (deleting) {
        AppSheet(stringResource(R.string.find_delete_title), onDismiss = { deleting = false }) {
            Spacer(Modifier.height(Spacing.s))
            val warning = stringResource(R.string.find_delete_text)
            Text(warning, style = ParagraphStyle, color = colors.textSecondary)
            Spacer(Modifier.height(Spacing.l))
            AppButton(stringResource(R.string.find_delete), onDelete, style = AppButtonStyle.Danger)
            Spacer(Modifier.height(Spacing.sm))
            val cancel = stringResource(R.string.settings_cancel)
            AppButton(cancel, { deleting = false }, style = AppButtonStyle.Secondary)
        }
    }
}
