package com.valebell.glyphsquish.ui.audio

internal object GlyphTone {
    internal fun hapticCues(effect: GlyphSoundEffect): List<GlyphHapticCue> =
        when (effect) {
            GlyphSoundEffect.SELECT -> {
                listOf(
                    GlyphHapticCue(progress = 0.03, strength = 0.55),
                )
            }

            GlyphSoundEffect.FORM_BLOB,
            GlyphSoundEffect.FORM_SHARK,
            GlyphSoundEffect.FORM_CAT,
            GlyphSoundEffect.FORM_STAR,
            GlyphSoundEffect.FORM_BUTTERFLY,
            -> {
                formProfile(effect).clicks.map { click ->
                    GlyphHapticCue(progress = click.start, strength = click.amplitude)
                }
            }

            GlyphSoundEffect.SLIDE_FORWARD -> {
                listOf(
                    GlyphHapticCue(progress = 0.12, strength = 0.25),
                    GlyphHapticCue(progress = 0.62, strength = 0.45),
                )
            }

            GlyphSoundEffect.SLIDE_BACKWARD -> {
                listOf(
                    GlyphHapticCue(progress = 0.12, strength = 0.45),
                    GlyphHapticCue(progress = 0.62, strength = 0.25),
                )
            }

            GlyphSoundEffect.MOVE_BURST,
            GlyphSoundEffect.MOVE_HELIX,
            GlyphSoundEffect.MOVE_ECHO,
            GlyphSoundEffect.MOVE_RICOCHET,
            GlyphSoundEffect.MOVE_THUNDERBOLT,
            -> {
                moveProfile(effect).pulses.map { pulse ->
                    GlyphHapticCue(progress = pulse.start, strength = pulse.amplitude)
                }
            }
        }
}
