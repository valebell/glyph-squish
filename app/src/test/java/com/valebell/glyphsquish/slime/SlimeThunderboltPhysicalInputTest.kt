package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.domain.CreatureSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SlimeThunderboltPhysicalInputTest {
    @Test
    fun immediateBounceThenHeldButtonAndPhysicalCircleUnlockThunderbolt() {
        verifyPhysicalRecipe(firstEdgeSeconds = 0.7)
    }

    @Test
    fun settledBounceThenHeldButtonAndPhysicalCircleUnlockThunderbolt() {
        verifyPhysicalRecipe(firstEdgeSeconds = 1.5)
    }

    @Test
    fun repeatingSelectedPlaybackKeepsTheBounceForFollowingPhysicalCircle() {
        verifyPhysicalRecipe(firstEdgeSeconds = 1.5, buttonPressCount = 2)
    }

    private fun verifyPhysicalRecipe(
        firstEdgeSeconds: Double,
        buttonPressCount: Int = 1,
    ) {
        for (frameRate in listOf(20, 30, 120)) {
            for (direction in listOf(-1, 1)) {
                val glyph = PhysicalGlyph(frameRate)
                val trace = "$firstEdgeSeconds seconds at first edge, $frameRate fps, direction $direction"
                glyph.engine.setTilt(-0.8f, 0f, 0.6f)
                glyph.advance(firstEdgeSeconds)
                glyph.engine.setTilt(0.8f, 0f, 0.6f)
                repeat(frameRate * 3) {
                    if (glyph.completedSteps != 1) glyph.advance(1.0 / frameRate)
                }
                assertEquals("Opposite impacts did not register a bounce: $trace", 1, glyph.completedSteps)

                repeat(buttonPressCount) { glyph.holdAndPlaySelectedMove(trace) }

                // Continue slightly beyond one tilt turn so the physical body can follow through.
                glyph.advance(7.5) { time ->
                    val angle = direction * time * 2 * PI / 6.0
                    glyph.engine.setTilt(0.8f * cos(angle).toFloat(), 0.8f * sin(angle).toFloat(), 0.6f)
                }
                assertTrue("Physical circle did not unlock Thunderbolt: $trace", glyph.isThunderboltUnlocked)
                val expectedMoves = List(buttonPressCount) { SlimeButtonAction.BURST } + SlimeButtonAction.THUNDERBOLT
                assertEquals(trace, expectedMoves, glyph.performances)
            }
        }
    }

    private class PhysicalGlyph(
        private val frameRate: Int,
    ) {
        private val session = CreatureSession(CreatureProgression.initialState())
        private var nowSeconds = 0.0
        val performances = mutableListOf<SlimeButtonAction>()
        val engine =
            SlimeEngine(
                interactionHandler = ::handle,
                onMovePlaybackChanged = { active ->
                    session.setRecognitionPaused(active, (nowSeconds * 1_000).toLong())
                },
            )
        val completedSteps: Int
            get() =
                session.recognition.recipes
                    .getValue(SlimeButtonAction.THUNDERBOLT)
                    .completedSteps
        val isThunderboltUnlocked: Boolean
            get() = SlimeButtonAction.THUNDERBOLT in session.state.unlockedMoves

        fun handle(interaction: CreatureInteraction): SlimeButtonAction? =
            session.handle(interaction, (nowSeconds * 1_000).toLong()).unlockedMove?.also(performances::add)

        fun holdAndPlaySelectedMove(trace: String) {
            engine.setPressed(true)
            advance(1.0)
            engine.handleGlyphButtonPress {
                handle(CreatureInteraction.GlyphButtonPressed)
                assertEquals("Holding the button lost the bounce: $trace", 2, completedSteps)
                session.state.selectedMove.also(performances::add)
            }
            engine.setPressed(false)
            assertEquals(
                "The held button must play the selected move: $trace",
                SlimeButtonAction.BURST,
                engine.snapshot().activeButtonAction,
            )
            repeat(frameRate * 3) {
                if (engine.isMovePlaying) advance(1.0 / frameRate)
            }
            assertFalse("Selected move did not finish: $trace", engine.isMovePlaying)
        }

        fun advance(
            seconds: Double,
            updateTilt: (Double) -> Unit = {},
        ) {
            repeat((seconds * frameRate).toInt()) { frame ->
                updateTilt(frame.toDouble() / frameRate)
                nowSeconds += 1.0 / frameRate
                engine.step(1f / frameRate)
            }
        }
    }
}
