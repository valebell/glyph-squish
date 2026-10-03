package com.valebell.glyphsquish.slime

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphFrameEncoderTest {
    @Test
    fun idleScaleUsesRawPrecisionAndPreservesOffPixels() {
        val input = intArrayOf(0, 1, 128, 255)
        assertArrayEquals(intArrayOf(0, 4, 514, 1024), GlyphFrameEncoder.encode(input, 0.25f))
        assertArrayEquals(IntArray(4), GlyphFrameEncoder.encode(input, 0f))
        assertArrayEquals(intArrayOf(0, 1, 128, 255), input)
    }

    @Test
    fun expandsGrayToRawIntensityWithoutMutatingEngineFrame() {
        val input = intArrayOf(0, 1, 64, 128, 170, 215, 254, 255)
        val before = input.copyOf()
        assertArrayEquals(intArrayOf(0, 16, 1027, 2055, 2730, 3452, 4078, 4095), GlyphFrameEncoder.encode(input))
        assertArrayEquals(before, input)
    }

    @Test
    fun preservesBlackAndSmoothFadesWithinHardwareRange() {
        val output = GlyphFrameEncoder.encode(IntArray(256) { it })
        assertEquals(0, output.first())
        assertEquals(4095, output.last())
        assertTrue(output.all { it in 0..4095 })
        assertTrue(output.toList().zipWithNext().all { (first, second) -> first < second })
        assertArrayEquals(intArrayOf(0, 4095), GlyphFrameEncoder.encode(intArrayOf(-1, 256)))
    }
}
