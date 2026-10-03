package com.valebell.glyphsquish.slime

import kotlin.math.roundToInt

/** SDK 2.0 bitmap conversion expands 8-bit gray to 12-bit raw matrix intensity. */
internal object GlyphFrameEncoder {
    private const val ENGINE_MAX = 255
    private const val MATRIX_MAX = 4095

    fun encode(
        frame: IntArray,
        brightnessScale: Float = 1f,
    ): IntArray {
        require(brightnessScale in 0f..1f)
        return IntArray(frame.size) { index ->
            // Match GlyphMatrixUtils.convertToGlyphMatrix at object brightness 255.
            val raw = frame[index].coerceIn(0, ENGINE_MAX) * MATRIX_MAX / ENGINE_MAX
            // Apply idle dimming in the 12-bit domain to preserve low-intensity fade detail.
            (raw * brightnessScale).roundToInt()
        }
    }
}
