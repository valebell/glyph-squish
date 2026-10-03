package com.valebell.glyphsquish.ui.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.slime.SlimePhenotype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlimeMatrixPlaybackTest {
    @Test
    fun playbackStateFollowsEveryMoveUntilTheEngineCompletes() {
        SlimeButtonAction.entries.forEach { move ->
            val state = newState()
            assertFalse(state.isMovePlaying)

            state.triggerButtonAction(move)

            assertTrue("$move should immediately be playing", state.isMovePlaying)
            repeat(100) {
                state.step(0.1f)
                assertEquals(
                    "$move playback must follow the engine",
                    state.snapshot().activeButtonAction != null,
                    state.isMovePlaying,
                )
            }
            assertFalse("$move should finish", state.isMovePlaying)
        }
    }

    @Test
    fun aQueuedMoveStaysBusyThroughoutTheFormTransitionAndPlayback() {
        val state = newState()
        state.setPhenotype(SlimePhenotype.DEFAULT.copy(form = CreatureForm.CAT), animateFormChange = true)
        assertFalse("A form change alone is not move playback", state.isMovePlaying)

        state.triggerButtonAction(SlimeButtonAction.HELIX)

        assertNull(state.snapshot().activeButtonAction)
        assertTrue(state.isMovePlaying)
        repeat(10) {
            state.step(0.1f)
            assertTrue(state.snapshot().isFormTransitioning)
            assertTrue("Queued playback must not briefly become ready", state.isMovePlaying)
        }
        repeat(5) { state.step(0.1f) }
        assertEquals(SlimeButtonAction.HELIX, state.snapshot().activeButtonAction)
        assertTrue(state.isMovePlaying)

        repeat(100) { state.step(0.1f) }

        assertFalse(state.isMovePlaying)
    }

    @Test
    fun changingFormCancelsThePlayingStatusImmediately() {
        val state = newState()
        state.triggerButtonAction(SlimeButtonAction.BURST)
        state.step(0.1f)

        state.setPhenotype(SlimePhenotype.DEFAULT.copy(form = CreatureForm.STAR), animateFormChange = true)

        assertNull(state.snapshot().activeButtonAction)
        assertFalse(state.isMovePlaying)
    }

    @Test
    fun interactionCleanupDoesNotEndAnUnfinishedMove() {
        val state = newState()
        state.triggerButtonAction(SlimeButtonAction.ECHO)

        state.cancelInteraction()

        assertTrue(state.isMovePlaying)
        repeat(100) { state.step(0.1f) }
        assertFalse(state.isMovePlaying)
    }

    private fun newState() = SlimeMatrixState(SlimePhenotype.DEFAULT) { null }
}
