package com.valebell.glyphsquish.slime

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test

class SpatialMotionSensorTest {
    @Test
    fun portraitKeepsSensorAxes() {
        assertAxes(
            expected = MotionAxes(2f, 5f, 7f),
            actual = remapMotionAxes(2f, 5f, 7f, Surface.ROTATION_0),
        )
    }

    @Test
    fun clockwiseLandscapeMapsPhysicalRightToScreenDown() {
        assertAxes(
            expected = MotionAxes(-5f, 2f, 7f),
            actual = remapMotionAxes(2f, 5f, 7f, Surface.ROTATION_90),
        )
    }

    @Test
    fun reversePortraitInvertsPlanarAxes() {
        assertAxes(
            expected = MotionAxes(-2f, -5f, 7f),
            actual = remapMotionAxes(2f, 5f, 7f, Surface.ROTATION_180),
        )
    }

    @Test
    fun counterClockwiseLandscapeMapsPhysicalLeftToScreenDown() {
        assertAxes(
            expected = MotionAxes(5f, -2f, 7f),
            actual = remapMotionAxes(2f, 5f, 7f, Surface.ROTATION_270),
        )
    }

    private fun assertAxes(
        expected: MotionAxes,
        actual: MotionAxes,
    ) {
        assertEquals(expected.x, actual.x, 0f)
        assertEquals(expected.y, actual.y, 0f)
        assertEquals(expected.z, actual.z, 0f)
    }
}
