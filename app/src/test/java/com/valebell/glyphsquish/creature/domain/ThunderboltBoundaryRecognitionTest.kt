package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ThunderboltBoundaryRecognitionTest {
    @Test
    fun reliableBoundaryReversalAndPositionReversalBothAllowAFreshThunderboltCircle() {
        for (boundaryStrength in listOf(null, 0.01f, 0.8f)) {
            val recognizer = MoveUnlockRecognizer()
            var now = 0L

            fun accept(interaction: CreatureInteraction): SlimeButtonAction? {
                now += 20
                return recognizer.accept(interaction, now)
            }

            accept(cardinal(ShellSector.LEFT))
            accept(cardinal(ShellSector.RIGHT))
            accept(CreatureInteraction.GlyphButtonPressed)
            // Begin an arc, then reposition by reversing a little more than 90 degrees.
            // Physics emits weak reliable contacts while sliding along the boundary.
            (0..18).forEach { accept(position(it * 5.0)) }
            (17 downTo 0).forEach { accept(position(it * 5.0)) }
            val reversal = position(-5.0)
            accept(
                if (boundaryStrength != null) {
                    CreatureInteraction.ShellContact(
                        ShellSector.RIGHT,
                        strength = boundaryStrength,
                        normalX = reversal.x,
                        normalY = reversal.y,
                        normalZ = reversal.z,
                        hasReliablePosition = true,
                    )
                } else {
                    reversal
                },
            )

            assertEquals(
                "Armed prefix was lost with boundaryStrength=${boundaryStrength ?: "position only"}",
                2,
                recognizer.progress.recipes
                    .getValue(SlimeButtonAction.THUNDERBOLT)
                    .completedSteps,
            )
            (0..72).forEach { step ->
                val recognized = accept(position(-5.0 - step * 5.0))
                if (step < 72) {
                    assertNull("Reversed travel must not contribute to the fresh circle", recognized)
                } else {
                    assertEquals(SlimeButtonAction.THUNDERBOLT, recognized)
                }
            }
        }
    }

    private fun position(degrees: Double): CreatureInteraction.CirclePosition {
        val radians = Math.toRadians(degrees)
        return CreatureInteraction.CirclePosition(cos(radians).toFloat(), sin(radians).toFloat(), 0f)
    }
}
