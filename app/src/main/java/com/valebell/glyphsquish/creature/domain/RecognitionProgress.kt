package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction

enum class GestureStep { CIRCLE, BOUNCE, DIAGONAL, GLYPH_BUTTON }

enum class RecognitionReset { INTERRUPTED, TIMED_OUT, WEAK_OR_DEPTH, DIRECTION_OR_JUMP }

/** Transient recognition state, never stored with the character. */
data class RecipeProgress(
    val steps: List<GestureStep>,
    val completedSteps: Int = 0,
)

data class RecognitionProgress(
    val recipes: Map<SlimeButtonAction, RecipeProgress> = emptyMap(),
    val resetReason: RecognitionReset? = null,
)

object MoveRecipes {
    fun steps(move: SlimeButtonAction): List<GestureStep> =
        when (move) {
            SlimeButtonAction.BURST -> listOf(GestureStep.GLYPH_BUTTON)
            SlimeButtonAction.HELIX -> listOf(GestureStep.CIRCLE, GestureStep.CIRCLE)
            SlimeButtonAction.ECHO -> listOf(GestureStep.BOUNCE, GestureStep.BOUNCE)
            SlimeButtonAction.RICOCHET -> listOf(GestureStep.DIAGONAL, GestureStep.DIAGONAL)
            SlimeButtonAction.THUNDERBOLT -> listOf(GestureStep.BOUNCE, GestureStep.GLYPH_BUTTON, GestureStep.CIRCLE)
        }
}
