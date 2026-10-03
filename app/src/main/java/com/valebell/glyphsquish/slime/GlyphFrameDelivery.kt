package com.valebell.glyphsquish.slime

/** Confined to the service's main thread, along with frame generation and SDK calls. */
internal class GlyphFrameDelivery {
    private var lastSubmittedFrame: IntArray? = null
    private var lastSubmissionMillis: Long? = null

    fun sendIfNeeded(
        frame: IntArray,
        nowMillis: Long,
        send: (IntArray) -> Unit,
    ) {
        val previousSubmission = lastSubmissionMillis
        if (previousSubmission != null &&
            nowMillis - previousSubmission in 0 until RETRY_INTERVAL_MILLIS &&
            lastSubmittedFrame?.contentEquals(frame) == true
        ) {
            return
        }
        send(frame)
        // SDK 2.0 swallows RemoteException: a normal return is not a delivery acknowledgment.
        // Retry unchanged output periodically so a dropped frame cannot stay suppressed.
        lastSubmittedFrame = frame.copyOf()
        lastSubmissionMillis = nowMillis
    }

    fun reset() {
        lastSubmittedFrame = null
        lastSubmissionMillis = null
    }

    private companion object {
        const val RETRY_INTERVAL_MILLIS = 1_000L
    }
}
