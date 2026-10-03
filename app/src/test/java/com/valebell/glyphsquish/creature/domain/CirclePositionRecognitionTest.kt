package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CirclePositionRecognitionTest {
    @Test
    fun exactlyTwoTurnsUnlockWithLongInteriorArcsInEitherDirection() {
        for (firstDirection in listOf(-1, 1)) {
            for (secondDirection in listOf(-1, 1)) {
                for (interiorRadius in listOf(0.46f, 0.6f)) {
                    val recognizer = MoveUnlockRecognizer()
                    val positions =
                        turn(firstDirection, interiorRadius = interiorRadius) +
                            turn(secondDirection, firstDirection * 360.0, interiorRadius).drop(1)
                    positions.forEachIndexed { index, position ->
                        val result = recognizer.accept(position, index * 20L)
                        if (index < positions.lastIndex) {
                            assertNull("Completed before two turns at sample $index", result)
                        } else {
                            assertEquals(SlimeButtonAction.HELIX, result)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun centerCrossingDiscardsTheCompletedCircleStreak() {
        val recognizer = MoveUnlockRecognizer()
        assertNull(resultAfter(recognizer, turn()))
        assertEquals(1, recognizer.completedSteps(SlimeButtonAction.HELIX))

        recognizer.accept(CreatureInteraction.CirclePosition(0f, 0f, 0f), 1_500)

        assertEquals(0, recognizer.completedSteps(SlimeButtonAction.HELIX))
        assertNull(resultAfter(recognizer, turn(), 1_600))
        assertEquals(1, recognizer.completedSteps(SlimeButtonAction.HELIX))
    }

    @Test
    fun circleMustStartInTheOuterRegion() {
        for (radius in listOf(0.05f, 0.6f, 0.69f)) {
            val recognizer = MoveUnlockRecognizer()
            val positions = (0..144).map { position(it * 5.0, radius) }
            assertNull(resultAfter(recognizer, positions))
            assertEquals(0, recognizer.completedSteps(SlimeButtonAction.HELIX))
        }
    }

    @Test
    fun duplicateBoundaryAndPositionSamplesDoNotCompleteEarly() {
        val recognizer = MoveUnlockRecognizer()
        val recognized = mutableListOf<SlimeButtonAction>()
        for (step in 0..144) {
            val angle = step * 5.0
            val contact = angularTrace(listOf(angle)).single().copy(hasReliablePosition = true)
            for (event in listOf(contact, position(angle), position(angle))) {
                val result = recognizer.accept(event, step * 20L)
                if (step < 144) assertNull("Duplicate counted at $angle degrees", result)
                result?.let(recognized::add)
            }
        }
        assertEquals(listOf(SlimeButtonAction.HELIX), recognized)
    }

    @Test
    fun continuousCenterCrossingsPreserveActualBounceAndDiagonalImpacts() {
        val bounces = listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT))
        for ((endpoints, expected) in listOf(
            bounces to SlimeButtonAction.ECHO,
            diagonalRising() to SlimeButtonAction.RICOCHET,
        )) {
            val contacts = endpoints + endpoints
            val events =
                buildList<CreatureInteraction> {
                    add(contacts.first())
                    contacts.zipWithNext().forEach { (from, to) ->
                        addAll(crossingPositions(from, to))
                        add(to)
                    }
                }
            assertEquals(expected, resultAfter(MoveUnlockRecognizer(), events))
        }
    }

    @Test
    fun continuousCenterPositionsDoNotKeepAnImpactEndpointAlive() {
        val bounces = listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT))
        for ((endpoints, move) in listOf(
            bounces to SlimeButtonAction.ECHO,
            diagonalRising() to SlimeButtonAction.RICOCHET,
        )) {
            val recognizer = MoveUnlockRecognizer()
            recognizer.accept(endpoints.first(), 0)
            for (time in 20L..10_000L step 20) {
                recognizer.accept(CreatureInteraction.CirclePosition(0f, 0f, 0f), time)
            }

            assertNull(recognizer.accept(endpoints.last(), 10_001))
            assertEquals("Stale endpoint completed $move", 0, recognizer.completedSteps(move))
        }
    }

    @Test
    fun expiringAnImpactEndpointDoesNotDiscardAContinuouslyProgressingCircle() {
        val recognizer = MoveUnlockRecognizer()
        recognizer.accept(cardinal(ShellSector.RIGHT), 0)
        // The first 45 degrees take 6.3 seconds, but every 5-degree checkpoint remains fresh.
        for (step in 1..72) {
            assertNull(recognizer.accept(position(step * 5.0), step * 700L))
        }

        assertEquals(1, recognizer.completedSteps(SlimeButtonAction.HELIX))
    }

    @Test
    fun centerRepositioningPreservesThunderboltPrefixForAnInteriorCircle() {
        val recognizer = MoveUnlockRecognizer()
        val prefix =
            listOf(
                cardinal(ShellSector.LEFT),
                cardinal(ShellSector.RIGHT),
                CreatureInteraction.GlyphButtonPressed,
                CreatureInteraction.CirclePosition(0f, 0f, 0f),
            )
        resultAfter(recognizer, prefix)
        assertEquals(2, recognizer.completedSteps(SlimeButtonAction.THUNDERBOLT))

        assertEquals(SlimeButtonAction.THUNDERBOLT, resultAfter(recognizer, turn(), 100))
    }

    @Test
    fun stationaryPositionsCannotKeepACompletedCircleAlive() {
        val recognizer = MoveUnlockRecognizer()
        resultAfter(recognizer, turn())
        val stationary = List(220) { position(360.0) }
        assertNull(resultAfter(recognizer, stationary, 1_460))

        assertEquals(RecognitionReset.TIMED_OUT, recognizer.progress.resetReason)
        assertEquals(0, recognizer.completedSteps(SlimeButtonAction.HELIX))
        assertNull(resultAfter(recognizer, turn(), 5_900))
    }

    @Test
    fun repositioningBeforeTheButtonPreservesThunderboltBounce() {
        val recognizer = MoveUnlockRecognizer()
        resultAfter(
            recognizer,
            listOf(
                cardinal(ShellSector.LEFT),
                cardinal(ShellSector.RIGHT),
                CreatureInteraction.CirclePosition(0f, 0f, 0f),
                position(90.0, 0.8f),
                position(100.0, 0.8f),
                CreatureInteraction.GlyphButtonPressed,
            ),
        )
        assertEquals(2, recognizer.completedSteps(SlimeButtonAction.THUNDERBOLT))
        assertEquals(
            SlimeButtonAction.THUNDERBOLT,
            resultAfter(recognizer, turn(startDegrees = 100.0), 200),
        )
    }

    @Test
    fun depthAndNonFinitePositionsBreakCircularContinuity() {
        val invalidPositions =
            listOf(
                CreatureInteraction.CirclePosition(0.8f, 0f, 0.9f),
                CreatureInteraction.CirclePosition(Float.NaN, 0f, 0f),
                CreatureInteraction.CirclePosition(1f, Float.POSITIVE_INFINITY, 0f),
                CreatureInteraction.CirclePosition(1f, 0f, Float.NaN),
            )
        for (invalid in invalidPositions) {
            val recognizer = MoveUnlockRecognizer()
            resultAfter(recognizer, turn())
            recognizer.accept(invalid, 1_500)

            assertEquals(0, recognizer.completedSteps(SlimeButtonAction.HELIX))
            assertNull(resultAfter(recognizer, turn(), 1_600))
        }
    }

    @Test
    fun centerCrossingCannotBeHiddenBySparseOuterEndpoints() {
        val recognizer = MoveUnlockRecognizer()
        resultAfter(recognizer, turn())
        recognizer.accept(position(0.0, 0.7f), 1_480)
        // A 120-degree chord passes through radius 0.35 despite two valid outer endpoints.
        recognizer.accept(position(120.0, 0.7f), 1_500)

        assertEquals(0, recognizer.completedSteps(SlimeButtonAction.HELIX))
        assertNull(resultAfter(recognizer, turn(startDegrees = 120.0), 1_600))
    }

    @Test
    fun shakeSuppressionAlsoBlocksContinuousPositionGestures() {
        val session = CreatureSession(CreatureProgression.initialState())
        turn().forEachIndexed { index, position -> session.handle(position, index * 20L) }
        session.resetRecognition(2_000, suppressContactsForMillis = 900)
        val shakePositions = turn() + turn().drop(1)
        shakePositions.forEachIndexed { index, position -> session.handle(position, 2_000 + index * 5L) }

        assertEquals(RecognitionReset.INTERRUPTED, session.recognition.resetReason)
        assertEquals(
            0,
            session.recognition.recipes
                .getValue(SlimeButtonAction.HELIX)
                .completedSteps,
        )
        turn().forEachIndexed { index, position -> session.handle(position, 2_900 + index * 20L) }
        assertEquals(
            1,
            session.recognition.recipes
                .getValue(SlimeButtonAction.HELIX)
                .completedSteps,
        )
        assertTrue(SlimeButtonAction.HELIX !in session.state.unlockedMoves)
    }

    private fun turn(
        direction: Int = 1,
        startDegrees: Double = 0.0,
        interiorRadius: Float = 0.6f,
    ): List<CreatureInteraction.CirclePosition> =
        (0..72).map { step ->
            // A 240-degree arc has no shell contact; both turns still cover exactly 360 degrees.
            position(startDegrees + direction * step * 5.0, if (step in 12..60) interiorRadius else 1f)
        }

    private fun position(
        degrees: Double,
        radius: Float = 1f,
    ): CreatureInteraction.CirclePosition {
        val angle = Math.toRadians(degrees)
        return CreatureInteraction.CirclePosition(radius * cos(angle).toFloat(), radius * sin(angle).toFloat(), 0f)
    }

    private fun crossingPositions(
        from: CreatureInteraction.ShellContact,
        to: CreatureInteraction.ShellContact,
    ): List<CreatureInteraction.CirclePosition> =
        (1..4).map { step ->
            val progress = step / 4f
            CreatureInteraction.CirclePosition(
                from.normalX + (to.normalX - from.normalX) * progress,
                from.normalY + (to.normalY - from.normalY) * progress,
                0f,
            )
        }

    private fun resultAfter(
        recognizer: MoveUnlockRecognizer,
        events: List<CreatureInteraction>,
        startMillis: Long = 0,
    ): SlimeButtonAction? {
        var result: SlimeButtonAction? = null
        events.forEachIndexed { index, event ->
            result = recognizer.accept(event, startMillis + index * 20L) ?: result
        }
        return result
    }

    private fun MoveUnlockRecognizer.completedSteps(move: SlimeButtonAction): Int =
        progress.recipes.getValue(move).completedSteps
}
