package com.valebell.glyphsquish.slime

import kotlin.math.sqrt

class ShakeDetector {
    private var ready = true
    private var impulseCount = 0
    private var firstImpulseTimestampNanos = UNSET_TIMESTAMP
    private var lastImpulseTimestampNanos = UNSET_TIMESTAMP
    private var lastSampleTimestampNanos = UNSET_TIMESTAMP
    private var quietStartedTimestampNanos = UNSET_TIMESTAMP
    private var cooldownEndsTimestampNanos = UNSET_TIMESTAMP
    private var lastImpulseX = 0f
    private var lastImpulseY = 0f
    private var lastImpulseZ = 0f
    private var lastImpulseMagnitudeSquared = 0f

    fun addSample(
        x: Float,
        y: Float,
        z: Float,
        timestampNanos: Long,
    ): Boolean {
        if (lastSampleTimestampNanos != UNSET_TIMESTAMP && timestampNanos < lastSampleTimestampNanos) {
            reset()
        }
        lastSampleTimestampNanos = timestampNanos

        val magnitudeSquared = x * x + y * y + z * z
        if (!ready) {
            updateRearming(magnitudeSquared, timestampNanos)
            if (!ready) return false
        }

        if (impulseCount > 0 &&
            timestampNanos - firstImpulseTimestampNanos > MAX_GESTURE_DURATION_NANOS
        ) {
            clearImpulseSequence()
        }
        if (magnitudeSquared < STRONG_IMPULSE_METERS_PER_SECOND_SQUARED_SQUARED) return false

        if (impulseCount == 0) {
            beginImpulseSequence(x, y, z, magnitudeSquared, timestampNanos)
            return false
        }

        val sinceLastImpulse = timestampNanos - lastImpulseTimestampNanos
        if (sinceLastImpulse < MIN_IMPULSE_INTERVAL_NANOS) return false
        if (sinceLastImpulse > MAX_IMPULSE_INTERVAL_NANOS) {
            beginImpulseSequence(x, y, z, magnitudeSquared, timestampNanos)
            return false
        }

        val directionSimilarity =
            (x * lastImpulseX + y * lastImpulseY + z * lastImpulseZ) /
                sqrt(magnitudeSquared * lastImpulseMagnitudeSquared)
        if (directionSimilarity > MAX_REVERSAL_DIRECTION_SIMILARITY) {
            if (directionSimilarity < MIN_SAME_DIRECTION_SIMILARITY) {
                beginImpulseSequence(x, y, z, magnitudeSquared, timestampNanos)
            }
            return false
        }

        impulseCount += 1
        recordImpulse(x, y, z, magnitudeSquared, timestampNanos)
        if (impulseCount < REQUIRED_ALTERNATING_IMPULSES) return false

        ready = false
        cooldownEndsTimestampNanos = timestampNanos + COOLDOWN_NANOS
        quietStartedTimestampNanos = UNSET_TIMESTAMP
        clearImpulseSequence()
        return true
    }

    fun reset() {
        ready = true
        quietStartedTimestampNanos = UNSET_TIMESTAMP
        cooldownEndsTimestampNanos = UNSET_TIMESTAMP
        lastSampleTimestampNanos = UNSET_TIMESTAMP
        clearImpulseSequence()
    }

    private fun updateRearming(
        magnitudeSquared: Float,
        timestampNanos: Long,
    ) {
        if (magnitudeSquared > QUIET_METERS_PER_SECOND_SQUARED_SQUARED) {
            quietStartedTimestampNanos = UNSET_TIMESTAMP
            return
        }
        if (quietStartedTimestampNanos == UNSET_TIMESTAMP) {
            quietStartedTimestampNanos = timestampNanos
            return
        }
        if (timestampNanos >= cooldownEndsTimestampNanos &&
            timestampNanos - quietStartedTimestampNanos >= REARM_QUIET_PERIOD_NANOS
        ) {
            ready = true
            quietStartedTimestampNanos = UNSET_TIMESTAMP
        }
    }

    private fun beginImpulseSequence(
        x: Float,
        y: Float,
        z: Float,
        magnitudeSquared: Float,
        timestampNanos: Long,
    ) {
        impulseCount = 1
        firstImpulseTimestampNanos = timestampNanos
        recordImpulse(x, y, z, magnitudeSquared, timestampNanos)
    }

    private fun recordImpulse(
        x: Float,
        y: Float,
        z: Float,
        magnitudeSquared: Float,
        timestampNanos: Long,
    ) {
        lastImpulseX = x
        lastImpulseY = y
        lastImpulseZ = z
        lastImpulseMagnitudeSquared = magnitudeSquared
        lastImpulseTimestampNanos = timestampNanos
    }

    private fun clearImpulseSequence() {
        impulseCount = 0
        firstImpulseTimestampNanos = UNSET_TIMESTAMP
        lastImpulseTimestampNanos = UNSET_TIMESTAMP
        lastImpulseMagnitudeSquared = 0f
    }

    private companion object {
        const val REQUIRED_ALTERNATING_IMPULSES = 3
        const val STRONG_IMPULSE_METERS_PER_SECOND_SQUARED = 6f
        const val QUIET_METERS_PER_SECOND_SQUARED = 1.5f
        const val MAX_REVERSAL_DIRECTION_SIMILARITY = -0.55f
        const val MIN_SAME_DIRECTION_SIMILARITY = 0.55f
        const val MIN_IMPULSE_INTERVAL_NANOS = 45_000_000L
        const val MAX_IMPULSE_INTERVAL_NANOS = 350_000_000L
        const val MAX_GESTURE_DURATION_NANOS = 700_000_000L
        const val COOLDOWN_NANOS = 1_000_000_000L
        const val REARM_QUIET_PERIOD_NANOS = 250_000_000L
        const val UNSET_TIMESTAMP = Long.MIN_VALUE
        const val STRONG_IMPULSE_METERS_PER_SECOND_SQUARED_SQUARED =
            STRONG_IMPULSE_METERS_PER_SECOND_SQUARED * STRONG_IMPULSE_METERS_PER_SECOND_SQUARED
        const val QUIET_METERS_PER_SECOND_SQUARED_SQUARED =
            QUIET_METERS_PER_SECOND_SQUARED * QUIET_METERS_PER_SECOND_SQUARED
    }
}
