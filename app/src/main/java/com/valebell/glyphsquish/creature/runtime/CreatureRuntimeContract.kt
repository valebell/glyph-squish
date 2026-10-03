package com.valebell.glyphsquish.creature.runtime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface CreatureRuntimeState {
    data object Loading : CreatureRuntimeState

    data class Ready(
        val creature: CreatureState,
    ) : CreatureRuntimeState

    data class LoadFailed(
        val message: String,
    ) : CreatureRuntimeState
}

data class CreaturePersistenceError(
    val message: String,
)

/** Companion motion animates the preview; only Glyph input can learn moves. */
enum class CreatureInputSource { GLYPH, COMPANION }

interface CreatureRuntimeContract {
    val state: StateFlow<CreatureRuntimeState>
    val persistenceErrors: SharedFlow<CreaturePersistenceError>

    fun retrySave()

    fun retryLoad()

    /** Returns a move only for its first Glyph unlock, independently of gallery acknowledgement. */
    fun handleInteraction(
        interaction: CreatureInteraction,
        source: CreatureInputSource,
    ): SlimeButtonAction?

    /** Plays the selected move, including as the middle step of a Thunderbolt recipe. */
    fun handleGlyphButtonPress(): SlimeButtonAction

    /** Pauses gesture input and its timeout throughout queued and active Glyph playback. */
    fun setGlyphMovePlaybackActive(active: Boolean)

    fun selectMove(move: SlimeButtonAction): Boolean

    fun markFormsSeen(forms: Set<CreatureForm>)

    fun selectForm(form: CreatureForm): Boolean

    fun cycleFormFromShake(source: CreatureInputSource): CreatureForm?

    fun resetMoveRecognition()
}
