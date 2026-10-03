package com.valebell.glyphsquish.ui.home

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLayoutInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureForms
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.ui.forms.FORM_CARD_MIN_WIDTH
import com.valebell.glyphsquish.ui.forms.formsGalleryItems
import com.valebell.glyphsquish.ui.moves.MOVE_CARD_MIN_WIDTH
import com.valebell.glyphsquish.ui.moves.movesGalleryItems
import com.valebell.glyphsquish.ui.theme.SquishSpacing
import kotlinx.coroutines.launch

@Composable
internal fun Gallery(
    creature: CreatureState,
    selectedSection: SquishSection,
    actions: GalleryActions,
    layout: GalleryLayout,
    modifier: Modifier = Modifier,
) {
    val newForms =
        CreatureForms
            .availableFor(creature.unlockedMoves)
            .filter { it !in creature.seenForms && it != creature.currentForm }
            .toSet()
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    val layoutDirection = LocalLayoutDirection.current
    if (layout.animateSectionChange) {
        AnimatedContent(
            targetState = selectedSection,
            modifier = modifier,
            transitionSpec =
                if (animationsEnabled) {
                    sectionTransitionSpec(layoutDirection)
                } else {
                    ReducedMotionSectionTransitionSpec
                },
            contentAlignment = Alignment.TopCenter,
            label = "forms and moves gallery",
        ) { section ->
            GalleryGrid(
                creature = creature,
                section = section,
                newForms = newForms,
                active = selectedSection == SquishSection.FORMS,
                modifier = Modifier.fillMaxSize(),
                actions = actions,
                layout = layout,
            )
        }
    } else {
        GalleryGrid(
            creature = creature,
            section = selectedSection,
            newForms = newForms,
            active = selectedSection == SquishSection.FORMS,
            modifier = modifier,
            actions = actions,
            layout = layout,
        )
    }
}

@Composable
private fun GalleryGrid(
    creature: CreatureState,
    section: SquishSection,
    newForms: Set<CreatureForm>,
    active: Boolean,
    actions: GalleryActions,
    layout: GalleryLayout,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val gridState = rememberLazyStaggeredGridState()
    val returnToPreview = rememberPreviewScroll(gridState, layout.scrollToPreviewOnSelection)
    val visibleForms =
        rememberVisibleForms(gridState, active && section == SquishSection.FORMS, creature, actions.onFormsVisible)
    LazyVerticalStaggeredGrid(
        state = gridState,
        columns =
            StaggeredGridCells.Adaptive(
                when (section) {
                    SquishSection.FORMS -> FORM_CARD_MIN_WIDTH * fontScale
                    SquishSection.MOVES -> MOVE_CARD_MIN_WIDTH * fontScale
                },
            ),
        modifier = modifier,
        contentPadding = layout.contentPadding,
        horizontalArrangement = Arrangement.spacedBy(SquishSpacing.Item),
        verticalItemSpacing = SquishSpacing.Item,
    ) {
        layout.leadingContent?.let { content ->
            item(
                key = "preview",
                span = StaggeredGridItemSpan.FullLine,
            ) {
                content()
            }
        }
        when (section) {
            SquishSection.FORMS -> {
                formsGalleryItems(
                    creature = creature,
                    onSelectForm = { form ->
                        actions.onSelectForm(form)
                        if (form != creature.currentForm &&
                            form in CreatureForms.availableFor(creature.unlockedMoves)
                        ) {
                            returnToPreview()
                        }
                    },
                    newForms = newForms,
                    visibleForms = visibleForms,
                )
            }

            SquishSection.MOVES -> {
                movesGalleryItems(
                    creature = creature,
                    onSelectMove = { move ->
                        actions.onSelectMove(move)
                        if (move != creature.selectedMove && move in creature.unlockedMoves) {
                            returnToPreview()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun rememberVisibleForms(
    gridState: LazyStaggeredGridState,
    active: Boolean,
    creature: CreatureState,
    onFormsVisible: (Set<CreatureForm>) -> Unit,
): Set<CreatureForm> {
    val currentCreature by rememberUpdatedState(creature)
    val visibleCallback by rememberUpdatedState(onFormsVisible)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visibleForms by remember { mutableStateOf(emptySet<CreatureForm>()) }
    LaunchedEffect(gridState, active, lifecycle) {
        visibleForms = emptySet()
        if (active) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    snapshotFlow {
                        visibleGalleryForms(gridState.layoutInfo, currentCreature)
                    }.collect {
                        visibleForms = it
                        // Existing/current selections must never regain discovery emphasis.
                        if (currentCreature.currentForm in it &&
                            currentCreature.currentForm !in currentCreature.seenForms
                        ) {
                            visibleCallback(setOf(currentCreature.currentForm))
                        }
                    }
                } finally {
                    visibleForms = emptySet()
                }
            }
        }
    }
    return visibleForms
}

private fun visibleGalleryForms(
    layout: LazyStaggeredGridLayoutInfo,
    creature: CreatureState,
): Set<CreatureForm> {
    val visibleKeys =
        layout.visibleItemsInfo
            .filter {
                it.offset.y + it.size.height > layout.beforeContentPadding &&
                    it.offset.y < layout.viewportEndOffset - layout.afterContentPadding
            }.map { it.key }
            .toSet()
    return CreatureForms
        .availableFor(creature.unlockedMoves)
        .filter { "form-$it" in visibleKeys }
        .toSet()
}

@Composable
private fun rememberPreviewScroll(
    gridState: LazyStaggeredGridState,
    enabled: Boolean,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    return {
        if (enabled) {
            scope.launch {
                if (animationsEnabled) gridState.animateScrollToItem(0) else gridState.scrollToItem(0)
            }
        }
    }
}
