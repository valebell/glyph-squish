package com.valebell.glyphsquish.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.runtime.CreatureInputSource
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeContract
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeState
import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlinx.coroutines.flow.StateFlow

class SquishViewModel(
    private val runtime: CreatureRuntimeContract,
) : ViewModel() {
    val uiState: StateFlow<CreatureRuntimeState> = runtime.state
    val persistenceErrors = runtime.persistenceErrors

    fun markFormsSeen(forms: Set<CreatureForm>) = runtime.markFormsSeen(forms)

    fun selectForm(form: CreatureForm): Boolean = runtime.selectForm(form)

    fun selectMove(move: SlimeButtonAction): Boolean = runtime.selectMove(move)

    fun handlePreviewInteraction(interaction: CreatureInteraction): SlimeButtonAction? =
        runtime.handleInteraction(interaction, CreatureInputSource.COMPANION)

    fun cycleFormFromShake(): CreatureForm? = runtime.cycleFormFromShake(CreatureInputSource.COMPANION)

    fun retrySave() = runtime.retrySave()

    fun retryLoad() {
        runtime.retryLoad()
    }

    companion object {
        fun factory(runtime: CreatureRuntimeContract): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SquishViewModel(runtime) }
            }
    }
}
