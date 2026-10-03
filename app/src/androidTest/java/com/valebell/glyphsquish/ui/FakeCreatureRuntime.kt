package com.valebell.glyphsquish.ui

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureForms
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.runtime.CreatureInputSource
import com.valebell.glyphsquish.creature.runtime.CreaturePersistenceError
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeContract
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeState
import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class FakeCreatureRuntime : CreatureRuntimeContract {
    private val mutableState =
        MutableStateFlow<CreatureRuntimeState>(
            CreatureRuntimeState.Ready(CreatureProgression.initialState()),
        )
    override val state: StateFlow<CreatureRuntimeState> = mutableState

    val mutablePersistenceErrors = MutableSharedFlow<CreaturePersistenceError>(extraBufferCapacity = 1)
    override val persistenceErrors: SharedFlow<CreaturePersistenceError> = mutablePersistenceErrors

    var saveRetryCount = 0

    override fun retrySave() {
        saveRetryCount++
    }

    override fun retryLoad() = Unit

    override fun handleInteraction(
        interaction: CreatureInteraction,
        source: CreatureInputSource,
    ): SlimeButtonAction? = null

    override fun handleGlyphButtonPress(): SlimeButtonAction = SlimeButtonAction.BURST

    override fun setGlyphMovePlaybackActive(active: Boolean) = Unit

    override fun selectMove(move: SlimeButtonAction): Boolean {
        val current = (mutableState.value as CreatureRuntimeState.Ready).creature
        if (move !in current.unlockedMoves) return false
        mutableState.value = CreatureRuntimeState.Ready(CreatureProgression.selectMove(current, move))
        return true
    }

    override fun markFormsSeen(forms: Set<CreatureForm>) {
        mutableState.value = CreatureRuntimeState.Ready(CreatureProgression.markFormsSeen(creature, forms))
    }

    val creature get() = (mutableState.value as CreatureRuntimeState.Ready).creature

    fun unlock(move: SlimeButtonAction) {
        mutableState.value = CreatureRuntimeState.Ready(CreatureProgression.unlockMove(creature, move))
    }

    fun restoreUnseenSelection(form: CreatureForm) {
        mutableState.value = CreatureRuntimeState.Ready(creature.copy(currentForm = form))
    }

    override fun selectForm(form: CreatureForm): Boolean {
        val current = (mutableState.value as CreatureRuntimeState.Ready).creature
        if (form !in CreatureForms.availableFor(current.unlockedMoves)) return false
        mutableState.value =
            CreatureRuntimeState.Ready(CreatureProgression.selectForm(current, form))
        return true
    }

    override fun cycleFormFromShake(source: CreatureInputSource): CreatureForm? = null

    override fun resetMoveRecognition() = Unit
}
