package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureForms
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.domain.CreatureSession
import com.valebell.glyphsquish.creature.domain.angularTrace
import com.valebell.glyphsquish.creature.domain.signedAngularDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

class SlimeOffRimRecognitionTest {
    @Test
    fun stationaryPhonesDoNotRecognizeIdleDriftAsGesturesInAnyForm() {
        val orientations =
            listOf(
                Triple(0f, 0f, 1f),
                Triple(0.12f, 0f, sqrt(1f - 0.12f * 0.12f)),
                Triple(0.4f, 0.2f, sqrt(0.8f)),
            )
        for (form in CreatureForm.entries) {
            for ((x, y, z) in orientations) {
                val glyph = GlyphSession(form)
                val initial = glyph.session.state
                glyph.holdTilt(x, y, z)

                val trace = "$form held at ($x, $y, $z) for 45 seconds"
                assertTrue("Idle drift recognized a gesture: $trace", glyph.recognizedMoves.isEmpty())
                assertEquals(trace, initial, glyph.session.state)
            }
        }
    }

    @Test
    fun fastPhysicalCirclesRecognizeAcrossFormsDirectionsAndFrameRates() {
        for (form in CreatureForm.entries) {
            for (frameRate in listOf(20, 30, 120)) {
                for (direction in listOf(-1, 1)) {
                    val glyph = GlyphSession(form)
                    val path = glyph.rotate(frameRate, direction, uneven = false)
                    val trace = "$form, $frameRate fps, direction $direction: $path"

                    assertTrue("Character did not complete two turns: $trace", path.travel * direction > 4 * PI)
                    assertTrue("Helix missing: $trace", SlimeButtonAction.HELIX in glyph.recognizedMoves)
                    if (form == CreatureForm.BLOB || form == CreatureForm.CAT) {
                        assertTrue("Trace did not leave the border: $trace", path.offBorderFrames > frameRate)
                    }
                }
            }
        }
    }

    @Test
    fun unevenPhysicalCirclesBridgeMissingWallContacts() {
        for (form in listOf(CreatureForm.BLOB, CreatureForm.CAT)) {
            for (frameRate in listOf(20, 30, 120)) {
                for (direction in listOf(-1, 1)) {
                    val glyph = GlyphSession(form)
                    val path = glyph.rotate(frameRate, direction, uneven = true)
                    val trace = "$form, $frameRate fps, direction $direction: $path"

                    assertTrue("Character did not complete two turns: $trace", path.travel * direction > 4 * PI)
                    assertTrue("Trace did not leave the border: $trace", path.offBorderFrames > frameRate)
                    assertTrue("Helix missing: $trace", SlimeButtonAction.HELIX in glyph.recognizedMoves)
                }
            }
        }
    }

    @Test
    fun positionSamplesThroughTheCentrePreserveDiscreteImpactRecipes() {
        val recipes =
            mapOf(
                SlimeButtonAction.ECHO to listOf(180.0, 0.0, 180.0, 0.0),
                SlimeButtonAction.RICOCHET to listOf(315.0, 135.0, 315.0, 135.0),
            )
        for ((move, angles) in recipes) {
            val glyph = GlyphSession()
            glyph.crossWithImpacts(angles)

            assertTrue("Centre samples interrupted $move", move in glyph.session.state.unlockedMoves)
        }
    }

    @Test
    fun centreCrossingBreaksHelixStreakButPreservesThunderboltPrefix() {
        val helix = GlyphSession()
        helix.feed(circlePositions())
        helix.feed(listOf(CreatureInteraction.CirclePosition(0.1f, 0f, 0f)))
        helix.feed(circlePositions())
        assertTrue(helix.recognizedMoves.isEmpty())
        assertEquals(
            1,
            helix.session.recognition.recipes
                .getValue(SlimeButtonAction.HELIX)
                .completedSteps,
        )

        val thunderbolt = GlyphSession()
        thunderbolt.crossWithImpacts(listOf(180.0, 0.0))
        thunderbolt.feed(
            listOf(
                CreatureInteraction.GlyphButtonPressed,
                CreatureInteraction.CirclePosition(0.1f, 0f, 0f),
            ),
        )
        thunderbolt.feed(circlePositions())
        assertEquals(listOf(SlimeButtonAction.THUNDERBOLT), thunderbolt.recognizedMoves)
    }

