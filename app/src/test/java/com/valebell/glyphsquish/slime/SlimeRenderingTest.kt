package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlimeRenderingTest {
    @Test
    fun frameAlwaysFitsPhoneThreeMatrix() {
        val engine = SlimeEngine()

        repeat(240) {
            driveFrame(engine, it)
            engine.step(1f / 12f)
            val frame = engine.renderFrame()

            assertFrameFitsMatrix(frame)
        }
    }

    @Test
    fun largerFormsKeepAReadableCoreAcrossTheMovementEnvelope() {
        val directions =
            listOf(
                Triple(1f, 0f, 0f),
                Triple(0f, 1f, 0f),
                Triple(INVERSE_SQRT_TWO, INVERSE_SQRT_TWO, 0f),
                Triple(-INVERSE_SQRT_TWO, INVERSE_SQRT_TWO, 0.6f),
                Triple(0f, 0f, -1f),
            )

        CreatureForm.entries.forEach { form ->
            directions.forEach { (x, y, z) ->
                val engine = engineFor(form)
                engine.setTilt(x, y, z)
                repeat(12) {
                    engine.step(1f / 12f)
                }

                assertTrue(
                    "$form lost its core while moving toward ($x, $y, $z)",
                    largestBrightComponent(engine.renderFrame()) >= MIN_READABLE_CORE_PIXELS,
                )
            }
        }
    }

    @Test
    fun appearanceUsesFixedRequestedScales() {
        assertEquals(1f, SlimeEngine.SHAPE_SCALE)
        assertEquals(1f, SlimeEngine.EYE_SCALE)
    }

    @Test
    fun restingFormsUseABrightCompactGrayscaleRamp() {
        CreatureForm.entries.forEach { form ->
            val litValues =
                engineFor(form)
                    .renderFrame()
                    .filter { pixel -> pixel > 0 }
                    .toSet()

            assertTrue("$form has a muddy body value", litValues.all(BODY_RAMP::contains))
            assertTrue("$form has no white highlight", BODY_HIGHLIGHT in litValues)
            assertTrue("$form has no shaded volume", litValues.size >= 2)
        }
    }

    @Test
    fun restingFormsKeepTwoCompactCutoutEyes() {
        EXPECTED_BODY_PIXELS.forEach { (form, bodyPixelCount) ->
            val frame = engineFor(form).renderFrame()
            val eyeSpacing = if (form == CreatureForm.SHARK || form == CreatureForm.BUTTERFLY) 1 else 2
            val eyeTop = SlimeEngine.MATRIX_SIZE / 2 - 1

            assertEquals(bodyPixelCount - RESTING_EYE_PIXELS, frame.count { it > 0 })
            listOf(
                SlimeEngine.MATRIX_SIZE / 2 - eyeSpacing,
                SlimeEngine.MATRIX_SIZE / 2 + eyeSpacing,
            ).forEach { eyeX ->
                assertEquals(0, frame[eyeTop * SlimeEngine.MATRIX_SIZE + eyeX])
                assertEquals(0, frame[(eyeTop + 1) * SlimeEngine.MATRIX_SIZE + eyeX])
                assertTrue(frame[(eyeTop - 1) * SlimeEngine.MATRIX_SIZE + eyeX] > 0)
                assertTrue(frame[(eyeTop + 2) * SlimeEngine.MATRIX_SIZE + eyeX] > 0)
            }
        }
    }

    @Test
    fun orbUsesBrightnessForVolumeAndTrail() {
        val engine = SlimeEngine()
        engine.setTilt(1f, 0f)
        repeat(12) { engine.step(1f / 12f) }

        val litValues =
            engine
                .renderFrame()
                .filter { pixel -> pixel > 0 }
                .distinct()

        assertTrue(litValues.size >= 4)
        assertTrue(litValues.any { pixel -> pixel in 1 until BODY_SHADOW })
        assertTrue(litValues.any { pixel -> pixel >= BODY_MIDTONE })
    }

    @Test
    fun sameInputsProduceSameFrame() {
        val first = SlimeEngine()
        val second = SlimeEngine()

        repeat(30) {
            first.setTilt(0.7f, -0.4f, 0.35f)
            second.setTilt(0.7f, -0.4f, 0.35f)
            first.setPhoneAcceleration(-0.2f, 0.15f, 0.3f)
            second.setPhoneAcceleration(-0.2f, 0.15f, 0.3f)
            first.step(1f / 12f)
            second.step(1f / 12f)
        }

        assertArrayEquals(first.renderFrame(), second.renderFrame())
    }

    @Test
    fun phenotypeProducesAStableDistinctAppearance() {
        val defaultEngine = SlimeEngine()
        val wideEngine =
            SlimeEngine(
                initialPhenotype =
                    SlimePhenotype.DEFAULT.copy(
                        widthScale = 1.2f,
                        heightScale = 0.9f,
                    ),
            )

        assertNotEquals(
            defaultEngine.renderFrame().contentHashCode(),
            wideEngine.renderFrame().contentHashCode(),
        )
        assertArrayEquals(wideEngine.renderFrame(), wideEngine.renderFrame())
    }

    @Test
    fun everyCatalogFormProducesADistinctFrame() {
        val signatures =
            CreatureForm.entries.map { form ->
                SlimeEngine(
                    initialPhenotype = SlimePhenotype.DEFAULT.copy(form = form),
                ).renderFrame().contentHashCode()
            }

        assertEquals(CreatureForm.entries.size, signatures.distinct().size)
    }
}

private fun driveFrame(
    engine: SlimeEngine,
    step: Int,
) {
    if (step > 0 && step % 40 == 0) {
        val action = SlimeButtonAction.entries[(step / 40) % SlimeButtonAction.entries.size]
        engine.triggerButtonAction(action)
    }
    if (step % 50 == 0) engine.setPressed(true)
    if (step % 50 == 8) engine.setPressed(false)
    driveMotion(engine, step)
}

private fun driveMotion(
    engine: SlimeEngine,
    step: Int,
) {
    engine.setTilt(
        x = if (step % 2 == 0) 1f else -1f,
        y = if (step % 3 == 0) 1f else -1f,
        z = if (step % 5 < 2) 1f else -1f,
    )
    engine.setPhoneAcceleration(
        x = if (step % 7 < 2) 0.7f else -0.2f,
        y = if (step % 11 < 3) -0.6f else 0.1f,
        z = if (step % 13 < 4) 0.8f else -0.15f,
    )
}

private fun assertFrameFitsMatrix(frame: IntArray) {
    assertEquals(SlimeEngine.PIXEL_COUNT, frame.size)
    assertTrue(frame.any { pixel -> pixel > 0 })
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
