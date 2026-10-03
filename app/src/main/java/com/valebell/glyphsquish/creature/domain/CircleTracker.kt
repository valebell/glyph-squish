package com.valebell.glyphsquish.creature.domain

import kotlin.math.abs
import kotlin.math.max

/** Tracks directed travel, independently of impact recipes and contact frequency. */
internal class CircleTracker {
    private var circleTrace: CircleTrace? = null
    val hasTrace: Boolean get() = circleTrace != null
    val isDeveloped: Boolean get() = abs(circleTrace?.signedTravel ?: 0.0) >= DIRECTION_LOCK_RADIANS
    val lastProgressTimeMillis: Long? get() = circleTrace?.lastProgressTimeMillis

    fun reset() {
        circleTrace = null
    }

    fun advance(
        angle: Double,
        nowMillis: Long,
    ): CircleUpdate {
        val trace = circleTrace
        if (trace == null) {
            circleTrace = newCircleTrace(angle, nowMillis)
            return CircleUpdate.STARTED
        }

        val delta = signedAngularDelta(trace.previousAngle, angle)
        if (abs(delta) > MAXIMUM_ANGULAR_STEP_RADIANS) return CircleUpdate.INVALID

        if (abs(delta) < ANGULAR_NOISE_RADIANS &&
            abs(trace.signedTravel + delta) < MINIMUM_CIRCLE_TRAVEL_RADIANS
        ) {
            return CircleUpdate.TRACKING
        }
        trace.previousAngle = angle

        trace.signedTravel += delta
        trace.absoluteTravel += abs(delta)

        updateCircleProgress(trace, nowMillis)
        if (trace.direction == 0) return CircleUpdate.TRACKING

        val directedTravel = trace.signedTravel * trace.direction
        trace.peakDirectedTravel = max(trace.peakDirectedTravel, directedTravel)
        if (trace.peakDirectedTravel - directedTravel > MAXIMUM_BACKTRACK_RADIANS) {
            return CircleUpdate.INVALID
        }

        val directionConsistency =
            if (trace.absoluteTravel == 0.0) {
                0.0
            } else {
                directedTravel / trace.absoluteTravel
            }
        return if (
            directedTravel >= MINIMUM_CIRCLE_TRAVEL_RADIANS &&
            directionConsistency >= MINIMUM_DIRECTION_CONSISTENCY
        ) {
            val remainder = max(0.0, abs(trace.signedTravel) - TWO_PI)
            circleTrace =
                newCircleTrace(angle - trace.direction * remainder, nowMillis).apply {
                    previousAngle = angle
                    signedTravel = trace.direction * remainder
                    absoluteTravel = remainder
                }
            CircleUpdate.COMPLETED
        } else {
            CircleUpdate.TRACKING
        }
    }

    private fun updateCircleProgress(
        trace: CircleTrace,
        nowMillis: Long,
    ) {
        if (trace.direction == 0 && abs(trace.signedTravel) >= DIRECTION_LOCK_RADIANS) {
            trace.direction = if (trace.signedTravel > 0.0) 1 else -1
        }
        val currentProgress =
            if (trace.direction == 0) {
                abs(trace.signedTravel)
            } else {
                max(0.0, trace.signedTravel * trace.direction)
            }
        if (currentProgress - trace.progressCheckpoint >= MINIMUM_PROGRESS_CHECKPOINT_RADIANS) {
            trace.progressCheckpoint = currentProgress
            trace.lastProgressTimeMillis = nowMillis
        }
    }

    private fun newCircleTrace(
        angle: Double,
        nowMillis: Long,
    ) = CircleTrace(
        previousAngle = angle,
        lastProgressTimeMillis = nowMillis,
    )

    private data class CircleTrace(
        var previousAngle: Double,
        var signedTravel: Double = 0.0,
        var absoluteTravel: Double = 0.0,
        var direction: Int = 0,
        var peakDirectedTravel: Double = 0.0,
        var lastProgressTimeMillis: Long,
        var progressCheckpoint: Double = 0.0,
    )

    private companion object {
        const val MINIMUM_DIRECTION_CONSISTENCY = 0.50
        val ANGULAR_NOISE_RADIANS = Math.toRadians(2.0)
        val MINIMUM_PROGRESS_CHECKPOINT_RADIANS = Math.toRadians(5.0)
        val MAXIMUM_BACKTRACK_RADIANS = Math.toRadians(90.0)
        val MINIMUM_CIRCLE_TRAVEL_RADIANS = TWO_PI - 0.00001
    }
}
