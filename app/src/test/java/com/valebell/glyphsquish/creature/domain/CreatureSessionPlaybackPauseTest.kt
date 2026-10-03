package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreatureSessionPlaybackPauseTest {
    @Test
    fun playbackLongerThanTheInputTimeoutPreservesThunderbolt() {
        val session = armedThunderbolt()
        session.setRecognitionPaused(true, 200)
        session.setRecognitionPaused(true, 3_000)
        session.setRecognitionPaused(false, 7_200)

        assertEquals(SlimeButtonAction.THUNDERBOLT, completeCircle(session, 7_300))
    }

    @Test
    fun theNormalTimeoutResumesAfterPlayback() {
        val session = armedThunderbolt()
        session.setRecognitionPaused(true, 200)
        session.setRecognitionPaused(false, 7_200)

        assertNull(completeCircle(session, 11_201))
        assertEquals(0, session.thunderboltSteps())
    }

    @Test
    fun pausingExcludesPlaybackTimeWithoutRefreshingEarlierIdleTime() {
        val session = armedThunderbolt()
        session.setRecognitionPaused(true, 3_900)
        session.setRecognitionPaused(false, 10_900)

        assertNull(completeCircle(session, 11_201))
        assertEquals(0, session.thunderboltSteps())
    }

    @Test
    fun motionDuringPlaybackCannotCompleteOrInterruptTheArmedRecipe() {
        val session = armedThunderbolt()
        session.setRecognitionPaused(true, 200)
        val animationMotion =
            listOf(cardinal(ShellSector.FRONT), cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT)) +
                circle() + circle()
        animationMotion.forEachIndexed { index, interaction ->
            assertNull(session.handle(interaction, 300 + index * 100L).unlockedMove)
        }
        assertEquals(2, session.thunderboltSteps())
        assertEquals(setOf(SlimeButtonAction.BURST), session.state.unlockedMoves)

        session.setRecognitionPaused(false, 7_200)
        assertEquals(SlimeButtonAction.THUNDERBOLT, completeCircle(session, 7_300))
    }

    @Test
    fun theGlyphButtonCanStillAdvanceARecipeDuringAPlaybackPause() {
        val session = CreatureSession(CreatureProgression.initialState())
        session.handle(cardinal(ShellSector.BOTTOM), 0)
        session.handle(cardinal(ShellSector.TOP), 100)
        session.setRecognitionPaused(true, 150)

        session.handle(CreatureInteraction.GlyphButtonPressed, 5_000)

        assertEquals(2, session.thunderboltSteps())
        session.setRecognitionPaused(false, 8_000)
        assertEquals(SlimeButtonAction.THUNDERBOLT, completeCircle(session, 8_100))
    }

    @Test
    fun pausingCannotReviveAnAlreadyExpiredPrefix() {
        val session = armedThunderbolt()
        session.setRecognitionPaused(true, 4_201)
        session.setRecognitionPaused(false, 10_000)

        assertNull(completeCircle(session, 10_100))
        assertEquals(0, session.thunderboltSteps())
    }

    @Test
    fun resetDuringPlaybackClearsThePrefixAndPauseClock() {
        val session = armedThunderbolt()
        session.setRecognitionPaused(true, 200)
        session.resetRecognition(5_000)
        assertEquals(0, session.thunderboltSteps())
        session.setRecognitionPaused(false, 8_000)

        session.handle(cardinal(ShellSector.BOTTOM), 8_000)
        session.handle(cardinal(ShellSector.TOP), 8_100)
        session.handle(CreatureInteraction.GlyphButtonPressed, 8_200)
        assertEquals(2, session.thunderboltSteps())
        assertEquals(SlimeButtonAction.THUNDERBOLT, completeCircle(session, 8_300))
    }

    @Test
    fun aBackwardClockDuringPlaybackDiscardsTheOldAttempt() {
        val session = armedThunderbolt()
        session.setRecognitionPaused(true, 1_000)
        session.setRecognitionPaused(false, 500)

        assertEquals(0, session.thunderboltSteps())
        assertNull(completeCircle(session, 600))
    }

    private fun armedThunderbolt(): CreatureSession =
        CreatureSession(CreatureProgression.initialState()).apply {
            handle(cardinal(ShellSector.BOTTOM), 0)
            handle(cardinal(ShellSector.TOP), 100)
            handle(CreatureInteraction.GlyphButtonPressed, 200)
            assertEquals(2, thunderboltSteps())
        }

    private fun completeCircle(
        session: CreatureSession,
        startMillis: Long,
    ): SlimeButtonAction? {
        var unlockedMove: SlimeButtonAction? = null
        circle().forEachIndexed { index, interaction ->
            val result = session.handle(interaction, startMillis + index * 100L).unlockedMove
            if (index < 16) assertNull(result)
            unlockedMove = result ?: unlockedMove
        }
        return unlockedMove
    }

    private fun CreatureSession.thunderboltSteps(): Int =
        recognition.recipes
            .getValue(SlimeButtonAction.THUNDERBOLT)
            .completedSteps
}
