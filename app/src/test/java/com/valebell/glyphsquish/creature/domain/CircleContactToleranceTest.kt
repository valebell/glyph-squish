package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CircleContactToleranceTest {
    private val unlocked = setOf(SlimeButtonAction.BURST)

    @Test
    fun reliableBoundaryPositionsTrackCirclesWithoutImpactStrength() {
        for (clockwise in listOf(false, true)) {
            val trace = circle(clockwise = clockwise, strength = 0f).map { it.copy(hasReliablePosition = true) }

            assertEquals(
                SlimeButtonAction.HELIX,
                feedForResult(MoveUnlockRecognizer(), trace + trace, 0L, unlocked),
            )
        }
    }

    @Test
    fun reliablePositionDoesNotReplaceDiscreteImpactStrength() {
        val bounces = listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT))
        for (endpoints in listOf(bounces, diagonalRising())) {
            val trace = (endpoints + endpoints).map { it.copy(strength = 0.01f, hasReliablePosition = true) }
            assertNull(feedForResult(MoveUnlockRecognizer(), trace, 0L, unlocked))
        }
    }

    @Test
    fun reliablePositionsStillRejectDepthAndNonFiniteNormals() {
        val trace = circle(strength = 0.01f).map { it.copy(hasReliablePosition = true) }
        val contact = trace.first()
        val invalidPositions =
            listOf(
                contact.copy(normalZ = 1f),
                contact.copy(normalX = Float.NaN),
                contact.copy(normalY = Float.POSITIVE_INFINITY),
            )
        for (invalid in invalidPositions) {
            assertNull(feedForResult(MoveUnlockRecognizer(), trace + invalid + trace, 0L, unlocked))
        }
    }

    @Test
    fun stationaryReliableContactsStillExpireCircleProgress() {
        val recognizer = MoveUnlockRecognizer()
        val trace = circle(strength = 0.01f).map { it.copy(hasReliablePosition = true) }
        var now = feed(recognizer, trace, 0L, unlocked)
        now = feed(recognizer, List(45) { trace.last() }, now, unlocked)

        assertEquals(RecognitionReset.TIMED_OUT, recognizer.progress.resetReason)
        assertNull(feedForResult(recognizer, trace, now, unlocked))
    }

    @Test
    fun lightContactsDuringEitherCircleDirectionPreserveHelixProgress() {
        for (clockwise in listOf(false, true)) {
            val recognizer = MoveUnlockRecognizer()
            val trace = circle(clockwise = clockwise).withLightContacts()

            assertEquals(
                SlimeButtonAction.HELIX,
                feedForResult(recognizer, trace + trace, 0L, unlocked),
            )
        }
    }

    @Test
    fun lightContactsBeforeAndDuringCirclePreserveThunderboltPrefix() {
        val recognizer = MoveUnlockRecognizer()
        val prefix =
            listOf(
                cardinal(ShellSector.LEFT),
                cardinal(ShellSector.RIGHT),
                CreatureInteraction.GlyphButtonPressed,
                cardinal(ShellSector.LEFT, strength = 0.01f),
            )
        val now = feed(recognizer, prefix, 0L, unlocked)

        assertEquals(
            2,
            recognizer.progress.recipes
                .getValue(SlimeButtonAction.THUNDERBOLT)
                .completedSteps,
        )
        assertEquals(
            SlimeButtonAction.THUNDERBOLT,
            feedForResult(recognizer, circle().withLightContacts(), now, unlocked),
        )
    }

    @Test
    fun returningAfterBrieflyLeavingTheBorderPreservesPartialCircles() {
        val recognizer = MoveUnlockRecognizer()
        val trace = circle() + circle()
        val beforeGap = trace.take(8)
        val now = feed(recognizer, beforeGap, 0L, unlocked)

        assertEquals(
            SlimeButtonAction.HELIX,
            feedForResult(recognizer, trace.drop(beforeGap.size), now + 1_000L, unlocked),
        )
    }

    @Test
    fun lightContactsDoNotContributeAngularTravel() {
        val recognizer = MoveUnlockRecognizer()
        var now = feed(recognizer, circle(), 0L, unlocked)
        assertNull(feedForResult(recognizer, circle(strength = 0.01f), now, unlocked))
        now += circle().size * 100L
        assertNull(recognizer.accept(circle().first(), now, unlocked))
        assertEquals(
            1,
            recognizer.progress.recipes
                .getValue(SlimeButtonAction.HELIX)
                .completedSteps,
        )

        assertEquals(SlimeButtonAction.HELIX, feedForResult(recognizer, circle(), now + 100L, unlocked))
    }

    @Test
    fun repeatedLightContactsCannotKeepCircleProgressAlive() {
        val recognizer = MoveUnlockRecognizer()
        var now = feed(recognizer, circle(), 0L, unlocked)
        now = feed(recognizer, List(45) { cardinal(ShellSector.RIGHT, strength = 0.01f) }, now, unlocked)

        assertEquals(RecognitionReset.TIMED_OUT, recognizer.progress.resetReason)
        assertEquals(
            0,
            recognizer.progress.recipes
                .getValue(SlimeButtonAction.HELIX)
                .completedSteps,
        )
        assertNull(feedForResult(recognizer, circle(), now, unlocked))
    }

    @Test
    fun lightContactsStillInterruptDiscreteGestures() {
        for ((endpoints, move) in listOf(
            listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT)) to SlimeButtonAction.ECHO,
            diagonalRising() to SlimeButtonAction.RICOCHET,
        )) {
            val recognizer = MoveUnlockRecognizer()
            val interrupted = listOf(endpoints.first(), endpoints.first().copy(strength = 0.01f), endpoints.last())
            feed(recognizer, interrupted, 0L, unlocked)

            assertEquals(
                0,
                recognizer.progress.recipes
                    .getValue(move)
                    .completedSteps,
            )
        }
    }

    private fun List<CreatureInteraction.ShellContact>.withLightContacts(): List<CreatureInteraction.ShellContact> =
        flatMapIndexed { index, contact ->
            if (index % 4 == 0) {
                listOf(
                    contact,
                    // A light contact's angle is unreliable and must not add travel or cause a jump.
                    contact.copy(strength = 0.01f, normalX = -contact.normalX, normalY = -contact.normalY),
                    contact.copy(strength = 0f),
                )
            } else {
                listOf(contact)
            }
        }
}
