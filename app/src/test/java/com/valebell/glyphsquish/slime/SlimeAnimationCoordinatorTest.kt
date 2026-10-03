package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.ShellSector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlimeAnimationCoordinatorTest {
    @Test
    fun shellContactProducesBoundedFadingPixels() {
        val coordinator = SlimeAnimationCoordinator()
        coordinator.onShellContact(ShellSector.LEFT)

        assertTrue(coordinator.renderPixels().isNotEmpty())
        repeat(20) {
            coordinator.renderPixels().forEach { pixel ->
                assertTrue(pixel.x in 0 until SlimeEngine.MATRIX_SIZE)
                assertTrue(pixel.y in 0 until SlimeEngine.MATRIX_SIZE)
                assertTrue(pixel.brightness in 0..255)
            }
            coordinator.step(1f / 30f)
        }
        assertEquals(0, coordinator.snapshot().sparkCount)
    }

    @Test
    fun invitationCompletesWithoutPersistentState() {
        val coordinator = SlimeAnimationCoordinator()
        coordinator.trigger(SlimeAnimationCue.INVITATION)

        repeat(60) {
            coordinator.step(1f / 30f)
        }

        assertNull(coordinator.snapshot().activeCue)
    }
}
