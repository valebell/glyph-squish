package com.valebell.glyphsquish.slime

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal class SlimeBurst(
    private val scene: SlimeScene,
) {
    fun step() = scene.applyBurstAnimation()

    fun prepare() = scene.prepareBurstPixels()

    fun render(frame: IntArray) = scene.renderBurstPixels(frame)

    private fun SlimeScene.applyBurstAnimation() {
        val time = buttonActionElapsed
        val feintZ = min(actionOriginZ + BURST_FEINT_DEPTH, BURST_MAX_FEINT_DEPTH)
        when {
            time < BURST_ANTICIPATION_END -> {
                setActionPhase(0, SlimeExpression.FOCUSED)
                val progress =
                    smoothSegment(time, 0f, BURST_ANTICIPATION_END)
                setActionPosition(
                    lerp(actionOriginX, actionOriginX * 0.65f, progress),
                    lerp(actionOriginY, actionOriginY * 0.65f, progress),
                    lerp(actionOriginZ, feintZ, progress),
                )
            }

            time < BURST_EXPLOSION_START -> {
                setActionPhase(1, SlimeExpression.SURPRISED)
                val progress =
                    smoothSegment(time, BURST_ANTICIPATION_END, BURST_EXPLOSION_START)
                setActionPosition(
                    lerp(actionOriginX * 0.65f, 0f, progress),
                    lerp(actionOriginY * 0.65f, 0f, progress),
                    lerp(feintZ, BURST_ASSEMBLY_DEPTH, progress),
                )
            }

            time < BURST_LOCK_END -> {
                setActionPhase(2, SlimeExpression.FOCUSED)
                val returnProgress =
                    smoothSegment(time, BURST_REASSEMBLY_START, BURST_LOCK_END)
                setActionPosition(
                    0f,
                    0f,
                    lerp(BURST_ASSEMBLY_DEPTH, 0f, returnProgress),
                )
            }

            else -> {
                setActionPhase(3, SlimeExpression.HAPPY)
                setActionPosition(0f, 0f, 0f)
            }
        }
    }

    private fun SlimeScene.prepareBurstPixels() {
        forEachFormPixel(phenotype) { x, y, particleIndex ->
            val angle = particleIndex * GOLDEN_ANGLE
            val distance =
                BURST_SCATTER_MIN_DISTANCE +
                    (particleIndex % BURST_SCATTER_DISTANCE_STEPS) *
                    BURST_SCATTER_DISTANCE_STEP
            burstPixels +=
                BurstPixel(
                    compressedX = x * BURST_COMPRESSION_WIDTH,
                    compressedY = y * BURST_COMPRESSION_HEIGHT,
                    targetX = x.toFloat(),
                    targetY = y.toFloat(),
                    scatterX = cos(angle) * distance + x * BURST_SOURCE_INFLUENCE,
                    scatterY = sin(angle) * distance + y * BURST_SOURCE_INFLUENCE,
                    scatterZ =
                        sin(angle * BURST_DEPTH_FREQUENCY) *
                            BURST_SCATTER_DEPTH,
                )
        }
    }

    private fun SlimeScene.renderBurstPixels(frame: IntArray) {
        val isExploding = buttonActionElapsed < BURST_REASSEMBLY_START
        val progress =
            if (isExploding) {
                easeOutCubic(
                    segmentProgress(buttonActionElapsed, BURST_EXPLOSION_START, BURST_REASSEMBLY_START),
                )
            } else {
                smoothSegment(buttonActionElapsed, BURST_REASSEMBLY_START, BURST_LOCK_END)
            }
        val rotation = (1f - progress) * BURST_REASSEMBLY_ROTATION
        val rotationCos = cos(rotation)
        val rotationSin = sin(rotation)

        burstPixels.forEach { particle ->
            val x: Float
            val y: Float
            val z: Float
            val brightness: Float
            if (isExploding) {
                x = lerp(particle.compressedX, particle.scatterX, progress)
                y = lerp(particle.compressedY, particle.scatterY, progress)
                z = lerp(BURST_ASSEMBLY_DEPTH, particle.scatterZ, progress)
                brightness =
                    lerp(BURST_EXPLOSION_BRIGHTNESS, BURST_SCATTER_BRIGHTNESS, progress)
            } else {
                val rotatedX =
                    particle.scatterX * rotationCos - particle.scatterY * rotationSin
                val rotatedY =
                    particle.scatterX * rotationSin + particle.scatterY * rotationCos
                x = lerp(rotatedX, particle.targetX, progress)
                y = lerp(rotatedY, particle.targetY, progress)
                z = lerp(particle.scatterZ, 0f, progress)
                brightness =
                    lerp(BURST_SCATTER_BRIGHTNESS, BURST_REASSEMBLY_BRIGHTNESS, progress)
            }

            val projection = project(x, y, z)
            val depthLight =
                BURST_FAR_LIGHT +
                    (BURST_NEAR_LIGHT - BURST_FAR_LIGHT) *
                    ((projection.depth + 1f) / 2f)
            setFramePixel(
                frame,
                projection.centerX.roundToInt(),
                projection.centerY.roundToInt(),
                (brightness * depthLight).roundToInt(),
            )
        }
    }
}

private const val BURST_REASSEMBLY_START = 0.8f
private const val BURST_FEINT_DEPTH = 0.7f
private const val BURST_MAX_FEINT_DEPTH = 2f
private const val BURST_ASSEMBLY_DEPTH = -1.1f
private const val BURST_COMPRESSION_WIDTH = 1.25f
private const val BURST_COMPRESSION_HEIGHT = 0.55f
private const val BURST_SOURCE_INFLUENCE = 0.25f
private const val BURST_SCATTER_MIN_DISTANCE = 3.2f
private const val BURST_SCATTER_DISTANCE_STEPS = 5
private const val BURST_SCATTER_DISTANCE_STEP = 0.55f
private const val BURST_SCATTER_DEPTH = 4.5f
private const val BURST_DEPTH_FREQUENCY = 1.37f
private const val BURST_REASSEMBLY_ROTATION = 6.2831855f
private const val BURST_EXPLOSION_BRIGHTNESS = 230f
private const val BURST_SCATTER_BRIGHTNESS = 125f
private const val BURST_REASSEMBLY_BRIGHTNESS = 235f
