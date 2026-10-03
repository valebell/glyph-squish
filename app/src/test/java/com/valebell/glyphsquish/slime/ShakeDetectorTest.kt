package com.valebell.glyphsquish.slime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {
    @Test
    fun alternatingStrongImpulsesProduceOneShake() {
        val detector = ShakeDetector()

        assertFalse(detector.addSample(8f, 0f, 0f, millis(0)))
        assertFalse(detector.addSample(0f, 0f, 0f, millis(40)))
        assertFalse(detector.addSample(-8f, 0f, 0f, millis(100)))
        assertFalse(detector.addSample(0f, 0f, 0f, millis(140)))
        assertTrue(detector.addSample(8f, 0f, 0f, millis(200)))
        assertFalse(detector.addSample(-8f, 0f, 0f, millis(300)))
        assertFalse(detector.addSample(8f, 0f, 0f, millis(400)))
    }

    @Test
    fun ordinaryMovementDoesNotProduceShake() {
        val detector = ShakeDetector()

        repeat(20) { index ->
            assertFalse(detector.addSample(3f, -2f, 1f, millis(index * 40L)))
        }
        assertFalse(detector.addSample(8f, 0f, 0f, millis(900)))
        assertFalse(detector.addSample(7f, 1f, 0f, millis(1_000)))
        assertFalse(detector.addSample(8f, 0f, 0f, millis(1_100)))
    }

    @Test
    fun cooldownSuppressesAnotherGestureUntilQuietAndElapsed() {
        val detector = ShakeDetector()

        assertTrue(feedShake(detector, startMillis = 0))
        assertFalse(feedShake(detector, startMillis = 400))
        assertFalse(detector.addSample(0f, 0f, 0f, millis(800)))
        assertFalse(detector.addSample(0f, 0f, 0f, millis(1_250)))
        assertTrue(feedShake(detector, startMillis = 1_300))
    }

    @Test
    fun resetClearsCooldownAndGestureState() {
        val detector = ShakeDetector()

        assertTrue(feedShake(detector, startMillis = 0))
        detector.reset()
        assertTrue(feedShake(detector, startMillis = 300))
    }

    private fun feedShake(
        detector: ShakeDetector,
        startMillis: Long,
    ): Boolean {
        var emitted = detector.addSample(8f, 0f, 0f, millis(startMillis))
        emitted = detector.addSample(0f, 0f, 0f, millis(startMillis + 40)) || emitted
        emitted = detector.addSample(-8f, 0f, 0f, millis(startMillis + 100)) || emitted
        emitted = detector.addSample(0f, 0f, 0f, millis(startMillis + 140)) || emitted
        return detector.addSample(8f, 0f, 0f, millis(startMillis + 200)) || emitted
    }

    private fun millis(value: Long): Long = value * 1_000_000L
}
