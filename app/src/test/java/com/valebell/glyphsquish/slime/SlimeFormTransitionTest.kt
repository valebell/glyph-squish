package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlimeFormTransitionTest {
    @Test
    fun formMorphIsDeterministicAndSettlesOnItsTarget() {
        val target = SlimePhenotype.DEFAULT.copy(form = CreatureForm.STAR)
        val first = SlimeEngine()
        val second = SlimeEngine()
        first.setPhenotype(target, animateFormChange = true)
        second.setPhenotype(target, animateFormChange = true)
        val signatures = mutableSetOf<Int>()

        repeat(45) {
            first.step(1f / 30f)
            second.step(1f / 30f)
            val firstFrame = first.renderFrame()
            assertArrayEquals(firstFrame, second.renderFrame())
            signatures += firstFrame.contentHashCode()
        }

        assertTrue(signatures.size >= 8)
        assertFalse(first.snapshot().isFormTransitioning)
        assertEquals(CreatureForm.STAR, first.snapshot().form)
    }

    @Test
    fun everyPairOfFormsMorphsAndSettlesCleanly() {
        CreatureForm.entries.forEach { source ->
            CreatureForm.entries
                .filterNot { target -> target == source }
                .forEach { target ->
                    val engine =
                        SlimeEngine(
                            initialPhenotype = SlimePhenotype.DEFAULT.copy(form = source),
                        )
                    engine.setPhenotype(
                        SlimePhenotype.DEFAULT.copy(form = target),
                        animateFormChange = true,
                    )

                    repeat(45) {
                        engine.step(1f / 30f)
                        assertTrue(engine.renderFrame().all { pixel -> pixel in 0..255 })
                    }

                    assertFalse("$source to $target did not settle", engine.snapshot().isFormTransitioning)
                    assertEquals(target, engine.snapshot().form)
                }
        }
    }

    @Test
    fun formMorphConvergesToTheRestingTargetWithoutAFrameSizeJump() {
        CreatureForm.entries.forEach { target ->
            val engine = SlimeEngine()
            engine.setPhenotype(
                SlimePhenotype.DEFAULT.copy(form = target),
                animateFormChange = true,
                forceReform = target == CreatureForm.BLOB,
            )

            repeat(38) {
                engine.step(1f / 30f)
            }
            val finalMorphFrame = engine.renderFrame()
            assertTrue(engine.snapshot().isFormTransitioning)

            engine.step(1f / 30f)
            val restingFrame = engine.renderFrame()
            val restingPixels = restingFrame.indices.filter { index -> restingFrame[index] > 0 }
            val overlappingPixels =
                restingPixels.count { index -> finalMorphFrame[index] > 0 }

            assertEquals(
                "$target width changed after morphing",
                litWidth(restingFrame),
                litWidth(finalMorphFrame),
            )
            assertEquals(
                "$target height changed after morphing",
                litHeight(restingFrame),
                litHeight(finalMorphFrame),
            )
            assertTrue(
                "$target did not visually converge before settling",
                overlappingPixels >= restingPixels.size * 0.9f,
            )
        }
    }

    @Test
    fun sameFormTransformationStillReassemblesParticles() {
        val engine = SlimeEngine()
        val restingFrame = engine.renderFrame()
        engine.setPhenotype(
            SlimePhenotype.DEFAULT,
            animateFormChange = true,
            forceReform = true,
        )

        repeat(18) { engine.step(1f / 30f) }

        assertTrue(engine.snapshot().isFormTransitioning)
        assertNotEquals(restingFrame.contentHashCode(), engine.renderFrame().contentHashCode())
    }

    @Test
    fun disabledFormAnimationSnapsImmediately() {
        val engine = SlimeEngine()

        engine.setPhenotype(
            SlimePhenotype.DEFAULT.copy(form = CreatureForm.SHARK),
            animateFormChange = false,
        )

        assertFalse(engine.snapshot().isFormTransitioning)
        assertEquals(CreatureForm.SHARK, engine.snapshot().form)
    }

    @Test
    fun unrelatedPhenotypeUpdateKeepsMorphAndQueuedAction() {
        val engine = SlimeEngine()
        val target = SlimePhenotype.DEFAULT.copy(form = CreatureForm.CAT)
        engine.setPhenotype(target, animateFormChange = true)
        engine.triggerButtonAction(SlimeButtonAction.HELIX)
        repeat(8) { engine.step(1f / 30f) }

        engine.setPhenotype(
            target.copy(widthScale = 1.08f),
            animateFormChange = false,
        )

        assertTrue(engine.snapshot().isFormTransitioning)
        repeat(34) { engine.step(1f / 30f) }
        assertFalse(engine.snapshot().isFormTransitioning)
        assertEquals(SlimeButtonAction.HELIX, engine.snapshot().activeButtonAction)
    }

    @Test
    fun physicalLongPressDuringMorphQueuesSelectedMoveBeforeOrAfterRelease() {
        for (move in SlimeButtonAction.entries) {
            for (releaseBeforeChange in listOf(false, true)) {
                val engine = SlimeEngine()
                engine.setPhenotype(
                    SlimePhenotype.DEFAULT.copy(form = CreatureForm.CAT),
                    animateFormChange = true,
                )
                repeat(8) { engine.step(1f / 30f) }
                engine.setPressed(true)
                if (releaseBeforeChange) engine.setPressed(false)

                engine.handleGlyphButtonPress { move }
                if (!releaseBeforeChange) engine.setPressed(false)
                engine.handleGlyphButtonPress { error("A duplicate change must not replace the queued move") }

                assertTrue("$move was discarded during the form change", engine.isMovePlaying)
                assertTrue(engine.snapshot().isFormTransitioning)
                assertFalse(engine.snapshot().isPressed)
                assertEquals(null, engine.snapshot().activeButtonAction)

                repeat(34) { engine.step(1f / 30f) }

                assertFalse(engine.snapshot().isFormTransitioning)
                assertEquals(CreatureForm.CAT, engine.snapshot().form)
                assertEquals(move, engine.snapshot().activeButtonAction)
                engine.handleGlyphButtonPress { error("A duplicate change must not restart the active move") }
                repeat(300) { engine.step(1f / 30f) }
                assertFalse("$move must finish without replaying the queued press", engine.isMovePlaying)
            }
        }
    }

    @Test
    fun snappedFormChangePreemptsAnOldBurstTarget() {
        val engine = SlimeEngine()
        engine.triggerButtonAction(SlimeButtonAction.BURST)
        repeat(15) { engine.step(1f / 30f) }
        assertEquals(SlimeButtonAction.BURST, engine.snapshot().activeButtonAction)

        engine.setPhenotype(
            SlimePhenotype.DEFAULT.copy(form = CreatureForm.BUTTERFLY),
            animateFormChange = false,
        )

        assertEquals(null, engine.snapshot().activeButtonAction)
        assertEquals(CreatureForm.BUTTERFLY, engine.snapshot().form)
        assertFalse(engine.snapshot().isFormTransitioning)
    }
}
