package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction

enum class ShellSector {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    FRONT,
    BACK,
}

data class CreatureState(
    val currentForm: CreatureForm,
    val selectedMove: SlimeButtonAction,
    val unlockedMoves: Set<SlimeButtonAction>,
    val revision: Long,
    val seenForms: Set<CreatureForm> = CreatureForms.availableFor(unlockedMoves),
) {
    init {
        require(SlimeButtonAction.BURST in unlockedMoves) {
            "Burst must always be unlocked"
        }
        require(selectedMove in unlockedMoves) {
            "The selected move must be unlocked"
        }
        require(currentForm in CreatureForms.availableFor(unlockedMoves)) {
            "The current form must be unlocked"
        }
    }
}

sealed interface CreatureInteraction {
    /** Ordinary-motion position divided by the shell radius; retains distance from the center. */
    data class CirclePosition(
        val x: Float,
        val y: Float,
        val z: Float,
    ) : CreatureInteraction

    data class ShellContact(
        val sector: ShellSector,
        val strength: Float,
        val normalX: Float,
        val normalY: Float,
        val normalZ: Float,
        // Physics knows the boundary position even when sliding produces almost no impact.
        val hasReliablePosition: Boolean = false,
    ) : CreatureInteraction

    data object GlyphButtonPressed : CreatureInteraction
}

data class InteractionOutcome(
    val state: CreatureState,
    val unlockedMove: SlimeButtonAction?,
    /** Recognition includes learned recipes; only unlockedMove requests automatic playback. */
    val recognizedMove: SlimeButtonAction? = unlockedMove,
)
