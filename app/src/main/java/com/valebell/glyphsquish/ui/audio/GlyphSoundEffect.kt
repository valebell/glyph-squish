package com.valebell.glyphsquish.ui.audio

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.slime.SlimeEngine

enum class GlyphSoundEffect(
    internal val durationMillis: Int,
    internal val form: CreatureForm? = null,
) {
    SELECT(82),
    FORM_BLOB(SlimeEngine.FORM_TRANSITION_MILLIS, CreatureForm.BLOB),
    FORM_SHARK(SlimeEngine.FORM_TRANSITION_MILLIS, CreatureForm.SHARK),
    FORM_CAT(SlimeEngine.FORM_TRANSITION_MILLIS, CreatureForm.CAT),
    FORM_STAR(SlimeEngine.FORM_TRANSITION_MILLIS, CreatureForm.STAR),
    FORM_BUTTERFLY(SlimeEngine.FORM_TRANSITION_MILLIS, CreatureForm.BUTTERFLY),
    SLIDE_FORWARD(145),
    SLIDE_BACKWARD(145),
    MOVE_BURST(SlimeEngine.BURST_ACTION_MILLIS),
    MOVE_HELIX(SlimeEngine.HELIX_ACTION_MILLIS),
    MOVE_ECHO(SlimeEngine.ECHO_ACTION_MILLIS),
    MOVE_RICOCHET(SlimeEngine.RICOCHET_ACTION_MILLIS),
    MOVE_THUNDERBOLT(SlimeEngine.THUNDERBOLT_ACTION_MILLIS),
    ;

    internal val isFormChange: Boolean
        get() = form != null

    companion object {
        fun forForm(form: CreatureForm): GlyphSoundEffect =
            when (form) {
                CreatureForm.BLOB -> FORM_BLOB
                CreatureForm.SHARK -> FORM_SHARK
                CreatureForm.CAT -> FORM_CAT
                CreatureForm.STAR -> FORM_STAR
                CreatureForm.BUTTERFLY -> FORM_BUTTERFLY
            }

        fun forMove(move: SlimeButtonAction): GlyphSoundEffect =
            when (move) {
                SlimeButtonAction.BURST -> MOVE_BURST
                SlimeButtonAction.HELIX -> MOVE_HELIX
                SlimeButtonAction.ECHO -> MOVE_ECHO
                SlimeButtonAction.RICOCHET -> MOVE_RICOCHET
                SlimeButtonAction.THUNDERBOLT -> MOVE_THUNDERBOLT
            }
    }
}
