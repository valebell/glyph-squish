package com.valebell.glyphsquish.slime

/** Physical input controls this envelope; simulated creature movement does not. */
internal class GlyphIdleBrightness {
    private var lastActivityMillis = 0L
    private var transitionStartMillis = 0L
    private var transitionDurationMillis = 1L
    private var transitionFrom = ACTIVE_LEVEL
    private var transitionTo = ACTIVE_LEVEL
    private val tiltHistory = ArrayDeque<MotionSample>()
    private var accelerationStartedMillis: Long? = null

    fun reset(nowMillis: Long) {
        lastActivityMillis = nowMillis
        transitionStartMillis = nowMillis
        transitionDurationMillis = 1L
        transitionFrom = ACTIVE_LEVEL
        transitionTo = ACTIVE_LEVEL
        tiltHistory.clear()
        accelerationStartedMillis = null
    }

    fun onMotion(
        nowMillis: Long,
        tiltX: Float,
        tiltY: Float,
        tiltZ: Float,
        accelerationX: Float,
        accelerationY: Float,
        accelerationZ: Float,
    ) {
        dimIfIdle(nowMillis)
        val tilt = MotionSample(nowMillis, tiltX, tiltY, tiltZ)
        val previousTime = tiltHistory.lastOrNull()?.timeMillis
        if (previousTime != null && nowMillis < previousTime) tiltHistory.clear()
        while (tiltHistory.isNotEmpty() && nowMillis - tiltHistory.first().timeMillis > TILT_WINDOW_MILLIS) {
            tiltHistory.removeFirst()
        }
        val tilted = tiltHistory.any { tilt.distanceSquared(it) >= TILT_THRESHOLD * TILT_THRESHOLD }
        val accelerated =
            sustainedAcceleration(
                nowMillis,
                accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ,
                previousTime,
            )
        if (tilted || accelerated) {
            tiltHistory.clear()
            wake(nowMillis)
        }
        if (tiltHistory.lastOrNull()?.timeMillis == nowMillis) tiltHistory.removeLast()
        tiltHistory.addLast(tilt)
    }

    private fun sustainedAcceleration(
        nowMillis: Long,
        magnitudeSquared: Float,
        previousTime: Long?,
    ): Boolean {
        val strong = magnitudeSquared >= ACCELERATION_THRESHOLD * ACCELERATION_THRESHOLD
        if (!strong || previousTime == null || nowMillis - previousTime !in 0..MAX_SAMPLE_GAP_MILLIS) {
            accelerationStartedMillis = null
        }
        if (!strong) return false
        val started = accelerationStartedMillis ?: nowMillis.also { accelerationStartedMillis = it }
        return nowMillis - started >= ACCELERATION_HOLD_MILLIS
    }

    fun wake(nowMillis: Long) {
        dimIfIdle(nowMillis)
        lastActivityMillis = nowMillis
        if (transitionTo == ACTIVE_LEVEL) return
        transitionFrom = sample(nowMillis)
        transitionTo = ACTIVE_LEVEL
        transitionStartMillis = nowMillis
        transitionDurationMillis = WAKE_MILLIS
    }

    fun level(
        nowMillis: Long,
        interactionActive: Boolean = false,
    ): Float {
        if (interactionActive) wake(nowMillis) else dimIfIdle(nowMillis)
        return sample(nowMillis)
    }

    private fun dimIfIdle(nowMillis: Long) {
        if (nowMillis - lastActivityMillis < IDLE_DELAY_MILLIS || transitionTo == IDLE_LEVEL) return
        val fadeStart = lastActivityMillis + IDLE_DELAY_MILLIS
        transitionFrom = sample(fadeStart)
        transitionTo = IDLE_LEVEL
        transitionStartMillis = fadeStart
        transitionDurationMillis = DIM_MILLIS
        // Pre-idle drift must not complete a wake gesture after dimming starts.
        val latestTilt = tiltHistory.lastOrNull()
        tiltHistory.clear()
        latestTilt?.let(tiltHistory::addLast)
        accelerationStartedMillis = null
    }

    private fun sample(nowMillis: Long): Float {
        val progress =
            ((nowMillis - transitionStartMillis).toFloat() / transitionDurationMillis).coerceIn(0f, 1f)
        return lerp(transitionFrom, transitionTo, smoothStep(progress))
    }

    private data class MotionSample(
        val timeMillis: Long,
        val x: Float,
        val y: Float,
        val z: Float,
    ) {
        fun distanceSquared(other: MotionSample): Float =
            (x - other.x) * (x - other.x) +
                (y - other.y) * (y - other.y) +
                (z - other.z) * (z - other.z)
    }

    private companion object {
        const val IDLE_DELAY_MILLIS = 2_000L
        const val DIM_MILLIS = 1_500L
        const val WAKE_MILLIS = 150L
        const val ACTIVE_LEVEL = 0.85f
        const val IDLE_LEVEL = 0.25f

        // Require deliberate motion: about ten degrees within one second, or sustained acceleration.
        // These thresholds apply only to brightness, leaving creature physics and unlocks unchanged.
        const val TILT_THRESHOLD = 0.18f
        const val TILT_WINDOW_MILLIS = 1_000L
        const val ACCELERATION_THRESHOLD = 0.10f
        const val ACCELERATION_HOLD_MILLIS = 120L
        const val MAX_SAMPLE_GAP_MILLIS = 100L
    }
}
