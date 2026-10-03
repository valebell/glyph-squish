package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionResetTest {
    @Test
    fun depthAndNonFiniteInputResetActualProgress() {
        for (invalid in listOf(
            cardinal(ShellSector.FRONT),
            cardinal(ShellSector.RIGHT).copy(normalX = Float.NaN),
            cardinal(ShellSector.RIGHT, 0.01f).copy(normalY = Float.NaN),
            cardinal(ShellSector.RIGHT).copy(strength = Float.POSITIVE_INFINITY),
        )) {
            val recognizer = MoveUnlockRecognizer()
            val now = feed(recognizer, circle(), 0, setOf(SlimeButtonAction.BURST))
            assertEquals(
                1,
                recognizer.progress.recipes
                    .getValue(SlimeButtonAction.HELIX)
                    .completedSteps,
            )
            recognizer.accept(invalid, now, setOf(SlimeButtonAction.BURST))
            assertEquals(
                0,
                recognizer.progress.recipes
                    .getValue(SlimeButtonAction.HELIX)
                    .completedSteps,
            )
            assertEquals(RecognitionReset.WEAK_OR_DEPTH, recognizer.progress.resetReason)
        }
    }

    @Test
    fun pausesExpireWithoutAnotherSensorEventAndBackwardTimeResets() {
        val recognizer = MoveUnlockRecognizer()
        var now = feed(recognizer, circle(), 0, setOf(SlimeButtonAction.BURST))
        recognizer.expireOldInput(now + 4_001)
        assertEquals(
            0,
            recognizer.progress.recipes
                .getValue(SlimeButtonAction.HELIX)
                .completedSteps,
        )
        assertEquals(RecognitionReset.TIMED_OUT, recognizer.progress.resetReason)
        now = feed(recognizer, circle(), now + 5_000, setOf(SlimeButtonAction.BURST))
        recognizer.accept(circle().first(), now - 500, setOf(SlimeButtonAction.BURST))
        assertEquals(
            0,
            recognizer.progress.recipes
                .getValue(SlimeButtonAction.HELIX)
                .completedSteps,
        )
    }

    @Test
    fun sessionInterruptionDiscardsPartialRecipesAndSuppressesShakeContacts() {
        val session = CreatureSession(CreatureProgression.initialState())
        circle().forEachIndexed { index, contact -> session.handle(contact, index * 100L) }
        session.resetRecognition(2_000, suppressContactsForMillis = 900)
        circle().forEachIndexed { index, contact -> session.handle(contact, 2_000 + index * 10L) }
        assertEquals(RecognitionReset.INTERRUPTED, session.recognition.resetReason)
        circle().forEachIndexed { index, contact -> session.handle(contact, 3_000 + index * 100L) }
        assertEquals(
            1,
            session.recognition.recipes
                .getValue(SlimeButtonAction.HELIX)
                .completedSteps,
        )
        assertTrue(SlimeButtonAction.HELIX !in session.state.unlockedMoves)
    }
}
