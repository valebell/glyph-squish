package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction

object CreatureProgression {
    fun initialState(): CreatureState =
        CreatureState(
            currentForm = CreatureForm.BLOB,
            selectedMove = SlimeButtonAction.BURST,
            unlockedMoves = setOf(SlimeButtonAction.BURST),
            revision = 1L,
        )

    fun selectMove(
        state: CreatureState,
        move: SlimeButtonAction,
    ): CreatureState {
        require(move in state.unlockedMoves) { "$move is still locked" }
        return if (move == state.selectedMove) state else state.copy(selectedMove = move, revision = state.revision + 1)
    }

    fun selectForm(
        state: CreatureState,
        form: CreatureForm,
    ): CreatureState {
        require(form in CreatureForms.availableFor(state.unlockedMoves)) {
            "$form is still locked"
        }
        return if (form == state.currentForm && form in state.seenForms) {
            state
        } else {
            state.copy(currentForm = form, seenForms = state.seenForms + form, revision = state.revision + 1)
        }
    }

    fun markFormsSeen(
        state: CreatureState,
        forms: Set<CreatureForm>,
    ): CreatureState {
        val seen = state.seenForms + forms.intersect(CreatureForms.availableFor(state.unlockedMoves))
        return if (seen == state.seenForms) state else state.copy(seenForms = seen, revision = state.revision + 1)
    }

    fun unlockMove(
        state: CreatureState,
        move: SlimeButtonAction,
    ): CreatureState {
        if (move in state.unlockedMoves) return state
        return state.copy(
            selectedMove = move,
            unlockedMoves = state.unlockedMoves + move,
            revision = state.revision + 1,
        )
    }
}
