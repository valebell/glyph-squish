package com.valebell.glyphsquish.ui.audio

import com.valebell.glyphsquish.R

/** Form indices preserve the original -1, 0 and +1 synthesis variations. */
internal fun GlyphSoundEffect.soundResources(): List<Int> =
    when (this) {
        GlyphSoundEffect.SELECT -> {
            listOf(R.raw.glyph_select)
        }

        GlyphSoundEffect.FORM_BLOB -> {
            listOf(R.raw.glyph_form_blob_0, R.raw.glyph_form_blob_1, R.raw.glyph_form_blob_2)
        }

        GlyphSoundEffect.FORM_SHARK -> {
            listOf(R.raw.glyph_form_shark_0, R.raw.glyph_form_shark_1, R.raw.glyph_form_shark_2)
        }

        GlyphSoundEffect.FORM_CAT -> {
            listOf(R.raw.glyph_form_cat_0, R.raw.glyph_form_cat_1, R.raw.glyph_form_cat_2)
        }

        GlyphSoundEffect.FORM_STAR -> {
            listOf(R.raw.glyph_form_star_0, R.raw.glyph_form_star_1, R.raw.glyph_form_star_2)
        }

        GlyphSoundEffect.FORM_BUTTERFLY -> {
            listOf(R.raw.glyph_form_butterfly_0, R.raw.glyph_form_butterfly_1, R.raw.glyph_form_butterfly_2)
        }

        GlyphSoundEffect.SLIDE_FORWARD -> {
            listOf(R.raw.glyph_slide_forward)
        }

        GlyphSoundEffect.SLIDE_BACKWARD -> {
            listOf(R.raw.glyph_slide_backward)
        }

        GlyphSoundEffect.MOVE_BURST -> {
            listOf(R.raw.glyph_move_burst)
        }

        GlyphSoundEffect.MOVE_HELIX -> {
            listOf(R.raw.glyph_move_helix)
        }

        GlyphSoundEffect.MOVE_ECHO -> {
            listOf(R.raw.glyph_move_echo)
        }

        GlyphSoundEffect.MOVE_RICOCHET -> {
            listOf(R.raw.glyph_move_ricochet)
        }

        GlyphSoundEffect.MOVE_THUNDERBOLT -> {
            listOf(R.raw.glyph_move_thunderbolt)
        }
    }
