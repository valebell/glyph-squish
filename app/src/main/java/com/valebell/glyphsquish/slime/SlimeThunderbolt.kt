package com.valebell.glyphsquish.slime

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal class SlimeThunderbolt(
    private val scene: SlimeScene,
) {
    fun step() = scene.applyThunderboltAnimation()

    fun prepare() = scene.prepareThunderboltParticles()

    fun render(frame: IntArray) = scene.renderThunderboltParticles(frame)

    private fun SlimeScene.applyThunderboltAnimation() {
        val time = buttonActionElapsed
        val direction = buttonActionDirection
        when {
            time < THUNDERBOLT_CHARGE_END -> {
                setActionPhase(0, SlimeExpression.FOCUSED)
                val progress =
                    smoothSegment(time, 0f, THUNDERBOLT_CHARGE_END)
                setActionPosition(
                    lerp(actionOriginX, -THUNDERBOLT_CHARGE_X * direction, progress),
                    lerp(actionOriginY, 0f, progress),
                    lerp(actionOriginZ, THUNDERBOLT_CHARGE_DEPTH, progress),
                )
            }

            time < THUNDERBOLT_ZIG_ONE_END -> {
                setActionPhase(1, SlimeExpression.SURPRISED)
                val progress =
                    smoothSegment(time, THUNDERBOLT_CHARGE_END, THUNDERBOLT_ZIG_ONE_END)
                setActionPosition(
                    lerp(-THUNDERBOLT_CHARGE_X, 0f, progress) * direction,
                    lerp(0f, THUNDERBOLT_ZIG_Y, progress),
                    lerp(THUNDERBOLT_CHARGE_DEPTH, THUNDERBOLT_REAR_DEPTH, progress),
                )
            }

            time < THUNDERBOLT_ZIG_TWO_END -> {
                setActionPhase(2, SlimeExpression.FOCUSED)
                val progress =
                    smoothSegment(time, THUNDERBOLT_ZIG_ONE_END, THUNDERBOLT_ZIG_TWO_END)
                setActionPosition(
                    lerp(0f, THUNDERBOLT_ZIG_X, progress) * direction,
                    lerp(THUNDERBOLT_ZIG_Y, 0f, progress),
                    lerp(THUNDERBOLT_REAR_DEPTH, THUNDERBOLT_FRONT_DEPTH, progress),
                )
            }

            time < THUNDERBOLT_ZIG_THREE_END -> {
                setActionPhase(3, SlimeExpression.SURPRISED)
                val progress =
                    smoothSegment(time, THUNDERBOLT_ZIG_TWO_END, THUNDERBOLT_ZIG_THREE_END)
                setActionPosition(
                    lerp(THUNDERBOLT_ZIG_X, 0f, progress) * direction,
                    lerp(0f, -THUNDERBOLT_ZIG_Y, progress),
                    lerp(THUNDERBOLT_FRONT_DEPTH, THUNDERBOLT_REAR_DEPTH, progress),
                )
            }

            time < THUNDERBOLT_ZIG_FOUR_END -> {
                setActionPhase(4, SlimeExpression.FOCUSED)
                val progress =
                    smoothSegment(time, THUNDERBOLT_ZIG_THREE_END, THUNDERBOLT_ZIG_FOUR_END)
                setActionPosition(
                    lerp(0f, -THUNDERBOLT_ZIG_X, progress) * direction,
                    lerp(-THUNDERBOLT_ZIG_Y, 0f, progress),
                    lerp(THUNDERBOLT_REAR_DEPTH, THUNDERBOLT_FRONT_DEPTH, progress),
                )
            }

            time < THUNDERBOLT_PARTICLE_BREAK_START -> {
                setActionPhase(5, SlimeExpression.FOCUSED)
                val progress =
                    smoothSegment(time, THUNDERBOLT_ZIG_FOUR_END, THUNDERBOLT_PARTICLE_BREAK_START)
                setActionPosition(
                    lerp(-THUNDERBOLT_ZIG_X * direction, 0f, progress),
                    0f,
                    lerp(THUNDERBOLT_FRONT_DEPTH, 0f, progress),
                )
            }

            time < THUNDERBOLT_PARTICLE_RADIATE_END -> {
                if (setActionPhase(6, SlimeExpression.SURPRISED)) {
                    trailParticles.clear()
                }
                setActionPosition(0f, 0f, 0f)
            }

            time < THUNDERBOLT_MATRIX_FLASH_END -> {
                setActionPhase(7, SlimeExpression.SURPRISED)
                setActionPosition(0f, 0f, 0f)
            }

            time < THUNDERBOLT_FORM_REFORM_END -> {
                setActionPhase(8, SlimeExpression.FOCUSED)
                setActionPosition(0f, 0f, 0f)
            }

            else -> {
                setActionPhase(9, SlimeExpression.CURIOUS)
                setActionPosition(0f, 0f, 0f)
            }
        }
    }

    private fun SlimeScene.prepareThunderboltParticles() {
        forEachFormPixel(phenotype) { x, y, particleIndex ->
            val sourceAngle =
                if (x == 0 && y == 0) {
                    particleIndex * GOLDEN_ANGLE
                } else {
                    kotlin.math.atan2(y.toFloat(), x.toFloat())
                }
            val rayAngle =
                sourceAngle +
                    sin(particleIndex * GOLDEN_ANGLE) *
                    THUNDERBOLT_RAY_ANGLE_VARIATION
            val rimPoint = thunderboltRimPoint(rayAngle)
            thunderboltParticles +=
                ThunderboltParticle(
                    sourceX = x.toFloat(),
                    sourceY = y.toFloat(),
                    rimX = rimPoint.x,
                    rimY = rimPoint.y,
                )
        }
    }

    private fun SlimeScene.thunderboltRimPoint(angle: Float): PlanarVector {
        val directionX = cos(angle)
        val directionY = sin(angle)
        var rimX = 0
        var rimY = 0
        var distance = 0f
        while (distance <= THUNDERBOLT_RIM_SEARCH_RADIUS) {
            val candidateX = (directionX * distance).roundToInt()
            val candidateY = (directionY * distance).roundToInt()
            if (candidateX * candidateX + candidateY * candidateY <= MATRIX_RADIUS_SQUARED) {
                rimX = candidateX
                rimY = candidateY
            }
            distance += THUNDERBOLT_RIM_SEARCH_STEP
        }
        return PlanarVector(rimX.toFloat(), rimY.toFloat())
    }

    private fun SlimeScene.renderThunderboltParticles(frame: IntArray) {
        if (buttonActionElapsed >= THUNDERBOLT_PARTICLE_RIM_HOLD_END &&
            buttonActionElapsed < THUNDERBOLT_MATRIX_FLASH_END
        ) {
            renderThunderboltMatrixFlash(frame)
            return
        }

        val isRadiating = buttonActionElapsed < THUNDERBOLT_PARTICLE_RADIATE_END
        val progress =
            when {
                isRadiating -> {
                    easeOutCubic(
                        segmentProgress(
                            buttonActionElapsed,
                            THUNDERBOLT_PARTICLE_BREAK_START,
                            THUNDERBOLT_PARTICLE_RADIATE_END,
                        ),
                    )
                }

                buttonActionElapsed < THUNDERBOLT_PARTICLE_RIM_HOLD_END -> {
                    1f
                }

                else -> {
                    1f -
                        smoothSegment(buttonActionElapsed, THUNDERBOLT_MATRIX_FLASH_END, THUNDERBOLT_FORM_REFORM_END)
                }
            }
        val brightness =
            when {
                isRadiating -> {
                    lerp(THUNDERBOLT_PARTICLE_BREAK_BRIGHTNESS, THUNDERBOLT_PARTICLE_RIM_BRIGHTNESS, progress)
                }

                buttonActionElapsed < THUNDERBOLT_PARTICLE_RIM_HOLD_END -> {
                    THUNDERBOLT_PARTICLE_RIM_BRIGHTNESS
                }

                else -> {
                    lerp(
                        THUNDERBOLT_PARTICLE_REFORM_BRIGHTNESS,
                        THUNDERBOLT_PARTICLE_SETTLED_BRIGHTNESS,
                        1f - progress,
                    )
                }
            }.roundToInt()

        thunderboltParticles.forEach { particle ->
            setFramePixel(
                frame,
                (MATRIX_CENTER + lerp(particle.sourceX, particle.rimX, progress)).roundToInt(),
                (MATRIX_CENTER + lerp(particle.sourceY, particle.rimY, progress)).roundToInt(),
                brightness,
            )
        }
    }

    private fun SlimeScene.renderThunderboltMatrixFlash(frame: IntArray) {
        MATRIX_PIXEL_INDICES.forEach { frame[it] = THUNDERBOLT_MATRIX_FLASH_BRIGHTNESS }
    }
}

