package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SlimeIntegrationTest {
    @Test
    fun delayedFramesMatchSmallStepsIncludingCollisions() {
        for (form in CreatureForm.entries) {
            val fast = SlimeEngine(SlimePhenotype.DEFAULT.copy(form = form))
            val slow = SlimeEngine(SlimePhenotype.DEFAULT.copy(form = form))
            fast.setTilt(0.8f, -0.6f, 0.3f)
            slow.setTilt(0.8f, -0.6f, 0.3f)
            repeat(60) {
                slow.step(0.1f)
                repeat(12) { fast.step(0.1f / 12) }
                assertEquals("$form position", fast.snapshot().x, slow.snapshot().x, 0.002f)
                assertEquals("$form position", fast.snapshot().y, slow.snapshot().y, 0.002f)
                assertEquals("$form depth", fast.snapshot().z, slow.snapshot().z, 0.002f)
                assertEquals("$form velocity", fast.snapshot().velocityX, slow.snapshot().velocityX, 0.002f)
            }
        }
    }

    @Test
    fun invalidTimeAndSensorValuesCannotPoisonSimulation() {
        val engine = SlimeEngine()
        val initial = engine.snapshot()
        for (delta in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f, 0f)) {
            engine.step(delta)
            assertEquals(initial, engine.snapshot())
        }
        engine.setTilt(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        engine.setPhoneAcceleration(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val reference = SlimeEngine()
        repeat(120) {
            engine.step(1f / 30)
            reference.step(1f / 30)
        }
        assertEquals(reference.snapshot(), engine.snapshot())
        assertArrayEquals(reference.renderFrame(), engine.renderFrame())
    }

    @Test
    fun normalReflectionPreservesTangentialMotionAndLosesEnergy() {
        val scene = SlimeScene(SlimePhenotype.DEFAULT, true) { null }
        scene.positionX = 7f
        scene.velocityX = 10f
        scene.velocityY = 3f
        val before = scene.speed()
        SlimeMotion(scene).step(0.0001f)
        val radius =
            sqrt(
                scene.positionX * scene.positionX + scene.positionY * scene.positionY +
                    scene.positionZ * scene.positionZ,
            )
        assertEquals(7f, radius, 0.0001f)
        assertTrue(scene.velocityX < 0f)
        assertEquals(3f, scene.velocityY, 0.002f)
        assertTrue(scene.speed() < before)
    }

    @Test
    fun aLongPauseOnlyAdvancesBoundedCatchUp() {
        val paused = SlimeEngine()
        val bounded = SlimeEngine()
        paused.setTilt(1f, 0f)
        bounded.setTilt(1f, 0f)
        paused.step(60f)
        bounded.step(0.1f)
        assertEquals(bounded.snapshot(), paused.snapshot())
    }
}
