package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureForms
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.domain.CreatureSession
import com.valebell.glyphsquish.creature.domain.angularTrace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SlimeGestureRecognitionTest {
    @Test
    fun physicalRimCirclesRecognizeHelixAcrossFormsDirectionsAndFrameRates() {
        for (form in CreatureForm.entries) {
            for (frameRate in listOf(20, 30, 120)) {
                for (periodSeconds in listOf(4.0, 8.0)) {
                    for (direction in listOf(-1, 1)) {
                        val glyph = GlyphSession(form)
                        glyph.holdTilt(frameRate)
                        glyph.rotate(periodSeconds, frameRate, direction)

                        val trace = "$form, $frameRate fps, $periodSeconds seconds/turn, direction $direction"
                        assertTrue(trace, SlimeButtonAction.HELIX in glyph.recognizedMoves)
                        assertTrue("No sliding contacts: $trace", glyph.weakBoundaryContacts > 0)
                    }
                }
            }
        }
    }

    @Test
    fun idleHeldAndNoisyTiltDoNotRecognizeRecipes() {
        for (amplitude in listOf(0f, 0.8f)) {
            val glyph = GlyphSession()
            val initial = glyph.session.state
            glyph.advance(30.0, 60) { time ->
                val angle = sin(time * 17.0) * 0.1
                glyph.engine.setTilt(amplitude * cos(angle).toFloat(), amplitude * sin(angle).toFloat(), 0.6f)
            }

            assertTrue(glyph.performances.isEmpty())
            assertEquals(initial, glyph.session.state)
        }
    }

    @Test
    fun physicalCircleCompletesThunderboltAfterBounceAndButton() {
        val glyph = GlyphSession()
        glyph.holdTilt()
        glyph.beginThunderbolt()

        assertTrue(glyph.performances.isEmpty())
        assertEquals(
            2,
            glyph.session.recognition.recipes
                .getValue(SlimeButtonAction.THUNDERBOLT)
                .completedSteps,
        )
        glyph.rotate(periodSeconds = 6.0, turns = 2)

        assertTrue(SlimeButtonAction.THUNDERBOLT in glyph.session.state.unlockedMoves)
        assertTrue(SlimeButtonAction.THUNDERBOLT in glyph.performances)
    }

    @Test
    fun multipleMovesUnlockInOneEngineSessionAfterTheirAnimationsFinish() {
        val glyph = GlyphSession()
        glyph.holdTilt()
        glyph.rotate(periodSeconds = 6.0)
        assertTrue(SlimeButtonAction.HELIX in glyph.session.state.unlockedMoves)
        glyph.finishAnimation()

        glyph.feed(angularTrace(listOf(180.0, 0.0, 180.0, 0.0)))
        assertTrue(SlimeButtonAction.ECHO in glyph.session.state.unlockedMoves)
        glyph.finishAnimation()

        glyph.feed(angularTrace(listOf(315.0, 135.0, 315.0, 135.0)))
        assertTrue(SlimeButtonAction.RICOCHET in glyph.session.state.unlockedMoves)
        glyph.finishAnimation()

        glyph.holdTilt()
        glyph.beginThunderbolt()
        glyph.rotate(periodSeconds = 6.0, turns = 2)

        assertEquals(SlimeButtonAction.entries.toSet(), glyph.session.state.unlockedMoves)
        assertEquals(5L, glyph.session.state.revision)
        assertEquals(SlimeButtonAction.entries.toSet() - SlimeButtonAction.BURST, glyph.performances.toSet())
    }

    @Test
    fun eachRecipePlaysItsUnlockAnimationOnlyOnceRegardlessOfFormAcknowledgement() {
        for ((move, recipe) in replayRecipes()) {
            val glyph = GlyphSession()
            glyph.feed(recipe)
            glyph.playAndVerifyAnimation(move)
            glyph.session.replace(CreatureProgression.selectMove(glyph.session.state, SlimeButtonAction.BURST))
            repeat(2) { acknowledgement ->
                if (acknowledgement == 1) {
                    glyph.session.replace(
                        CreatureProgression.markFormsSeen(glyph.session.state, CreatureForm.entries.toSet()),
                    )
                }
                val learned = glyph.session.state
                glyph.feed(recipe)
                assertFalse("Learned $move queued another unlock animation", glyph.engine.isMovePlaying)
                glyph.advance(1.0 / 30, 30)
                assertFalse("Learned $move started another unlock animation", glyph.engine.isMovePlaying)
                assertEquals("Repeated gesture changed progression for $move", learned, glyph.session.state)
            }

            assertEquals(listOf(move), glyph.performances)
        }
    }

    private fun replayRecipes(): Map<SlimeButtonAction, List<CreatureInteraction>> {
        val circle =
            (0..72).map { step ->
                val angle = step * 2 * PI / 72
                val radius = if (step in 12..60) 0.6f else 1f
                CreatureInteraction.CirclePosition(radius * cos(angle).toFloat(), radius * sin(angle).toFloat(), 0f)
            }
        return mapOf(
            SlimeButtonAction.HELIX to circle + circle.drop(1),
            SlimeButtonAction.ECHO to angularTrace(listOf(180.0, 0.0, 180.0, 0.0)),
            SlimeButtonAction.RICOCHET to angularTrace(listOf(315.0, 135.0, 315.0, 135.0)),
            SlimeButtonAction.THUNDERBOLT to
                angularTrace(listOf(180.0, 0.0)) + CreatureInteraction.GlyphButtonPressed + circle,
        )
    }

    private class GlyphSession(
        form: CreatureForm = CreatureForm.BLOB,
    ) {
        val session =
            CreatureSession(
                CreatureProgression.initialState().let { initial ->
                    val available =
                        CreatureForms.requiredMove(form)?.let { CreatureProgression.unlockMove(initial, it) } ?: initial
                    CreatureProgression.selectForm(available, form)
                },
            )
        val recognizedMoves = mutableListOf<SlimeButtonAction>()
        val performances = mutableListOf<SlimeButtonAction>()
        var weakBoundaryContacts = 0
            private set
        private var nowSeconds = 0.0
        val engine =
            SlimeEngine(
                initialPhenotype = SlimePhenotype.from(session.state),
                interactionHandler = { interaction ->
                    if (interaction is CreatureInteraction.ShellContact &&
                        interaction.hasReliablePosition && interaction.strength < 0.05f
                    ) {
                        weakBoundaryContacts++
                    }
                    val outcome = session.handle(interaction, (nowSeconds * 1_000).toLong())
                    outcome.recognizedMove?.let(recognizedMoves::add)
                    outcome.unlockedMove?.also(performances::add)
                },
            )

        fun holdTilt(frameRate: Int = 30) {
            engine.setTilt(0.8f, 0f, 0.6f)
            advance(3.0, frameRate)
        }

        fun rotate(
            periodSeconds: Double,
            frameRate: Int = 30,
            direction: Int = 1,
            turns: Int = 3,
        ) {
            advance(periodSeconds * turns, frameRate) { time ->
                val angle = direction * time * 2 * PI / periodSeconds
                engine.setTilt(0.8f * cos(angle).toFloat(), 0.8f * sin(angle).toFloat(), 0.6f)
            }
        }

        fun beginThunderbolt() {
            // Isolate this injected prefix from firm contacts made while positioning the body.
            session.resetRecognition((nowSeconds * 1_000).toLong())
            feed(angularTrace(listOf(180.0, 0.0)) + CreatureInteraction.GlyphButtonPressed)
        }

        fun feed(interactions: List<CreatureInteraction>) {
            interactions.forEach {
                nowSeconds += 0.1
                engine.simulateInteraction(it)
            }
        }

        fun finishAnimation() {
            engine.setTilt(0f, 0f, 0f)
            advance(4.0, 30)
            assertFalse(engine.isMovePlaying)
        }

        fun playAndVerifyAnimation(move: SlimeButtonAction) {
            assertTrue("$move did not queue its animation", engine.isMovePlaying)
            advance(1.0 / 30, 30)
            assertEquals(move, engine.snapshot().activeButtonAction)
            val firstFrame = engine.renderFrame()
            advance(0.4, 30)
            assertEquals(move, engine.snapshot().activeButtonAction)
            assertFalse("$move did not animate", firstFrame.contentEquals(engine.renderFrame()))
            finishAnimation()
        }

        fun advance(
            seconds: Double,
            frameRate: Int,
            updateTilt: (Double) -> Unit = {},
        ) {
            val delta = 1.0 / frameRate
            repeat((seconds * frameRate).toInt()) { frame ->
                updateTilt(frame * delta)
                nowSeconds += delta
                engine.step(delta.toFloat())
            }
        }
    }
}
