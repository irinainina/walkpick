package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.ui.components.FindPin
import com.walkspot.app.ui.find.AskedKinds
import com.walkspot.app.ui.find.answer
import com.walkspot.app.ui.find.look
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing

private val AskBottom = 60.dp
private val KindHeight = 64.dp
private const val KINDS_IN_ROW = 2

/** "What is it?" under the photo just taken: the answer decides which service is asked for the name. */
@Composable
internal fun KindAsk(onKind: (FindKind) -> Unit) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.photoBar)
            .navigationBarsPadding()
            .padding(start = Spacing.ml, top = Spacing.ml, end = Spacing.ml, bottom = AskBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.find_what_is_it),
            modifier = Modifier.padding(bottom = Spacing.xs),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onAccent,
        )
        AskedKinds.chunked(KINDS_IN_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                row.forEach { KindButton(it, onKind, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KindButton(kind: FindKind, onKind: (FindKind) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .height(KindHeight)
            .clip(MaterialTheme.shapes.large)
            .background(colors.photoChrome)
            .clickable(role = Role.Button) { onKind(kind) }
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FindPin(kind.look())
        Spacer(Modifier.width(Spacing.sm))
        val label = MaterialTheme.typography.titleSmall
        Text(kind.answer(), style = label, color = colors.onAccent, maxLines = 1)
    }
}
