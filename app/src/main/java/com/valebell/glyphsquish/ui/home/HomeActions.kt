package com.valebell.glyphsquish.ui.home

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.slime.SlimeButtonAction

data class GalleryActions(
    val onSelectForm: (CreatureForm) -> Unit,
    val onFormsVisible: (Set<CreatureForm>) -> Unit,
    val onSelectMove: (SlimeButtonAction) -> Unit,
)

data class HomeActions(
    val gallery: GalleryActions,
    val onSelectSection: (SquishSection) -> Unit,
    val onReplayMove: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onRetry: () -> Unit,
)
