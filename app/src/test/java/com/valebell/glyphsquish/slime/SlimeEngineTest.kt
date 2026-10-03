package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.ShellSector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class SlimeEngineTest {
    @Test
    fun tiltMovesOrbInRequestedDirection() {
        val engine = SlimeEngine()
        engine.setTilt(1f, 0f)

        repeat(24) {
            engine.step(1f / 12f)
        }

        assertTrue(engine.snapshot().x > 2f)
    }

    @Test
    fun strongTiltCarriesOrbToShellQuickly() {
        val engine = SlimeEngine()
        engine.setTilt(1f, 0f)

        repeat(6) {
            engine.step(1f / 12f)
        }

        assertTrue(engine.snapshot().x > 5.2f)
    }

    @Test
    fun phoneMovementThrowsOrbAgainstTheMovement() {
        val engine = SlimeEngine()
        engine.setPhoneAcceleration(1f, 0f, 0f)

        repeat(4) {
            engine.step(1f / 12f)
        }

        assertTrue(engine.snapshot().x < -2f)
    }

    @Test
    fun sharkIsFastestOnHorizontalAndVerticalAxes() {
        listOf(1f to 0f, 0f to 1f).forEach { (x, y) ->
            val snapshots =
                CreatureForm.entries.associateWith { form ->
                    earlyPhoneMovement(form, x, y)
                }
            val sharkAxisSpeed = axisSpeed(snapshots.getValue(CreatureForm.SHARK), x, y)
            val fastestOther =
                snapshots
                    .filterKeys { form -> form != CreatureForm.SHARK }
                    .values
                    .maxOf { snapshot -> axisSpeed(snapshot, x, y) }

            assertTrue("shark should lead on the $x,$y axis", sharkAxisSpeed > fastestOther * 1.2f)
        }
    }

    @Test
    fun starSuppressesCrossAxisDriftForNearAxisInput() {
        val star = earlyPhoneMovement(CreatureForm.STAR, 1f, 0.18f)
        val blob = earlyPhoneMovement(CreatureForm.BLOB, 1f, 0.18f)
        val starDriftRatio = abs(star.velocityY / star.velocityX)
        val blobDriftRatio = abs(blob.velocityY / blob.velocityX)

        assertTrue(starDriftRatio < blobDriftRatio * 0.5f)
        assertTrue("star must retain cross-axis control", abs(star.velocityY) > 0.01f)
    }

    @Test
    fun butterflyRespondsBestOnDiagonals() {
        val diagonalX = INVERSE_SQRT_TWO
        val diagonalY = INVERSE_SQRT_TWO
        val butterflyDiagonal =
            planarSpeed(
                earlyPhoneMovement(CreatureForm.BUTTERFLY, diagonalX, diagonalY),
            )
        val butterflyAxis = planarSpeed(earlyPhoneMovement(CreatureForm.BUTTERFLY, 1f, 0f))
        val blobDiagonal =
            planarSpeed(
                earlyPhoneMovement(CreatureForm.BLOB, diagonalX, diagonalY),
            )
        val catDiagonal =
            planarSpeed(
                earlyPhoneMovement(CreatureForm.CAT, diagonalX, diagonalY),
            )

        assertTrue(butterflyDiagonal > butterflyAxis * 1.45f)
        assertTrue(butterflyDiagonal > blobDiagonal * 1.3f)
        assertTrue(butterflyDiagonal > catDiagonal * 1.25f)
    }

    @Test
    fun blobAndCatAreDirectionallyBalancedGeneralists() {
        listOf(CreatureForm.BLOB, CreatureForm.CAT).forEach { form ->
            val axis = planarSpeed(earlyPhoneMovement(form, 1f, 0f))
            val diagonal =
                planarSpeed(
                    earlyPhoneMovement(
                        form,
                        INVERSE_SQRT_TWO,
                        INVERSE_SQRT_TWO,
                    ),
                )

            assertTrue("$form should balance axis and diagonal response", abs(axis - diagonal) < axis * 0.03f)
        }
    }

    @Test
    fun movingPhoneForwardSendsOrbBackThroughDepth() {
        val engine = SlimeEngine()
        val centeredBrightPixels = engine.renderFrame().count { pixel -> pixel >= 150 }
        engine.setPhoneAcceleration(0f, 0f, 1f)

        repeat(4) {
            engine.step(1f / 12f)
        }

        assertTrue(engine.snapshot().z < -2f)
        assertTrue(
            engine.renderFrame().count { pixel -> pixel >= 150 } <
                centeredBrightPixels,
        )
    }

    @Test
    fun threeDimensionalBoundaryContainsOrbCenter() {
        val engine = SlimeEngine()
        engine.setTilt(1f, 1f, 1f)
        engine.setPhoneAcceleration(-1f, -1f, -1f)

        repeat(400) {
            engine.step(1f / 12f)
        }

        val state = engine.snapshot()
        val distance = sqrt(state.x * state.x + state.y * state.y + state.z * state.z)
        assertTrue(distance <= 7.01f)
    }

    @Test
    fun everyFormRemainsControllableAndBoundedInEveryPlanarDirection() {
        val directions =
            listOf(
                1f to 0f,
                INVERSE_SQRT_TWO to INVERSE_SQRT_TWO,
                0f to 1f,
                -INVERSE_SQRT_TWO to INVERSE_SQRT_TWO,
                -1f to 0f,
                -INVERSE_SQRT_TWO to -INVERSE_SQRT_TWO,
                0f to -1f,
                INVERSE_SQRT_TWO to -INVERSE_SQRT_TWO,
            )
        CreatureForm.entries.forEach { form ->
            directions.forEach { (x, y) ->
                val early = earlyTiltMovement(form, x, y)
                assertTrue(
                    "$form should remain controllable toward $x,$y",
                    early.x * x + early.y * y > 0f,
                )
            }

            val engine = engineFor(form)
            repeat(480) { step ->
                val (x, y) = directions[(step / 60) % directions.size]
                engine.setTilt(x, y, if (step % 120 < 60) 1f else -1f)
                engine.setPhoneAcceleration(-x, -y, if (step % 90 < 45) -1f else 1f)
                engine.step(1f / 12f)
                val state = engine.snapshot()
                val distance = sqrt(state.x * state.x + state.y * state.y + state.z * state.z)
                assertTrue("$form escaped the spherical boundary", distance <= 7.01f)
            }
        }
    }

    @Test
    fun orbStartsCenteredAndOnlyFloatsNearItsRestingPoint() {
        val engine = SlimeEngine()
        val initialState = engine.snapshot()

        assertEquals(0f, initialState.x)
        assertEquals(0f, initialState.y)
        assertEquals(0f, initialState.z)

        repeat(120) {
            engine.step(1f / 12f)
        }

        val state = engine.snapshot()
        val distance = sqrt(state.x * state.x + state.y * state.y + state.z * state.z)
        assertTrue(distance < 1f)
    }

    @Test
    fun movementCreatesTrailParticles() {
        val engine = SlimeEngine()
        engine.setPhoneAcceleration(0.8f, 0.35f, 0.2f)

        repeat(8) {
            engine.step(1f / 12f)
        }

        assertTrue(engine.snapshot().trailCount > 0)
    }

    @Test
    fun orbFloatsBackTowardCenterAfterPhoneStops() {
        val engine = SlimeEngine()
        engine.setPhoneAcceleration(1f, 0.4f, -0.3f)
        repeat(4) { engine.step(1f / 12f) }
        engine.setPhoneAcceleration(0f, 0f, 0f)

        repeat(120) {
            engine.step(1f / 12f)
        }

        val state = engine.snapshot()
        val distance = sqrt(state.x * state.x + state.y * state.y + state.z * state.z)
        assertTrue(distance < 1f)
    }

    @Test
    fun strongCollisionChangesExpressionToHappy() {
        val engine = SlimeEngine()
        engine.setTilt(1f, 0f)

        repeat(8) {
            engine.step(1f / 12f)
        }

        assertEquals(SlimeExpression.HAPPY, engine.snapshot().expression)
    }

    @Test
    fun holdChangesPoseAndReleaseAddsHopAndDepthPop() {
        val engine = SlimeEngine()
        val restingFrame = engine.renderFrame()
        engine.setPressed(true)
        repeat(6) { engine.step(1f / 12f) }
        val heldFrame = engine.renderFrame()
        engine.setPressed(false)

        assertNotEquals(restingFrame.contentHashCode(), heldFrame.contentHashCode())
        assertTrue(engine.snapshot().velocityY < 0f)
        assertTrue(engine.snapshot().velocityZ > 0f)
    }

    @Test
    fun boundaryCollisionEmitsCappedSemanticContact() {
        val interactions = mutableListOf<CreatureInteraction>()
        val engine =
            SlimeEngine(
                interactionHandler = { interaction ->
                    interactions += interaction
                    null
                },
            )
        engine.setTilt(1f, 0f)

        repeat(12) {
            engine.step(1f / 12f)
        }

        val contacts = interactions.filterIsInstance<CreatureInteraction.ShellContact>()
        assertTrue(contacts.isNotEmpty())
        assertTrue(contacts.all { contact -> contact.strength in 0f..1f })
        assertTrue(contacts.any { contact -> contact.sector == ShellSector.RIGHT })
    }
}
