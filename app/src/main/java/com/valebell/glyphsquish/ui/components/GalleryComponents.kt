package com.valebell.glyphsquish.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.ui.theme.SquishSpacing

@Composable
internal fun GalleryHeader(
    title: String,
    progress: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(SquishSpacing.Tiny),
    ) {
        SectionLabel(title)
        Text(
            text = progress,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun GalleryCard(
    onClick: () -> Unit,
    description: String,
    selected: Boolean,
    actionLabel: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardModifier =
        modifier.semantics(mergeDescendants = true) {
            contentDescription = description
            this.selected = selected
            onClick(label = actionLabel, action = null)
        }
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border =
        BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        )
    Card(onClick = onClick, modifier = cardModifier, colors = colors, border = border, content = content)
}
