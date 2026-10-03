package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureState

data class SlimePhenotype(
    val form: CreatureForm,
    val widthScale: Float = 1f,
    val heightScale: Float = 1f,
) {
    companion object {
        val DEFAULT = SlimePhenotype(form = CreatureForm.BLOB)

        fun from(state: CreatureState): SlimePhenotype = DEFAULT.copy(form = state.currentForm)
    }
}
