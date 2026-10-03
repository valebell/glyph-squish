package com.valebell.glyphsquish.slime

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class GlyphIdleBrightnessTest {
    private val brightness = GlyphIdleBrightness().apply { reset(0) }

    @Test
    fun idleGracePeriodThenSmoothFadeKeepsCreatureVisible() {
        assertLevel(0, 0.85f)
        assertLevel(1_999, 0.85f)
        assertLevel(2_000, 0.85f)
        assertLevel(2_750, 0.55f)
        assertLevel(3_500, 0.25f)
        assertLevel(60_000, 0.25f)
    }

    @Test
    fun movementWakesQuicklyWithoutRestartingTheRampOnEverySample() {
        assertLevel(8_000, 0.25f)
        for (time in 8_000L..8_120L step 20) motion(time, accelerationX = 0.12f)
        assertLevel(8_120, 0.25f)
        motion(8_180, accelerationX = 0.12f)
        assertLevel(8_195, 0.55f)
        motion(8_240, accelerationX = 0.12f)
        assertLevel(8_270, 0.85f)
        assertLevel(10_240, 0.85f)
        assertLevel(11_740, 0.25f)
    }

    @Test
    fun stationaryTiltAndSensorNoiseDoNotPreventDimming() {
        motion(0, tiltX = 0.6f, tiltY = 0.8f, tiltZ = 0f)
        for (time in 1L..8_000L step 20) {
            val noise = if (time % 40 < 20) 0.003f else -0.003f
            motion(time, tiltX = 0.6f + noise, tiltY = 0.8f, tiltZ = 0f, accelerationX = noise)
        }
        assertLevel(8_000, 0.25f)
    }

    @Test
    fun smallPostureChangesNeitherPostponeDimmingNorWakeIt() {
        for (amplitude in listOf(2.0, 5.0)) {
            brightness.reset(0)
            for (time in 0L..18_000L step 20) {
                tilt(time, amplitude * sin(time / 300.0))
                if (time >= 3_500) assertLevel(time, 0.25f)
            }
        }
    }

    @Test
    fun slowDriftDoesNotAccumulateAcrossTheObservationWindow() {
        for (time in 0L..60_000L step 20) {
            tilt(time, time / 1_000.0)
            if (time >= 3_500) assertLevel(time, 0.25f)
        }
    }

    @Test
    fun dimmingRecentersTiltHistoryBeforeAcceptingMoreMotion() {
        for (renderBeforeMotion in listOf(false, true)) {
            brightness.reset(0)
            tilt(1_500, 0.0)
            tilt(1_980, 9.0)
            if (renderBeforeMotion) brightness.level(2_000)
            // Eleven degrees from old history, but only two degrees after settling.
            tilt(2_010, 11.0)
            assertLevel(3_500, 0.25f)
        }
    }

    @Test
    fun deliberateTiltWakesAtDifferentSampleRates() {
        for (interval in listOf(10L, 20L, 50L, 100L)) {
            brightness.reset(0)
            assertLevel(8_000, 0.25f)
            for (elapsed in 0L..600L step interval) {
                tilt(8_000 + elapsed, elapsed / 50.0)
            }
            assertLevel(8_750, 0.85f)
        }
    }

    @Test
    fun deliberateTiltSpanningAWindowBoundaryStillWakes() {
        assertLevel(8_000, 0.25f)
        for (time in 8_000L..8_900L step 20) tilt(time, 0.0)
        for (elapsed in 20L..400L step 20) tilt(8_900 + elapsed, elapsed * 0.03)
        assertLevel(9_450, 0.85f)
    }

    @Test
    fun expiredTiltHistoryDoesNotWakeAfterASensorGap() {
        assertLevel(8_000, 0.25f)
        tilt(8_000, 0.0)
        tilt(9_001, 15.0)
        assertLevel(9_151, 0.25f)
    }

    @Test
    fun tiltUsesTheWholeWindowIncludingRecentDirectionChanges() {
        assertLevel(8_000, 0.25f)
        tilt(8_000, 0.0)
        tilt(8_500, 9.0)
        tilt(9_000, -2.0)
        assertLevel(9_150, 0.85f)
    }

    @Test
    fun isolatedAccelerationSpikesDoNotWake() {
        assertLevel(8_000, 0.25f)
        for (time in 8_000L..18_000L step 20) {
            motion(time, accelerationX = if (time % 200 == 0L) 0.5f else 0f)
            assertLevel(time, 0.25f)
        }
    }

    @Test
    fun subthresholdAccelerationDoesNotPostponeDimming() {
        for (time in 0L..8_000L step 20) {
            motion(time, accelerationX = 0.09f)
            if (time >= 3_500) assertLevel(time, 0.25f)
        }
    }

    @Test
    fun sustainedAccelerationRequiresElapsedTimeAtDifferentSampleRates() {
        for (interval in listOf(10L, 20L, 50L, 100L)) {
            brightness.reset(0)
            assertLevel(8_000, 0.25f)
            val qualificationTime = ((120L + interval - 1) / interval) * interval
            for (elapsed in 0L..qualificationTime step interval) {
                motion(8_000 + elapsed, accelerationX = 0.12f)
                assertLevel(8_000 + elapsed, 0.25f)
            }
            assertLevel(8_150 + qualificationTime, 0.85f)
        }
    }

    @Test
    fun accelerationBelowThresholdBreaksTheRequiredHold() {
        assertLevel(8_000, 0.25f)
        motion(8_000, accelerationX = 0.12f)
        motion(8_060, accelerationX = 0.12f)
        motion(8_100, accelerationX = 0.05f)
        motion(8_120, accelerationX = 0.12f)
        motion(8_180, accelerationX = 0.12f)
        assertLevel(8_240, 0.25f)
        motion(8_240, accelerationX = 0.12f)
        assertLevel(8_390, 0.85f)
    }

    @Test
    fun accelerationEvidenceDoesNotSpanASensorGap() {
        assertLevel(8_000, 0.25f)
        motion(8_000, accelerationX = 0.12f)
        motion(8_200, accelerationX = 0.12f)
        motion(8_260, accelerationX = 0.12f)
        assertLevel(8_320, 0.25f)
        motion(8_320, accelerationX = 0.12f)
        assertLevel(8_470, 0.85f)
    }

    @Test
    fun repeatedCallbacksAtTheSameTimeCannotCompleteAccelerationHold() {
        assertLevel(8_000, 0.25f)
        repeat(100) { motion(8_000, accelerationX = 0.12f) }
        motion(8_020)
        assertLevel(8_150, 0.25f)
    }

    @Test
    fun allThreeAccelerationAxesCanWakeTheGlyph() {
        for (axis in 0..2) {
            brightness.reset(0)
            assertLevel(8_000, 0.25f)
            val acceleration = FloatArray(3).apply { this[axis] = 0.12f }
            for (time in 8_000L..8_120L step 60) {
                brightness.onMotion(time, 0f, 0f, 1f, acceleration[0], acceleration[1], acceleration[2])
            }
            assertLevel(8_270, 0.85f)
        }
    }

    @Test
    fun buttonWakeInterruptsDimmingWithoutABrightnessJump() {
        val before = brightness.level(2_750)
        brightness.wake(2_750)
        assertEquals(before, brightness.level(2_750), 0.0001f)
        assertLevel(2_900, 0.85f)
    }

    @Test
    fun heldButtonOrPerformanceStaysBrightAndRestartsGracePeriod() {
        assertLevel(8_000, 0.25f)
        for (time in 8_000L..18_000L step 50) {
            brightness.level(time, interactionActive = true)
        }
        assertLevel(18_000, 0.85f)
        assertLevel(20_000, 0.85f)
        assertLevel(21_500, 0.25f)
    }

    @Test
    fun freshSessionStartsBrightAndForgetsOldMotion() {
        motion(0, tiltX = 0.6f)
        assertLevel(8_000, 0.25f)
        brightness.reset(20_000)
        motion(20_000, tiltX = -0.6f)
        assertLevel(20_000, 0.85f)
        assertLevel(22_000, 0.85f)
        assertLevel(23_500, 0.25f)
    }

    @Test
    fun missedFramesStillProduceTheCorrectEnvelope() {
        assertLevel(30_000, 0.25f)
        brightness.wake(30_000)
        assertLevel(30_200, 0.85f)
        assertLevel(32_750, 0.55f)
        assertLevel(33_500, 0.25f)
    }

    private fun motion(
        now: Long,
        tiltX: Float = 0f,
        tiltY: Float = 0f,
        tiltZ: Float = 1f,
        accelerationX: Float = 0f,
    ) = brightness.onMotion(now, tiltX, tiltY, tiltZ, accelerationX, 0f, 0f)

    private fun tilt(
        now: Long,
        degrees: Double,
    ) {
        val angle = Math.toRadians(degrees)
        motion(now, tiltX = sin(angle).toFloat(), tiltZ = cos(angle).toFloat())
    }

    private fun assertLevel(
        now: Long,
        expected: Float,
    ) = assertEquals(expected, brightness.level(now), 0.0001f)
}
