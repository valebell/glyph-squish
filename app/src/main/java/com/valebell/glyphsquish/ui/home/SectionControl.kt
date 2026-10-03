package com.valebell.glyphsquish.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExpressiveSectionControl(
    selectedSection: SquishSection,
    onSelectSection: (SquishSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier.widthIn(
                min = FLOATING_CONTROL_MIN_WIDTH,
                max = FLOATING_CONTROL_MAX_WIDTH,
            ),
        horizontalArrangement =
            Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        SectionControlItem(
            label = stringResource(R.string.form_gallery_title),
            description = stringResource(R.string.show_forms),
            selected = selectedSection == SquishSection.FORMS,
            leading = true,
            onClick = { onSelectSection(SquishSection.FORMS) },
            modifier = Modifier.weight(1f),
        )
        SectionControlItem(
            label = stringResource(R.string.move_gallery_title),
            description = stringResource(R.string.show_moves),
            selected = selectedSection == SquishSection.MOVES,
            leading = false,
            onClick = { onSelectSection(SquishSection.MOVES) },
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionControlItem(
    label: String,
    description: String,
    selected: Boolean,
    leading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { checked ->
            if (checked) onClick()
        },
        colors =
            ToggleButtonDefaults.colors(
                checkedContainerColor = MaterialTheme.colorScheme.onBackground,
                checkedContentColor = MaterialTheme.colorScheme.background,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        modifier =
            modifier
                .heightIn(min = FLOATING_CONTROL_ITEM_HEIGHT)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    role = Role.Tab
                    this.selected = selected
                },
        shapes =
            if (leading) {
                ButtonGroupDefaults.connectedLeadingButtonShapes()
            } else {
                ButtonGroupDefaults.connectedTrailingButtonShapes()
            },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private val FLOATING_CONTROL_MAX_WIDTH = 320.dp
private val FLOATING_CONTROL_MIN_WIDTH = 240.dp
private val FLOATING_CONTROL_ITEM_HEIGHT = 48.dp
