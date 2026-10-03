package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CircleRecognitionTest {
    @Test
    fun twoCirclesUnlockHelix() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = mutableSetOf(SlimeButtonAction.BURST)
        var now = 0L

        repeat(2) {
            circle().forEach { contact ->
                recognizer.accept(contact, now, unlocked)?.let(unlocked::add)
                now += 100L
            }
        }

        assertTrue(SlimeButtonAction.HELIX in unlocked)
    }

    @Test
    fun denseOffAxisGentleCirclesWithRepeatedSamplesUnlockHelix() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = setOf(SlimeButtonAction.BURST)
        val denseGentleCircle =
            circle(strength = 0.3f, startDegrees = 13.0)
                .map { it.copy(sector = ShellSector.LEFT) }
                .flatMap { contact -> List(10) { contact } }

        val result =
            feedForResult(
                recognizer,
                denseGentleCircle + denseGentleCircle,
                0L,
                unlocked,
            )

        assertEquals(SlimeButtonAction.HELIX, result)
    }

    @Test
    fun clockwiseThenCounterClockwiseCirclesUnlockHelix() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = setOf(SlimeButtonAction.BURST)

        val result =
            feedForResult(
                recognizer,
                circle(clockwise = true) + circle(clockwise = false),
                0L,
                unlocked,
            )

        assertEquals(SlimeButtonAction.HELIX, result)
    }

    @Test
    fun modestAngularJitterAndBacktrackingStillUnlockHelix() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = setOf(SlimeButtonAction.BURST)
        val jitteryCircle =
            angularTrace(
                listOf(
                    7.0,
                    27.0,
                    47.0,
                    67.0,
                    97.0,
                    92.0,
                    127.0,
                    147.0,
                    167.0,
                    187.0,
                    217.0,
                    212.0,
                    247.0,
                    267.0,
                    287.0,
                    307.0,
                    327.0,
                    347.0,
                    367.0,
                ),
            )

        val result =
            feedForResult(
                recognizer,
                jitteryCircle + jitteryCircle,
                0L,
                unlocked,
            )

        assertEquals(SlimeButtonAction.HELIX, result)
    }

    @Test
    fun timeoutBetweenCompletedCirclesBreaksHelixStreak() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = setOf(SlimeButtonAction.BURST)
        val nextTime = feed(recognizer, circle(), 0L, unlocked)

        val result =
            feedForResult(
                recognizer,
                circle(),
                nextTime + 4_001L,
                unlocked,
            )

        assertNull(result)
    }

    @Test
    fun repeatedContactsWithoutAngularProgressExpireTheCircleStreak() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = setOf(SlimeButtonAction.BURST)
        val firstCircle = circle()
        var now = feed(recognizer, firstCircle, 0L, unlocked)
        now =
            feed(
                recognizer,
                List(50) { firstCircle.last() },
                now,
                unlocked,
            )

        assertNull(feedForResult(recognizer, circle(), now, unlocked))
    }

    @Test
    fun partialArcsOscillationPointJitterAndJumpsDoNotUnlockHelix() {
        val unlocked = setOf(SlimeButtonAction.BURST)
        val partialArcs =
            angularTrace((0..9).map { 11.0 + it * 20.0 }) +
                angularTrace((0..9).map { 11.0 + it * 20.0 })
        val fullCircleThenPartialArc =
            circle() +
                angularTrace((0..18).map { 11.0 + it * 15.0 })
        val oscillation =
            angularTrace(
                listOf(0.0, 38.0, 4.0, 42.0, 8.0, 46.0, 12.0, 50.0)
                    .repeatValues(4),
                sector = ShellSector.FRONT,
            )
        val pointJitter =
            angularTrace(
                listOf(-2.0, 1.0, 0.0, 3.0, -1.0, 2.0).repeatValues(8),
                sector = ShellSector.FRONT,
            )
        val randomJumps =
            angularTrace(
                listOf(0.0, 95.0, 15.0, 110.0, 30.0, 125.0, 45.0, 140.0)
                    .repeatValues(3),
                sector = ShellSector.FRONT,
            )
        val largeJumps =
            angularTrace(
                listOf(0.0, 145.0, -80.0, 70.0, -150.0, 20.0).repeatValues(4),
                sector = ShellSector.FRONT,
            )

        assertNull(feedForResult(MoveUnlockRecognizer(), partialArcs, 0L, unlocked))
        assertNull(feedForResult(MoveUnlockRecognizer(), fullCircleThenPartialArc, 0L, unlocked))
        assertNull(feedForResult(MoveUnlockRecognizer(), oscillation, 0L, unlocked))
        assertNull(feedForResult(MoveUnlockRecognizer(), pointJitter, 0L, unlocked))
        assertNull(feedForResult(MoveUnlockRecognizer(), randomJumps, 0L, unlocked))
        assertNull(feedForResult(MoveUnlockRecognizer(), largeJumps, 0L, unlocked))
    }

    @Test
    fun depthDominantContactAndExplicitGesturesBreakCircleStreak() {
        val unlocked = setOf(SlimeButtonAction.BURST)
        val depthInterrupted = MoveUnlockRecognizer()
        val depthContact =
            contact(
                sector = ShellSector.FRONT,
                normalX = 0.5f,
                normalY = 0.1f,
                normalZ = 1f,
            )
        val depthResult =
            feedForResult(
                depthInterrupted,
                circle() + depthContact + circle(),
                0L,
                unlocked,
            )

        val gestureInterrupted = MoveUnlockRecognizer()
        val gestureResult =
            feedForResult(
                gestureInterrupted,
                circle() + diagonalRising() + circle(),
                0L,
                unlocked,
            )

        val buttonInterrupted = MoveUnlockRecognizer()
        var now = feed(buttonInterrupted, circle(), 0L, unlocked)
        buttonInterrupted.accept(CreatureInteraction.GlyphButtonPressed, now, unlocked)
        now += 100L
        val buttonResult = feedForResult(buttonInterrupted, circle(), now, unlocked)

        assertNull(depthResult)
        assertNull(gestureResult)
        assertNull(buttonResult)
    }

    @Test
    fun gentleCircleStrengthDoesNotCompleteDiscreteRecipes() {
        val unlocked = setOf(SlimeButtonAction.BURST)
        val gentleBounces =
            listOf(
                cardinal(ShellSector.LEFT, strength = 0.3f),
                cardinal(ShellSector.RIGHT, strength = 0.3f),
                cardinal(ShellSector.LEFT, strength = 0.3f),
                cardinal(ShellSector.RIGHT, strength = 0.3f),
            )
        val gentleDiagonals = diagonalRising(strength = 0.3f) + diagonalRising(strength = 0.3f)
        val gentleThunderbolt =
            buildList<CreatureInteraction> {
                addAll(diagonalRising(strength = 0.3f))
                addAll(diagonalFalling(strength = 0.3f))
                add(CreatureInteraction.GlyphButtonPressed)
            }

        assertNull(feedForResult(MoveUnlockRecognizer(), gentleBounces, 0L, unlocked))
        assertNull(feedForResult(MoveUnlockRecognizer(), gentleDiagonals, 0L, unlocked))
        assertNull(feedForResult(MoveUnlockRecognizer(), gentleThunderbolt, 0L, unlocked))
    }

    @Test
    fun partialRecipesExpireAndLargeAngularJumpsResetRecognition() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = setOf(SlimeButtonAction.BURST)

        var now = feed(recognizer, circle(), 0L, unlocked)
        val circleAfterTimeout = circle()
        val timeoutStart = now + 4_001L
        assertNull(feedForResult(recognizer, circleAfterTimeout, timeoutStart, unlocked))
        now = timeoutStart + circleAfterTimeout.size * 100L
        assertNull(
            recognizer.accept(
                cardinal(ShellSector.LEFT, strength = 0.2f),
                now,
                unlocked,
            ),
        )
        assertNull(feedForResult(recognizer, circle(), now + 100L, unlocked))
    }

    @Test
    fun fullTurnsAreSamplingIndependentAndRetainOvershoot() {
        for (samplesPerTurn in listOf(4, 7, 16, 60, 120, 360)) {
            val recognizer = MoveUnlockRecognizer()
            var result: SlimeButtonAction? = null
            val trace = angularTrace((0..samplesPerTurn * 2).map { 13.0 + it * 360.0 / samplesPerTurn })
            trace.forEachIndexed { index, contact ->
                val unlocked =
                    recognizer.accept(
                        contact,
                        index * 6_000L / (samplesPerTurn * 2),
                        setOf(SlimeButtonAction.BURST),
                    )
                if (index < samplesPerTurn * 2) assertNull("Premature unlock at $samplesPerTurn / $index", unlocked)
                result = unlocked ?: result
            }
            assertEquals("Sampling: $samplesPerTurn", SlimeButtonAction.HELIX, result)
        }
        val overshootingTrace = angularTrace((0..11).map { it * 65.5 })
        assertEquals(
            SlimeButtonAction.HELIX,
            feedForResult(MoveUnlockRecognizer(), overshootingTrace, 0, setOf(SlimeButtonAction.BURST)),
        )
    }

    @Test
    fun subDegreeJitterAtHighSamplingRatesDoesNotDiluteCircleDirection() {
        val trace =
            angularTrace(
                (0..720).flatMap { angle ->
                    listOf(angle.toDouble(), angle + 0.4, angle - 0.4, angle.toDouble())
                },
            )
        val recognizer = MoveUnlockRecognizer()
        var result: SlimeButtonAction? = null
        trace.forEachIndexed { index, contact ->
            result = recognizer.accept(contact, index * 2L, setOf(SlimeButtonAction.BURST)) ?: result
        }
        assertEquals(SlimeButtonAction.HELIX, result)
    }
}
