package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.ShellSector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlimePlaybackLifecycleTest {
    @Test
    fun queuedMoveKeepsRecognitionPausedUntilItsAnimationFinishes() {
        for (move in SlimeButtonAction.entries) {
            val changes = mutableListOf<Boolean>()
            val engine = SlimeEngine(onMovePlaybackChanged = { changes += it })
            val target = SlimePhenotype.DEFAULT.copy(form = CreatureForm.CAT)
            engine.setPhenotype(target, animateFormChange = true)
            assertTrue(changes.isEmpty())

            engine.handleGlyphButtonPress { move }
            assertEquals(listOf(true), changes)
            assertTrue(engine.isMovePlaying)
            assertNull(engine.snapshot().activeButtonAction)

            repeat(20) { engine.step(1f / 60f) }
            engine.setPhenotype(target.copy(widthScale = 1.08f))
            engine.handleGlyphButtonPress { error("The queued move must not restart") }
            assertEquals(listOf(true), changes)

            repeat(70) { engine.step(1f / 60f) }
            assertFalse(engine.snapshot().isFormTransitioning)
            assertEquals(move, engine.snapshot().activeButtonAction)
            assertEquals("The queued-to-active handoff must stay paused", listOf(true), changes)

            repeat(180) { engine.step(1f / 30f) }
            assertFalse(engine.isMovePlaying)
            assertEquals(listOf(true, false), changes)
        }
    }

    @Test
    fun discoveringAMovePausesImmediatelyBeforeItsFirstAnimationFrame() {
        val changes = mutableListOf<Boolean>()
        val engine =
            SlimeEngine(
                interactionHandler = { SlimeButtonAction.THUNDERBOLT },
                onMovePlaybackChanged = { changes += it },
            )

        engine.simulateInteraction(CreatureInteraction.ShellContact(ShellSector.TOP, 0.8f, 0f, -1f, 0f))

        assertEquals(listOf(true), changes)
        assertTrue(engine.isMovePlaying)
        assertNull(engine.snapshot().activeButtonAction)

        engine.step(1f / 60f)

        assertEquals(SlimeButtonAction.THUNDERBOLT, engine.snapshot().activeButtonAction)
        assertEquals(listOf(true), changes)
    }

    @Test
    fun recognitionResumesBeforePostAnimationMotionInTheSameFrame() {
        for (move in SlimeButtonAction.entries) {
            val events = mutableListOf<String>()
            var paused = false
            val engine =
                SlimeEngine(
                    interactionHandler = {
                        assertFalse("$move left recognition paused for real motion", paused)
                        events += "input"
                        null
                    },
                    onMovePlaybackChanged = {
                        paused = it
                        events += if (it) "paused" else "resumed"
                    },
                )
            engine.triggerButtonAction(move)
            val framesBeforeCompletion = ((actionDuration(move) - 0.04f) * 60).toInt()
            repeat(framesBeforeCompletion) { engine.step(1f / 60f) }
            assertEquals(listOf("paused"), events)
            assertTrue(engine.isMovePlaying)

            engine.step(0.1f)

            assertFalse(engine.isMovePlaying)
            assertTrue("$move must leave time for physical input in the final frame", events.size > 2)
            assertEquals(listOf("paused", "resumed", "input"), events.take(3))
        }
    }

    @Test
    fun snappingReleasesBothQueuedAndActiveMoves() {
        for (queued in listOf(false, true)) {
            val changes = mutableListOf<Boolean>()
            val engine = SlimeEngine(onMovePlaybackChanged = { changes += it })
            if (queued) {
                engine.setPhenotype(
                    SlimePhenotype.DEFAULT.copy(form = CreatureForm.CAT),
                    animateFormChange = true,
                )
            }
            engine.triggerButtonAction(SlimeButtonAction.BURST)
            assertEquals(listOf(true), changes)

            engine.snapToPhenotype(SlimePhenotype.DEFAULT)

            assertFalse(engine.isMovePlaying)
            assertEquals(listOf(true, false), changes)
            repeat(180) { engine.step(1f / 30f) }
            assertEquals("The cancelled move must not restart", listOf(true, false), changes)
        }
    }

    @Test
    fun changingFormReleasesAnInterruptedAnimation() {
        for (animate in listOf(false, true)) {
            val changes = mutableListOf<Boolean>()
            val engine = SlimeEngine(onMovePlaybackChanged = { changes += it })
            engine.triggerButtonAction(SlimeButtonAction.BURST)
            engine.step(0.1f)

            engine.setPhenotype(
                SlimePhenotype.DEFAULT.copy(form = CreatureForm.STAR),
                animateFormChange = animate,
            )

            assertFalse(engine.isMovePlaying)
            assertEquals(listOf(true, false), changes)
        }
    }

    @Test
    fun aStalledFrameResumesAtActualCompletionInsteadOfTheNominalDuration() {
        var nowMillis = 200L
        val changes = mutableListOf<Pair<Boolean, Long>>()
        val engine = SlimeEngine(onMovePlaybackChanged = { changes += it to nowMillis })
        engine.triggerButtonAction(SlimeButtonAction.BURST)

        nowMillis += 60_000
        engine.step(60f)

        assertTrue("Catch-up is capped, so the animation is still playing", engine.isMovePlaying)
        assertEquals(listOf(true to 200L), changes)
        var remainingFrames = 100
        while (engine.isMovePlaying && remainingFrames-- > 0) {
            nowMillis += 1_000
            engine.step(0.1f)
        }

        assertFalse(engine.isMovePlaying)
        assertTrue(nowMillis > 60_200)
        assertEquals(listOf(true to 200L, false to nowMillis), changes)
    }

    @Test
    fun cancellingFromThePlaybackCallbackDoesNotLeaveAStalePause() {
        val changes = mutableListOf<Boolean>()
        lateinit var engine: SlimeEngine
        engine =
            SlimeEngine(onMovePlaybackChanged = {
                assertEquals(it, engine.isMovePlaying)
                changes += it
                if (it) engine.snapToPhenotype(SlimePhenotype.DEFAULT)
            })

        repeat(2) {
            engine.triggerButtonAction(SlimeButtonAction.BURST)
            assertFalse(engine.isMovePlaying)
        }

        assertEquals(listOf(true, false, true, false), changes)
    }
}
