package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ThunderboltRetryRecognitionTest {
    @Test
    fun repeatedGlyphButtonKeepsTheBounceForThunderbolt() {
        val recognizer = armedThunderbolt()

        assertNull(recognizer.accept(CreatureInteraction.GlyphButtonPressed, 300))
        assertEquals(2, recognizer.thunderboltSteps())
        assertEquals(SlimeButtonAction.THUNDERBOLT, completeCircle(recognizer, 400))
    }

    @Test
    fun repeatedGlyphButtonRestartsTheCircleWithoutForgettingTheBounce() {
        val recognizer = armedThunderbolt()
        (0..8).forEach { step ->
            assertNull(recognizer.accept(position(step * 22.5), 300 + step * 100L))
        }

        assertNull(recognizer.accept(CreatureInteraction.GlyphButtonPressed, 1_200))
        assertEquals(2, recognizer.thunderboltSteps())
        // The half-circle before the second press cannot complete the final step.
        assertEquals(SlimeButtonAction.THUNDERBOLT, completeCircle(recognizer, 1_300))
    }

    @Test
    fun retryBeforeExpiryGivesTheFreshCircleItsNormalTimeout() {
        val recognizer = armedThunderbolt()

        recognizer.accept(CreatureInteraction.GlyphButtonPressed, 4_000)

        assertEquals(2, recognizer.thunderboltSteps())
        assertEquals(8_001L, recognizer.expiryMillis)
        assertEquals(SlimeButtonAction.THUNDERBOLT, completeCircle(recognizer, 7_800))
    }

    @Test
    fun retryAfterExpiryCannotReviveTheOldBounce() {
        val recognizer = armedThunderbolt()

        recognizer.accept(CreatureInteraction.GlyphButtonPressed, 4_201)

        assertEquals(0, recognizer.thunderboltSteps())
        assertNull(completeCircle(recognizer, 4_300))
    }

    @Test
    fun repeatedButtonsWithoutABounceCannotArmThunderbolt() {
        val recognizer = MoveUnlockRecognizer()
        recognizer.accept(CreatureInteraction.GlyphButtonPressed, 0)
        recognizer.accept(CreatureInteraction.GlyphButtonPressed, 100)

        assertEquals(0, recognizer.thunderboltSteps())
        assertNull(completeCircle(recognizer, 200))
    }

    private fun armedThunderbolt(): MoveUnlockRecognizer =
        MoveUnlockRecognizer().apply {
            accept(cardinal(ShellSector.BOTTOM), 0)
            accept(cardinal(ShellSector.TOP), 100)
            accept(CreatureInteraction.GlyphButtonPressed, 200)
            assertEquals(2, thunderboltSteps())
        }

    private fun completeCircle(
        recognizer: MoveUnlockRecognizer,
        startMillis: Long,
    ): SlimeButtonAction? {
        (0 until 16).forEach { step ->
            assertNull(
                "The retry must require a full fresh circle: step=$step",
                recognizer.accept(position(step * 22.5), startMillis + step * 100L),
            )
        }
        return recognizer.accept(position(360.0), startMillis + 1_600)
    }

    private fun position(degrees: Double): CreatureInteraction.CirclePosition {
        val radians = Math.toRadians(degrees)
        return CreatureInteraction.CirclePosition(cos(radians).toFloat(), sin(radians).toFloat(), 0f)
    }

    private fun MoveUnlockRecognizer.thunderboltSteps(): Int =
        progress.recipes
            .getValue(SlimeButtonAction.THUNDERBOLT)
            .completedSteps
}
