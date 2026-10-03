package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscreteRecognitionTest {
    @Test
    fun doubleBounceUnlocksEchoOnEitherCardinalAxis() {
        listOf(
            listOf(ShellSector.LEFT, ShellSector.RIGHT, ShellSector.LEFT, ShellSector.RIGHT),
            listOf(ShellSector.BOTTOM, ShellSector.TOP, ShellSector.BOTTOM, ShellSector.TOP),
        ).forEach { sectors ->
            val recognizer = MoveUnlockRecognizer()
            val unlocked = mutableSetOf(SlimeButtonAction.BURST)

            sectors.forEachIndexed { index, sector ->
                recognizer
                    .accept(cardinal(sector), index * 100L, unlocked)
                    ?.let(unlocked::add)
            }

            assertTrue(SlimeButtonAction.ECHO in unlocked)
        }
    }

    @Test
    fun reversingTheSecondBounceDoesNotUnlockEcho() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = mutableSetOf(SlimeButtonAction.BURST)
        val sectors =
            listOf(
                ShellSector.LEFT,
                ShellSector.RIGHT,
                ShellSector.RIGHT,
                ShellSector.LEFT,
            )

        sectors.forEachIndexed { index, sector ->
            recognizer
                .accept(cardinal(sector), index * 100L, unlocked)
                ?.let(unlocked::add)
        }

        assertTrue(SlimeButtonAction.ECHO !in unlocked)
    }

    @Test
    fun traversingTheSameDiagonalAxisTwiceUnlocksRicochet() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = mutableSetOf(SlimeButtonAction.BURST)
        val contacts =
            diagonalRising() + diagonalRising()

        contacts.forEachIndexed { index, contact ->
            recognizer
                .accept(contact, index * 100L, unlocked)
                ?.let(unlocked::add)
        }

        assertTrue(SlimeButtonAction.RICOCHET in unlocked)
    }

    @Test
    fun bounceGlyphButtonAndCircleUnlockThunderbolt() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = mutableSetOf(SlimeButtonAction.BURST)
        val interactions =
            buildList<CreatureInteraction> {
                add(cardinal(ShellSector.TOP))
                add(cardinal(ShellSector.BOTTOM))
                add(CreatureInteraction.GlyphButtonPressed)
                addAll(circle())
            }

        interactions.forEachIndexed { index, interaction ->
            recognizer
                .accept(interaction, index * 100L, unlocked)
                ?.let(unlocked::add)
        }

        assertTrue(SlimeButtonAction.THUNDERBOLT in unlocked)
    }

    @Test
    fun diagonalRecipesCannotUnlockEachOther() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = mutableSetOf(SlimeButtonAction.BURST)

        (diagonalRising() + diagonalFalling()).forEachIndexed { index, contact ->
            recognizer
                .accept(contact, index * 100L, unlocked)
                ?.let(unlocked::add)
        }

        assertTrue(SlimeButtonAction.RICOCHET !in unlocked)
        assertTrue(SlimeButtonAction.THUNDERBOLT !in unlocked)
    }

    @Test
    fun invalidInterveningInputBreaksCircleBounceAndDiagonalRecipes() {
        val unlocked = setOf(SlimeButtonAction.BURST)

        val circleRecognizer = MoveUnlockRecognizer()
        var now = feed(circleRecognizer, circle(), 0L, unlocked)
        circleRecognizer.accept(cardinal(ShellSector.FRONT), now, unlocked)
        assertNull(
            feedForResult(
                circleRecognizer,
                circle(),
                now + 100L,
                unlocked,
            ),
        )

        val bounceRecognizer = MoveUnlockRecognizer()
        now =
            feed(
                bounceRecognizer,
                listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT)),
                0L,
                unlocked,
            )
        bounceRecognizer.accept(cardinal(ShellSector.FRONT), now, unlocked)
        assertNull(
            feedForResult(
                bounceRecognizer,
                listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT)),
                now + 100L,
                unlocked,
            ),
        )

        val diagonalRecognizer = MoveUnlockRecognizer()
        now = feed(diagonalRecognizer, diagonalRising(), 0L, unlocked)
        diagonalRecognizer.accept(cardinal(ShellSector.FRONT), now, unlocked)
        assertNull(
            feedForResult(
                diagonalRecognizer,
                diagonalRising(),
                now + 100L,
                unlocked,
            ),
        )
    }

    @Test
    fun incompleteMotionBeforeGlyphButtonBreaksThunderboltRecipe() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = setOf(SlimeButtonAction.BURST)
        var now = feed(recognizer, diagonalRising(), 0L, unlocked)
        now = feed(recognizer, diagonalFalling(), now, unlocked)
        recognizer.accept(cardinal(ShellSector.LEFT), now, unlocked)

        assertNull(
            recognizer.accept(
                CreatureInteraction.GlyphButtonPressed,
                now + 100L,
                unlocked,
            ),
        )
    }

    @Test
    fun repeatedDiscreteEndpointsDoNotBecomeExtraGesturesOrEraseRecipes() {
        val bounces =
            listOf(
                ShellSector.LEFT,
                ShellSector.RIGHT,
                ShellSector.LEFT,
                ShellSector.RIGHT,
            ).map { cardinal(it) }
        for (copies in listOf(1, 2, 12)) {
            for ((contacts, expected) in listOf(
                bounces to SlimeButtonAction.ECHO,
                (diagonalRising() + diagonalRising()) to SlimeButtonAction.RICOCHET,
            )) {
                assertEquals(
                    expected,
                    feedForResult(
                        MoveUnlockRecognizer(),
                        contacts.flatMap { c ->
                            List(copies) { c }
                        },
                        0,
                        setOf(SlimeButtonAction.BURST),
                    ),
                )
            }
        }
    }

    @Test
    fun partialTurnThenOppositeJumpCannotBeReinterpretedAsBounce() {
        val recognizer = MoveUnlockRecognizer()
        val trace = angularTrace(listOf(0.0, 30.0, 60.0, 90.0, 270.0))
        feed(recognizer, trace, 0, setOf(SlimeButtonAction.BURST))
        assertEquals(
            0,
            recognizer.progress.recipes
                .getValue(SlimeButtonAction.ECHO)
                .completedSteps,
        )
        assertEquals(RecognitionReset.DIRECTION_OR_JUMP, recognizer.progress.resetReason)
    }

    @Test
    fun holdingABounceEndpointCannotKeepTheRecipeAliveForever() {
        val recognizer = MoveUnlockRecognizer()
        val unlocked = setOf(SlimeButtonAction.BURST)
        var now = feed(recognizer, listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT)), 0, unlocked)
        now = feed(recognizer, List(50) { cardinal(ShellSector.RIGHT) }, now, unlocked)
        assertNull(
            feedForResult(recognizer, listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT)), now, unlocked),
        )
    }

    @Test
    fun glyphButtonPreservesAValidThunderboltPrefix() {
        val session = CreatureSession(CreatureProgression.initialState())
        session.handle(cardinal(ShellSector.LEFT), 0)
        session.handle(cardinal(ShellSector.RIGHT), 100)
        session.handle(CreatureInteraction.GlyphButtonPressed, 200)
        assertEquals(
            2,
            session.recognition.recipes
                .getValue(SlimeButtonAction.THUNDERBOLT)
                .completedSteps,
        )
        assertNull(session.recognition.resetReason)
    }
}
