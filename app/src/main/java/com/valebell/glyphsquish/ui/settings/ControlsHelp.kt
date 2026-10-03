package com.valebell.glyphsquish.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.ui.components.SquishHelpDialog
import com.valebell.glyphsquish.ui.theme.SquishSpacing

@Composable
internal fun ControlsHelp(onDismiss: () -> Unit) {
    SquishHelpDialog(
        title = stringResource(R.string.settings_how_to_play),
        onDismiss = onDismiss,
    ) {
        Text(stringResource(R.string.controls_glyph_context))
        GestureInstruction(R.string.controls_move, R.string.controls_move_description)
        GestureInstruction(R.string.controls_form, R.string.controls_form_description)
        GestureInstruction(R.string.controls_squish, R.string.controls_squish_description)
    }
}

@Composable
private fun GestureInstruction(
    titleResource: Int,
    descriptionResource: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SquishSpacing.Tiny)) {
        Text(
            text = stringResource(titleResource),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(descriptionResource),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