private const val THUNDERBOLT_CHARGE_END = 0.1f
private const val THUNDERBOLT_ZIG_ONE_END = 0.23f
private const val THUNDERBOLT_ZIG_TWO_END = 0.36f
private const val THUNDERBOLT_ZIG_THREE_END = 0.49f
private const val THUNDERBOLT_ZIG_FOUR_END = 0.62f
private const val THUNDERBOLT_PARTICLE_RADIATE_END = 1.12f
private const val THUNDERBOLT_PARTICLE_RIM_HOLD_END = 1.24f
private const val THUNDERBOLT_MATRIX_FLASH_END = 1.4f
private const val THUNDERBOLT_CHARGE_X = 2.5f
private const val THUNDERBOLT_CHARGE_DEPTH = -1f
private const val THUNDERBOLT_ZIG_X = 3f
private const val THUNDERBOLT_ZIG_Y = 3f
private const val THUNDERBOLT_REAR_DEPTH = -3.4f
private const val THUNDERBOLT_FRONT_DEPTH = 3.2f
private const val THUNDERBOLT_RAY_ANGLE_VARIATION = 0.16f
private const val THUNDERBOLT_RIM_SEARCH_RADIUS = 14f
private const val THUNDERBOLT_RIM_SEARCH_STEP = 0.1f
private const val THUNDERBOLT_PARTICLE_BREAK_BRIGHTNESS = 235f
private const val THUNDERBOLT_PARTICLE_RIM_BRIGHTNESS = 205f
private const val THUNDERBOLT_MATRIX_FLASH_BRIGHTNESS = 255
private const val THUNDERBOLT_PARTICLE_REFORM_BRIGHTNESS = 205f
private const val THUNDERBOLT_PARTICLE_SETTLED_BRIGHTNESS = 245f
