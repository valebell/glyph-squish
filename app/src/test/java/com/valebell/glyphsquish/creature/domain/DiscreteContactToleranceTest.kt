package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscreteContactToleranceTest {
    @Test
    fun settlingAtFirmEndpointsPreservesBounceAndDiagonalRecipes() {
        val recipes =
            mapOf(
                SlimeButtonAction.ECHO to listOf(cardinal(ShellSector.LEFT), cardinal(ShellSector.RIGHT)),
                SlimeButtonAction.RICOCHET to diagonalRising(),
            )
        for ((move, endpoints) in recipes) {
            val contacts =
                (endpoints + endpoints).flatMap { impact ->
                    listOf(impact) + List(5) { impact.copy(strength = 0.01f, hasReliablePosition = true) }
                }
            assertEquals(move, feedForResult(MoveUnlockRecognizer(), contacts, 0, setOf(SlimeButtonAction.BURST)))
        }
    }

    @Test
    fun settlingContactsCannotKeepTheFirstEndpointAliveForever() {
        val recognizer = MoveUnlockRecognizer()
        val left = cardinal(ShellSector.LEFT)
        val contacts =
            listOf(left) +
                List(45) { left.copy(strength = 0.01f, hasReliablePosition = true) } +
                cardinal(ShellSector.RIGHT) + CreatureInteraction.GlyphButtonPressed + circle()

        assertNull(feedForResult(recognizer, contacts, 0, setOf(SlimeButtonAction.BURST)))
    }
}
