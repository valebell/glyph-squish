package com.valebell.glyphsquish.slime

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class SlimeOrbitMoves(
    private val scene: SlimeScene,
) {
    fun helix() = scene.applyHelixAnimation()

    fun echo() = scene.applyEchoAnimation()

    fun ricochet() = scene.applyRicochetAnimation()

    private fun SlimeScene.applyHelixAnimation() {
        val time = buttonActionElapsed
        val direction = buttonActionDirection
        when {
            time < HELIX_COIL_END -> {
                setActionPhase(0, SlimeExpression.FOCUSED)
                val progress = smoothSegment(time, 0f, HELIX_COIL_END)
                val angle = HELIX_START_ANGLE * direction
                setActionPosition(
                    lerp(actionOriginX, cos(angle) * HELIX_COIL_RADIUS, progress),
                    lerp(actionOriginY, sin(angle) * HELIX_COIL_RADIUS, progress),
                    lerp(actionOriginZ, HELIX_COIL_DEPTH, progress),
                )
            }

            time < HELIX_REAR_END -> {
                setActionPhase(1, SlimeExpression.DIZZY)
                val progress =
                    smoothSegment(time, HELIX_COIL_END, HELIX_REAR_END)
                val angle =
                    lerp(
                        HELIX_START_ANGLE,
                        PI.toFloat(),
                        progress,
                    ) * direction
                val radius = lerp(HELIX_COIL_RADIUS, HELIX_REAR_RADIUS, progress)
                setActionPosition(
                    cos(angle) * radius,
                    sin(angle) * radius,
                    lerp(HELIX_COIL_DEPTH, HELIX_REAR_DEPTH, progress),
                )
            }

            time < HELIX_FRONT_END -> {
                setActionPhase(2, SlimeExpression.DIZZY)
                val progress =
                    easeOutCubic(
                        segmentProgress(time, HELIX_REAR_END, HELIX_FRONT_END),
                    )
                val angle = lerp(PI.toFloat(), TWO_PI, progress) * direction
                val radius = lerp(HELIX_REAR_RADIUS, HELIX_FRONT_RADIUS, progress)
                setActionPosition(
                    cos(angle) * radius,
                    sin(angle) * radius,
                    lerp(HELIX_REAR_DEPTH, HELIX_FRONT_DEPTH, progress),
                )
            }

            time < HELIX_SLING_END -> {
                setActionPhase(3, SlimeExpression.SURPRISED)
                val progress =
                    smoothSegment(time, HELIX_FRONT_END, HELIX_SLING_END)
                val angle = lerp(TWO_PI, HELIX_SLING_ANGLE, progress) * direction
                val radius = lerp(HELIX_FRONT_RADIUS, HELIX_SLING_RADIUS, progress)
                setActionPosition(
                    cos(angle) * radius,
                    sin(angle) * radius,
                    lerp(HELIX_FRONT_DEPTH, HELIX_SLING_DEPTH, progress),
                )
            }

            else -> {
                setActionPhase(4, SlimeExpression.HAPPY)
                val progress =
                    smoothSegment(time, HELIX_SLING_END, HELIX_ACTION_SECONDS)
                setActionPosition(
                    lerp(
                        cos(HELIX_SLING_ANGLE * direction) * HELIX_SLING_RADIUS,
                        0f,
                        progress,
                    ),
                    lerp(
                        sin(HELIX_SLING_ANGLE * direction) * HELIX_SLING_RADIUS,
                        0f,
                        progress,
                    ),
                    lerp(HELIX_SLING_DEPTH, 0f, progress),
                )
            }
        }
    }

    private fun SlimeScene.applyEchoAnimation() {
        val time = buttonActionElapsed
        when {
            time < ECHO_CROUCH_END -> {
                setActionPhase(0, SlimeExpression.FOCUSED)
                val progress = smoothSegment(time, 0f, ECHO_CROUCH_END)
                setActionPosition(
                    lerp(actionOriginX, actionOriginX * 0.5f, progress),
                    lerp(actionOriginY, actionOriginY * 0.5f, progress),
                    lerp(actionOriginZ, ECHO_CROUCH_DEPTH, progress),
                )
            }

            time < ECHO_REAR_END -> {
                setActionPhase(1, SlimeExpression.SURPRISED)
                val progress =
                    smoothSegment(time, ECHO_CROUCH_END, ECHO_REAR_END)
                setActionPosition(
                    lerp(actionOriginX * 0.5f, 0f, progress),
                    lerp(actionOriginY * 0.5f, 0f, progress),
                    lerp(ECHO_CROUCH_DEPTH, ECHO_REAR_DEPTH, progress),
                )
            }

            time < ECHO_REAR_HOLD_END -> {
                setActionPhase(2, SlimeExpression.FOCUSED)
                setActionPosition(0f, 0f, ECHO_REAR_DEPTH)
            }

            time < ECHO_RETURN_END -> {
                setActionPhase(3, SlimeExpression.SURPRISED)
                val progress =
                    smoothSegment(time, ECHO_REAR_HOLD_END, ECHO_RETURN_END)
                setActionPosition(
                    0f,
                    0f,
                    lerp(ECHO_REAR_DEPTH, ECHO_FRONT_DEPTH, progress),
                )
            }

            time < ECHO_SURF_END -> {
                setActionPhase(4, SlimeExpression.HAPPY)
                val progress =
                    smoothSegment(time, ECHO_RETURN_END, ECHO_SURF_END)
                setActionPosition(
                    sin(progress * PI.toFloat()) * ECHO_SURF_X,
                    -sin(progress * PI.toFloat()) * ECHO_SURF_Y,
                    lerp(ECHO_FRONT_DEPTH, ECHO_SURF_DEPTH, progress),
                )
            }

            else -> {
                setActionPhase(5, SlimeExpression.HAPPY)
                val progress =
                    smoothSegment(time, ECHO_SURF_END, ECHO_ACTION_SECONDS)
                setActionPosition(
                    0f,
                    0f,
                    lerp(ECHO_SURF_DEPTH, 0f, progress),
                )
            }
        }
    }

    private fun SlimeScene.applyRicochetAnimation() {
        val time = buttonActionElapsed
        val direction = buttonActionDirection
        when {
            time < RICOCHET_LOAD_END -> {
                setActionPhase(0, SlimeExpression.FOCUSED)
                val progress = smoothSegment(time, 0f, RICOCHET_LOAD_END)
                setActionPosition(
                    lerp(actionOriginX, -RICOCHET_LOAD_X * direction, progress),
                    lerp(actionOriginY, RICOCHET_LOAD_Y, progress),
                    lerp(actionOriginZ, RICOCHET_LOAD_DEPTH, progress),
                )
            }

            time < RICOCHET_FAR_FLIGHT_END -> {
                setActionPhase(1, SlimeExpression.SURPRISED)
                val progress =
                    easeInCubic(
                        segmentProgress(time, RICOCHET_LOAD_END, RICOCHET_FAR_FLIGHT_END),
                    )
                setActionPosition(
                    lerp(-RICOCHET_LOAD_X, RICOCHET_WALL_X, progress) * direction,
                    lerp(RICOCHET_LOAD_Y, -RICOCHET_WALL_Y, progress),
                    lerp(RICOCHET_LOAD_DEPTH, RICOCHET_FAR_DEPTH, progress),
                )
            }

            time < RICOCHET_FAR_IMPACT_END -> {
                if (setActionPhase(2, SlimeExpression.HAPPY)) {
                    markActionImpact(direction, -1f, -1f)
                }
                setActionPosition(RICOCHET_WALL_X * direction, -RICOCHET_WALL_Y, RICOCHET_FAR_DEPTH)
            }

            time < RICOCHET_NEAR_FLIGHT_END -> {
                setActionPhase(3, SlimeExpression.SURPRISED)
                val progress =
                    smoothSegment(time, RICOCHET_FAR_IMPACT_END, RICOCHET_NEAR_FLIGHT_END)
                setActionPosition(
                    lerp(RICOCHET_WALL_X, -RICOCHET_WALL_X, progress) * direction,
                    lerp(-RICOCHET_WALL_Y, RICOCHET_WALL_Y, progress),
                    lerp(RICOCHET_FAR_DEPTH, RICOCHET_NEAR_DEPTH, progress),
                )
            }

            time < RICOCHET_NEAR_IMPACT_END -> {
                if (setActionPhase(4, SlimeExpression.HAPPY)) {
                    markActionImpact(-direction, 1f, 1f)
                }
                setActionPosition(-RICOCHET_WALL_X * direction, RICOCHET_WALL_Y, RICOCHET_NEAR_DEPTH)
            }

            else -> {
                setActionPhase(5, SlimeExpression.HAPPY)
                val progress =
                    smoothSegment(time, RICOCHET_NEAR_IMPACT_END, RICOCHET_ACTION_SECONDS)
                setActionPosition(
                    lerp(-RICOCHET_WALL_X * direction, 0f, progress),
                    lerp(RICOCHET_WALL_Y, 0f, progress),
                    lerp(RICOCHET_NEAR_DEPTH, 0f, progress),
                )
            }
        }
    }
}

