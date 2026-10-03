package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.ShellSector
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlimeMoveAnimationTest {
    @Test
    fun physicalButtonAndContactsCannotOverlapQueuedOrActiveMoves() {
        for (move in SlimeButtonAction.entries) {
            var contacts = 0
            val engine =
                SlimeEngine(interactionHandler = {
                    contacts++
                    if (it is CreatureInteraction.ShellContact) move else null
                })
            val contact = CreatureInteraction.ShellContact(ShellSector.LEFT, 0.8f, -1f, 0f, 0f)
            val position = CreatureInteraction.CirclePosition(1f, 0f, 0f)
            engine.simulateInteraction(contact)
            engine.simulateInteraction(contact)
            engine.simulateInteraction(position)
            engine.handleGlyphButtonPress { error("Queued action must block the button") }
            assertEquals(1, contacts)
            engine.step(1f / 30f)
            engine.simulateInteraction(contact)
            engine.simulateInteraction(position)
            engine.handleGlyphButtonPress { error("Active action must block the button") }
            assertEquals(move, engine.snapshot().activeButtonAction)
            assertEquals(1, contacts)
            repeat(300) { engine.step(1f / 30f) }
            assertEquals(null, engine.snapshot().activeButtonAction)
            engine.handleGlyphButtonPress { SlimeButtonAction.BURST }
            assertEquals(SlimeButtonAction.BURST, engine.snapshot().activeButtonAction)
        }
    }

    @Test
    fun completeAnimationsAndTheirExitMotionDoNotEmitRecipeContacts() {
        for (move in SlimeButtonAction.entries) {
            var contacts = 0
            lateinit var engine: SlimeEngine
            engine =
                SlimeEngine(interactionHandler = {
                    assertTrue("Animation-generated input from $move", !engine.isMovePlaying)
                    if (it is CreatureInteraction.ShellContact) contacts++
                    null
                })
            engine.triggerButtonAction(move)
            repeat(450) { engine.step(1f / 30f) }
            assertEquals("Animation-generated contacts from $move", 0, contacts)
        }
    }

    @Test
    fun consumedGlyphButtonDoesNotStartAnAnimation() {
        val engine = SlimeEngine()
        engine.setPressed(true)
        engine.handleGlyphButtonPress { null }
        engine.step(1f / 30f)
        assertEquals(null, engine.snapshot().activeButtonAction)
    }

    @Test
    fun fiveButtonActionsProduceDistinctAnimatedReactions() {
        val signatures =
            SlimeButtonAction.entries.map { action ->
                val engine = SlimeEngine()
                engine.setTilt(0.35f, -0.2f, 0.15f)
                engine.triggerButtonAction(action)
                buildList {
                    repeat(42) { frameIndex ->
                        engine.step(1f / 30f)
                        if (frameIndex % 6 == 5) {
                            add(engine.renderFrame().contentHashCode())
                        }
                    }
                }
            }

        assertEquals(5, SlimeButtonAction.entries.size)
        assertEquals(5, signatures.distinct().size)
    }

    @Test
    fun thunderboltZigzagsAcrossBothDiagonalsAndStaysBounded() {
        val engine = SlimeEngine()
        engine.triggerButtonAction(SlimeButtonAction.THUNDERBOLT)
        val positions = mutableListOf(engine.snapshot())
        var completedState: SlimeSnapshot? = null

        repeat(150) {
            engine.step(1f / 60f)
            val state = engine.snapshot()
            positions += state
            if (completedState == null && state.activeButtonAction == null) {
                completedState = state
            }
            val frame = engine.renderFrame()
            assertEquals(SlimeEngine.PIXEL_COUNT, frame.size)
            assertTrue(frame.all { pixel -> pixel in 0..255 })
            frame.forEachIndexed { index, pixel ->
                if (pixel == 0) return@forEachIndexed
                val x = index % SlimeEngine.MATRIX_SIZE
                val y = index / SlimeEngine.MATRIX_SIZE
                val deltaX = x - SlimeEngine.MATRIX_SIZE / 2
                val deltaY = y - SlimeEngine.MATRIX_SIZE / 2
                assertTrue(deltaX * deltaX + deltaY * deltaY <= 156)
            }
        }

        val diagonalProducts =
            positions.zipWithNext { first, second ->
                (second.x - first.x) * (second.y - first.y)
            }
        assertTrue(diagonalProducts.any { product -> product > 0.001f })
        assertTrue(diagonalProducts.any { product -> product < -0.001f })
        assertTrue(
            positions.all { state ->
                state.x * state.x + state.y * state.y + state.z * state.z <= 49.01f
            },
        )
        assertEquals(null, engine.snapshot().activeButtonAction)
        val completed = checkNotNull(completedState) { "Thunderbolt must complete within 150 frames" }
        assertEquals(0f, completed.x, 0.001f)
        assertEquals(0f, completed.y, 0.001f)
        assertEquals(0f, completed.z, 0.001f)
    }

    @Test
    fun thunderboltZigzagsThenExplodesFlashesAndRestoresSelectedForm() {
        val engine = engineFor(CreatureForm.STAR)
        val initialFrame = engine.renderFrame()
        val validGlyphIndices = validGlyphIndices()
        engine.triggerButtonAction(SlimeButtonAction.THUNDERBOLT)

        val samples =
            List(THUNDERBOLT_TEST_MAX_STEPS) {
                engine.step(ACTION_TEST_STEP_SECONDS)
                engine.snapshot() to engine.renderFrame()
            }
        val diagonals =
            samples.zipWithNext { previous, next ->
                (next.first.x - previous.first.x) * (next.first.y - previous.first.y)
            }
        val positive = diagonals.indexOfFirst { it > 0.001f }
        val negative = diagonals.indexOfFirst { it < -0.001f }
        assertTrue("Thunderbolt never zigzagged on both diagonals", positive >= 0 && negative >= 0)
        val particles =
            samples.indexOfFirst { (_, frame) ->
                val lit = frame.filter { it > 0 }
                lit.size in 1 until validGlyphIndices.size && lit.distinct().size == 1
            }
        val rim =
            samples.indexOfFirst { (_, frame) ->
                frame.indices.count { frame[it] > 0 && isGlyphRimIndex(it) } >= MIN_THUNDERBOLT_RIM_PIXELS &&
                    frame.count { it > 0 } < validGlyphIndices.size
            }
        val flash = samples.indexOfFirst { (_, frame) -> validGlyphIndices.all { frame[it] == BODY_HIGHLIGHT } }
        val restored =
            samples.indexOfFirst { (state, frame) ->
                state.activeButtonAction != null && frame.contentEquals(initialFrame)
            }
        assertTrue("Zigzags must precede breakup", particles > positive && particles > negative)
        assertTrue("Particles must reach the rim before the flash", rim > particles && flash > rim)
        assertTrue("The original form must return after the flash", restored > flash)
        assertArrayEquals(initialFrame, samples[restored].second)
        samples[flash].second.indices.filterNot { it in validGlyphIndices }.forEach {
            assertEquals("Outside the Glyph circle", 0, samples[flash].second[it])
        }
        assertEquals(CreatureForm.STAR, engine.snapshot().form)
        assertEquals(null, engine.snapshot().activeButtonAction)
    }

    @Test
    fun intactButtonActionsTravelFromBackToFront() {
        SlimeButtonAction.entries
            .filterNot { action -> action == SlimeButtonAction.BURST }
            .forEach { action ->
                val engine = SlimeEngine()
                engine.setTilt(0.4f, -0.3f, 0.5f)
                engine.setPhoneAcceleration(0.2f, -0.15f, 0.25f)
                engine.triggerButtonAction(action)
                var minimumDepth = 0f
                var maximumDepth = 0f

                repeat(45) {
                    engine.step(1f / 30f)
                    val depth = engine.snapshot().z
                    minimumDepth = minOf(minimumDepth, depth)
                    maximumDepth = maxOf(maximumDepth, depth)
                }

                assertTrue("$action did not travel into the back", minimumDepth < -2.5f)
                assertTrue("$action did not return through the front", maximumDepth > 2f)
            }
    }

    @Test
    fun burstCompletesPreservesSelectedFormAndCanReplay() {
        CreatureForm.entries.forEach { form ->
            val engine = engineFor(form)
            repeat(2) {
                engine.triggerButtonAction(SlimeButtonAction.BURST)
                assertEquals(SlimeButtonAction.BURST, engine.snapshot().activeButtonAction)
                var steps = 0
                while (engine.snapshot().activeButtonAction != null && steps < 90) {
                    engine.step(1f / 30f)
                    steps++
                }
                val state = engine.snapshot()
                assertEquals("Burst did not finish for $form", null, state.activeButtonAction)
                assertEquals(form, state.form)
                assertEquals(0f, state.x, 0.001f)
                assertEquals(0f, state.y, 0.001f)
                assertEquals(0f, state.z, 0.001f)
                assertTrue("$form disappeared after Burst", engine.renderFrame().any { pixel -> pixel > 0 })
            }
        }
    }

    @Test
    fun directionalActionsLatchDirectionAtTrigger() {
        listOf(
            SlimeButtonAction.HELIX,
            SlimeButtonAction.RICOCHET,
        ).forEach { action ->
            val stableTilt = SlimeEngine()
            val changingTilt = SlimeEngine()
            stableTilt.setTilt(1f, 0f)
            changingTilt.setTilt(1f, 0f)
            stableTilt.triggerButtonAction(action)
            changingTilt.triggerButtonAction(action)

            repeat(30) {
                changingTilt.setTilt(-1f, 0f)
                stableTilt.step(1f / 30f)
                changingTilt.step(1f / 30f)
            }

            assertEquals(stableTilt.snapshot().x, changingTilt.snapshot().x, 0.0001f)
            assertEquals(stableTilt.snapshot().y, changingTilt.snapshot().y, 0.0001f)
            assertEquals(stableTilt.snapshot().z, changingTilt.snapshot().z, 0.0001f)
        }
    }

    @Test
    fun unlockedMoveReturnedByInteractionQueuesItsAction() {
        var contactCount = 0
        val engine =
            SlimeEngine(
                interactionHandler = { interaction ->
                    if (interaction is CreatureInteraction.ShellContact) {
                        contactCount += 1
                    }
                    if (contactCount == 3) SlimeButtonAction.HELIX else null
                },
            )

        repeat(3) {
            engine.simulateInteraction(
                CreatureInteraction.ShellContact(
                    sector = ShellSector.LEFT,
                    strength = 0.5f,
                    normalX = -1f,
                    normalY = 0f,
                    normalZ = 0f,
                ),
            )
        }
        engine.step(1f / 30f)

        assertEquals(SlimeButtonAction.HELIX, engine.snapshot().activeButtonAction)
    }

    @Test
    fun previewingAButtonActionDoesNotContributeUnlockInput() {
        var interactionCount = 0
        val engine =
            SlimeEngine(
                interactionHandler = {
                    interactionCount += 1
                    null
                },
            )

        engine.triggerButtonAction(SlimeButtonAction.THUNDERBOLT)
        repeat(10) { engine.step(1f / 30f) }

        assertEquals(0, interactionCount)
    }
}
