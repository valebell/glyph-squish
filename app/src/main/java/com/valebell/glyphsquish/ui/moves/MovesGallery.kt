package com.valebell.glyphsquish.ui.moves

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.descriptionResource
import com.valebell.glyphsquish.nameResource
import com.valebell.glyphsquish.recipeResource
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.ui.components.Badge
import com.valebell.glyphsquish.ui.components.GalleryCard
import com.valebell.glyphsquish.ui.components.GalleryHeader
import com.valebell.glyphsquish.ui.components.LockFeedbackIcon
import com.valebell.glyphsquish.ui.theme.SquishSpacing

internal fun LazyStaggeredGridScope.movesGalleryItems(
    creature: CreatureState,
    onSelectMove: (SlimeButtonAction) -> Unit,
) {
    item(
        key = "moves-header",
        span = StaggeredGridItemSpan.FullLine,
    ) {
        GalleryHeader(
            title = stringResource(R.string.move_gallery_title),
            progress =
                pluralStringResource(
                    R.plurals.move_gallery_unlocked_format,
                    creature.unlockedMoves.size,
                    creature.unlockedMoves.size,
                    SlimeButtonAction.entries.size,
                ),
            modifier = Modifier.animateItem(),
        )
    }
    item(
        key = "moves-hint",
        span = StaggeredGridItemSpan.FullLine,
    ) {
        Text(
            text = stringResource(R.string.move_gallery_unlock_hint),
            modifier = Modifier.animateItem(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    items(
        items = SlimeButtonAction.entries,
        key = { "move-$it" },
    ) { move ->
        val unlocked = move in creature.unlockedMoves
        MoveCard(
            move = move,
            unlocked = unlocked,
            selected = move == creature.selectedMove,
            onClick = { onSelectMove(move) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateItem(),
        )
    }
}

@Composable
private fun MoveCard(
    move: SlimeButtonAction,
    unlocked: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lockFeedbackTrigger by remember { mutableIntStateOf(0) }
    val recipe = move.recipeResource()?.let { stringResource(it) }
    val description =
        stringResource(
            when {
                !unlocked -> R.string.move_gallery_card_locked_description
                recipe != null -> R.string.move_gallery_card_recipe_description
                else -> R.string.move_gallery_card_unlocked_description
            },
            stringResource(move.nameResource()),
            stringResource(move.descriptionResource()),
            recipe.orEmpty(),
        )

    GalleryCard(
        onClick = {
            if (!unlocked) {
                lockFeedbackTrigger += 1
            }
            onClick()
        },
        description = description,
        selected = selected,
        actionLabel = if (unlocked) stringResource(R.string.select_move_action) else null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(SquishSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(SquishSpacing.Compact),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SquishSpacing.Compact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!unlocked) {
                    LockFeedbackIcon(feedbackTrigger = lockFeedbackTrigger)
                }
                Text(
                    text = stringResource(move.nameResource()),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(move.descriptionResource()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (recipe != null) {
                Text(
                    text = recipe,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MOVE_BADGE_SLOT_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(SquishSpacing.Tiny),
                verticalAlignment = Alignment.Bottom,
            ) {
                MoveCardBadges(selected, unlocked)
            }
        }
    }
}

@Composable
private fun MoveCardBadges(
    selected: Boolean,
    unlocked: Boolean,
) {
    if (selected) {
        Badge(
            text = stringResource(R.string.move_gallery_selected_badge),
            emphasized = true,
        )
    }
    if (!unlocked) {
        Badge(text = stringResource(R.string.move_gallery_locked_badge))
    }
}

internal val MOVE_CARD_MIN_WIDTH = 220.dp
private val MOVE_BADGE_SLOT_HEIGHT = 28.dp