private const val HELIX_COIL_END = 0.17f
private const val HELIX_REAR_END = 0.5f
private const val HELIX_FRONT_END = 0.77f
private const val HELIX_SLING_END = 0.93f
private const val HELIX_START_ANGLE = -0.5f
private const val HELIX_COIL_RADIUS = 1.2f
private const val HELIX_COIL_DEPTH = -0.8f
private const val HELIX_REAR_RADIUS = 3.8f
private const val HELIX_REAR_DEPTH = -4.3f
private const val HELIX_FRONT_RADIUS = 3f
private const val HELIX_FRONT_DEPTH = 3.7f
private const val HELIX_SLING_ANGLE = 7.853982f
private const val HELIX_SLING_RADIUS = 0.5f
private const val HELIX_SLING_DEPTH = 4.7f
private const val ECHO_SURF_END = 0.93f
private const val ECHO_SURF_X = 1f
private const val ECHO_SURF_Y = 0.6f
private const val RICOCHET_LOAD_END = 0.17f
private const val RICOCHET_FAR_FLIGHT_END = 0.4f
private const val RICOCHET_FAR_IMPACT_END = 0.5f
private const val RICOCHET_NEAR_FLIGHT_END = 0.77f
private const val RICOCHET_NEAR_IMPACT_END = 0.87f
private const val RICOCHET_LOAD_X = 1f
private const val RICOCHET_LOAD_Y = 1f
private const val RICOCHET_LOAD_DEPTH = 0.6f
private const val RICOCHET_WALL_X = 3.5f
private const val RICOCHET_WALL_Y = 3.5f
private const val RICOCHET_FAR_DEPTH = -3.5f
private const val RICOCHET_NEAR_DEPTH = 3.5f