    @Test
    fun centreCrossingsWithoutImpactsCannotUnlockDiscreteMoves() {
        val glyph = GlyphSession()
        for (angle in listOf(180.0, 0.0, 180.0, 0.0, 315.0, 135.0, 315.0, 135.0)) {
            val radians = Math.toRadians(angle)
            glyph.feed(
                listOf(
                    CreatureInteraction.CirclePosition(0f, 0f, 0f),
                    CreatureInteraction.CirclePosition(cos(radians).toFloat(), sin(radians).toFloat(), 0f),
                ),
            )
        }

        assertEquals(setOf(SlimeButtonAction.BURST), glyph.session.state.unlockedMoves)
        assertTrue(glyph.recognizedMoves.isEmpty())
    }

    private fun circlePositions(): List<CreatureInteraction.CirclePosition> =
        (0..32).map { step ->
            val angle = step * 2 * PI / 32
            CreatureInteraction.CirclePosition(0.85f * cos(angle).toFloat(), 0.85f * sin(angle).toFloat(), 0f)
        }

    private data class Path(
        var previousAngle: Double,
        var travel: Double = 0.0,
        var offBorderFrames: Int = 0,
        var minimumRadius: Double = 7.0,
    ) {
        fun observe(snapshot: SlimeSnapshot) {
            val angle = atan2(snapshot.y.toDouble(), snapshot.x.toDouble())
            travel += signedAngularDelta(previousAngle, angle)
            previousAngle = angle
            val radius = hypot(snapshot.x.toDouble(), snapshot.y.toDouble())
            minimumRadius = minOf(minimumRadius, radius)
            if (radius < 6.8) offBorderFrames++
        }
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
        private var nowSeconds = 0.0
        private val engine =
            SlimeEngine(
                initialPhenotype = SlimePhenotype.from(session.state),
                interactionHandler = { interaction ->
                    session.handle(interaction, (nowSeconds * 1_000).toLong()).recognizedMove?.let(recognizedMoves::add)
                    // Observe raw recognition, including learned moves, without altering the physical trajectory.
                    null
                },
            )

        fun holdTilt(
            x: Float,
            y: Float,
            z: Float,
        ) {
            engine.setTilt(x, y, z)
            repeat(45 * 30) { step(1.0 / 30) }
        }

        fun rotate(
            frameRate: Int,
            direction: Int,
            uneven: Boolean,
        ): Path {
            val delta = 1.0 / frameRate
            val amplitude = if (uneven) 0.5 else 0.8
            val periodSeconds = if (uneven) 3.5 else 2.2
            engine.setTilt(amplitude.toFloat(), 0f, 0.6f)
            repeat(3 * frameRate) { step(delta) }
            val settled = engine.snapshot()
            val path = Path(atan2(settled.y.toDouble(), settled.x.toDouble()))
            repeat((periodSeconds * 3 * frameRate).toInt()) { frame ->
                val angle = direction * frame * delta * 2 * PI / periodSeconds
                val tilt = amplitude * if (uneven) 0.75 + 0.25 * cos(angle) else 1.0
                engine.setTilt((tilt * cos(angle)).toFloat(), (tilt * sin(angle)).toFloat(), 0.6f)
                step(delta)
                path.observe(engine.snapshot())
            }
            return path
        }

        fun crossWithImpacts(angles: List<Double>) {
            val impacts = angularTrace(angles)
            impacts.forEachIndexed { index, impact ->
                if (index > 0) {
                    val previous = impacts[index - 1]
                    feed(
                        (1..9).map { sample ->
                            val fraction = sample / 10f
                            CreatureInteraction.CirclePosition(
                                previous.normalX + (impact.normalX - previous.normalX) * fraction,
                                previous.normalY + (impact.normalY - previous.normalY) * fraction,
                                0f,
                            )
                        },
                    )
                }
                feed(listOf(impact, CreatureInteraction.CirclePosition(impact.normalX, impact.normalY, 0f)))
            }
        }

        fun feed(interactions: List<CreatureInteraction>) {
            interactions.forEach { interaction ->
                nowSeconds += 0.02
                engine.simulateInteraction(interaction)
            }
        }

        private fun step(delta: Double) {
            nowSeconds += delta
            engine.step(delta.toFloat())
        }
    }
}
