package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatureProgressionTest {
    @Test
    fun recognitionReportsRepeatedMoveButGrantsOnlyItsFirstUnlock() {
        val session = CreatureSession(CreatureProgression.initialState())
        var now = 0L
        repeat(3) { performance ->
            val before = session.state
            val outcomes = (circle() + circle()).map { session.handle(it, now.also { now += 100 }) }
            assertEquals(listOf(SlimeButtonAction.HELIX), outcomes.mapNotNull { it.recognizedMove })
            if (performance == 0) {
                assertEquals(listOf(SlimeButtonAction.HELIX), outcomes.mapNotNull { it.unlockedMove })
                session.replace(CreatureProgression.selectMove(session.state, SlimeButtonAction.BURST))
            } else {
                assertEquals(emptyList<SlimeButtonAction>(), outcomes.mapNotNull { it.unlockedMove })
                assertEquals(before, session.state)
            }
        }
    }

    @Test
    fun seenFormsOnlyIncludeUnlockedFormsAndDoNotChangeSelection() {
        val initial = CreatureProgression.initialState()
        val unlocked = CreatureProgression.unlockMove(initial, SlimeButtonAction.HELIX)
        assertEquals(setOf(CreatureForm.BLOB), unlocked.seenForms)
        val seen = CreatureProgression.markFormsSeen(unlocked, setOf(CreatureForm.BUTTERFLY, CreatureForm.STAR))
        assertEquals(setOf(CreatureForm.BLOB, CreatureForm.BUTTERFLY), seen.seenForms)
        assertEquals(unlocked.currentForm, seen.currentForm)
        assertEquals(unlocked.selectedMove, seen.selectedMove)
        assertEquals(seen, CreatureProgression.markFormsSeen(seen, setOf(CreatureForm.BUTTERFLY)))
    }

    @Test
    fun selectingNewFormAcknowledgesOnlyThatFormAndIsIdempotent() {
        val unlocked =
            CreatureProgression.unlockMove(
                CreatureProgression.unlockMove(CreatureProgression.initialState(), SlimeButtonAction.HELIX),
                SlimeButtonAction.THUNDERBOLT,
            )
        val selected = CreatureProgression.selectForm(unlocked, CreatureForm.BUTTERFLY)
        assertEquals(setOf(CreatureForm.BLOB, CreatureForm.BUTTERFLY), selected.seenForms)
        assertEquals(unlocked.revision + 1, selected.revision)
        assertEquals(selected, CreatureProgression.selectForm(selected, CreatureForm.BUTTERFLY))
        val legacy = unlocked.copy(currentForm = CreatureForm.BUTTERFLY)
        assertTrue(CreatureForm.BUTTERFLY in CreatureProgression.selectForm(legacy, CreatureForm.BUTTERFLY).seenForms)
    }

    @Test
    fun initialStateContainsOnlyBlobAndBurst() {
        val state = CreatureProgression.initialState()

        assertEquals(CreatureForm.BLOB, state.currentForm)
        assertEquals(SlimeButtonAction.BURST, state.selectedMove)
        assertEquals(setOf(SlimeButtonAction.BURST), state.unlockedMoves)
        assertEquals(setOf(CreatureForm.BLOB), CreatureForms.availableFor(state.unlockedMoves))
    }

    @Test
    fun moveUnlocksSelectTheMoveAndRevealItsPairedForm() {
        val expectedForms =
            mapOf(
                SlimeButtonAction.RICOCHET to CreatureForm.SHARK,
                SlimeButtonAction.ECHO to CreatureForm.CAT,
                SlimeButtonAction.THUNDERBOLT to CreatureForm.STAR,
                SlimeButtonAction.HELIX to CreatureForm.BUTTERFLY,
            )

        expectedForms.forEach { (move, form) ->
            val state =
                CreatureProgression.unlockMove(
                    CreatureProgression.initialState(),
                    move,
                )

            assertEquals(move, state.selectedMove)
            assertTrue(form in CreatureForms.availableFor(state.unlockedMoves))
            assertEquals(move, CreatureForms.requiredMove(form))
        }
    }

    @Test
    fun lockedFormsAndMovesCannotBeSelected() {
        val state = CreatureProgression.initialState()

        assertThrows(IllegalArgumentException::class.java) {
            CreatureProgression.selectForm(state, CreatureForm.SHARK)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatureProgression.selectMove(state, SlimeButtonAction.RICOCHET)
        }
    }

    @Test
    fun unlockedFormsCycleInCatalogOrder() {
        var state = CreatureProgression.initialState()
        state = CreatureProgression.unlockMove(state, SlimeButtonAction.RICOCHET)
        state = CreatureProgression.unlockMove(state, SlimeButtonAction.ECHO)
        state = CreatureProgression.unlockMove(state, SlimeButtonAction.THUNDERBOLT)
        state = CreatureProgression.unlockMove(state, SlimeButtonAction.HELIX)

        val visited = mutableListOf<CreatureForm>()
        var current = state.currentForm
        repeat(CreatureForm.entries.size) {
            visited += current
            current = CreatureForms.nextUnlocked(current, state.unlockedMoves)
        }

        assertEquals(CreatureForm.entries, visited)
        assertEquals(CreatureForm.BLOB, current)
    }

    @Test
    fun sessionPersistsOnlyCompletedUnlocks() {
        val session = CreatureSession(CreatureProgression.initialState())
        var outcome = InteractionOutcome(session.state, unlockedMove = null)
        var unlockedMove: SlimeButtonAction? = null

        (circle() + circle()).forEachIndexed { index, interaction ->
            outcome = session.handle(interaction, index * 100L)
            unlockedMove = outcome.unlockedMove ?: unlockedMove
        }

        assertEquals(SlimeButtonAction.HELIX, unlockedMove)
        assertEquals(SlimeButtonAction.HELIX, session.state.selectedMove)
        assertEquals(CreatureForm.BLOB, session.state.currentForm)
        assertTrue(CreatureForm.BUTTERFLY in CreatureForms.availableFor(session.state.unlockedMoves))
    }
}
