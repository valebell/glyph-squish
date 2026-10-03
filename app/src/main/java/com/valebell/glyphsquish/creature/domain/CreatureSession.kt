package com.valebell.glyphsquish.creature.domain

class CreatureSession(
    initialState: CreatureState,
    private val recognizer: MoveUnlockRecognizer = MoveUnlockRecognizer(),
) {
    var state: CreatureState = initialState
        private set

    val recognition: RecognitionProgress get() = recognizer.progress
    private var suppressContactsUntilMillis = Long.MIN_VALUE
    private var recognitionPausedAtMillis: Long? = null
    private var recognitionPausedMillis = 0L

    fun handle(
        interaction: CreatureInteraction,
        nowMillis: Long,
    ): InteractionOutcome {
        val pausedAt = recognitionPausedAtMillis
        if (pausedAt != null && nowMillis < pausedAt) resetRecognition(nowMillis)
        if (
            interaction != CreatureInteraction.GlyphButtonPressed &&
            (recognitionPausedAtMillis != null || nowMillis < suppressContactsUntilMillis)
        ) {
            return InteractionOutcome(state, unlockedMove = null)
        }
        val recognitionTime = (recognitionPausedAtMillis ?: nowMillis) - recognitionPausedMillis
        val move = recognizer.accept(interaction, recognitionTime)
        val firstUnlock = move?.takeUnless { it in state.unlockedMoves }
        if (firstUnlock != null) {
            state = CreatureProgression.unlockMove(state, firstUnlock)
        }
        return InteractionOutcome(state, unlockedMove = firstUnlock, recognizedMove = move)
    }

    /** Playback contributes neither gesture input nor time spent waiting for the next step. */
    fun setRecognitionPaused(
        paused: Boolean,
        nowMillis: Long,
    ) {
        val pausedAt = recognitionPausedAtMillis
        if (pausedAt != null && nowMillis < pausedAt) {
            resetRecognition(nowMillis)
        } else if (pausedAt != null && !paused) {
            recognitionPausedMillis += nowMillis - pausedAt
            recognitionPausedAtMillis = null
        }
        if (paused && recognitionPausedAtMillis == null) {
            recognizer.expireOldInput(nowMillis - recognitionPausedMillis)
            recognitionPausedAtMillis = nowMillis
        }
    }

    fun replace(newState: CreatureState) {
        state = newState
    }

    fun resetRecognition(
        nowMillis: Long,
        suppressContactsForMillis: Long = 0L,
    ) {
        recognizer.reset()
        recognitionPausedAtMillis = null
        recognitionPausedMillis = 0L
        suppressContactsUntilMillis = nowMillis + suppressContactsForMillis
    }
}
