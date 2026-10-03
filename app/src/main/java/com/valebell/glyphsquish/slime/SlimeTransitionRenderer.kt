package com.valebell.glyphsquish.slime

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal class SlimeTransitionRenderer(
    private val scene: SlimeScene,
) {
    private val body = SlimeBodyRenderer(scene)

    fun render(
        frame: IntArray,
        transition: ActiveFormTransition,
    ) = scene.renderFormTransition(frame, transition)

    private fun SlimeScene.renderFormTransition(
        frame: IntArray,
        transition: ActiveFormTransition,
    ) {
        val progress =
            (transition.elapsedSeconds / FORM_TRANSITION_SECONDS)
                .coerceIn(0f, 1f)
        val morphProgress =
            smoothSegment(progress, FORM_MORPH_START, FORM_MORPH_END)
        val transitionPulse = sin(progress * PI.toFloat())
        val rotation = transitionPulse * FORM_MORPH_ROTATION
        val rotationCos = cos(rotation)
        val rotationSin = sin(rotation)
        val baseRadius = ORB_DIAMETER * SHAPE_SCALE / 2f
        val sourceWidth = effectiveWidthScale(transition.source)
        val sourceHeight = effectiveHeightScale(transition.source)
        val targetWidth = effectiveWidthScale(transition.target)
        val targetHeight = effectiveHeightScale(transition.target)
        val targetSettleProgress =
            smoothSegment(progress, FORM_TARGET_SETTLE_START, 1f)
        val particleVisibility = 1f - targetSettleProgress

        if (particleVisibility > FORM_PARTICLE_MIN_VISIBILITY) {
            repeat(FORM_PARTICLE_COUNT) { index ->
                val source = transition.sourcePoints[index]
                val target = transition.targetPoints[index]
                val localX =
                    lerp(source.x * sourceWidth, target.x * targetWidth, morphProgress) * baseRadius
                val localY =
                    lerp(source.y * sourceHeight, target.y * targetHeight, morphProgress) * baseRadius
                val rotatedX = localX * rotationCos - localY * rotationSin
                val rotatedY = localX * rotationSin + localY * rotationCos
                val particleDepth =
                    sin(index * GOLDEN_ANGLE) *
                        transitionPulse *
                        FORM_MORPH_DEPTH
                val projection =
                    project(positionX + rotatedX, positionY + rotatedY, positionZ + particleDepth)
                val particleLight =
                    FORM_PARTICLE_MIN_BRIGHTNESS +
                        FORM_PARTICLE_BRIGHTNESS_RANGE *
                        ((index * 37) % 11) / 10f
                val depthLight =
                    BURST_FAR_LIGHT +
                        (BURST_NEAR_LIGHT - BURST_FAR_LIGHT) *
                        ((projection.depth + 1f) / 2f)
                setFramePixel(
                    frame,
                    projection.centerX.roundToInt(),
                    projection.centerY.roundToInt(),
                    (particleLight * depthLight * particleVisibility).roundToInt(),
                )
            }
        }

        if (targetSettleProgress > 0f) {
            renderSettlingForm(
                frame = frame,
                value = transition.target,
                brightnessScale = targetSettleProgress,
            )
        }

        when {
            progress <= FORM_EYES_VISIBLE_START -> {
                cutTransitionEyes(frame, transition.source, rotation)
            }

            progress >= FORM_TARGET_SETTLE_START -> {
                cutTransitionEyes(frame, transition.target, rotation)
            }
        }
    }

    private fun SlimeScene.renderSettlingForm(
        frame: IntArray,
        value: SlimePhenotype,
        brightnessScale: Float,
    ) {
        val width = formDimension(effectiveWidthScale(value))
        val height = formDimension(effectiveHeightScale(value))
        body.draw(
            frame = frame,
            bodyMask = BooleanArray(PIXEL_COUNT),
            form = value.form,
            bounds =
                RenderBounds(
                    left = MATRIX_CENTER - width / 2,
                    top = MATRIX_CENTER - height / 2,
                    width = width,
                    height = height,
                ),
            depth = 0f,
            brightnessScale = brightnessScale,
        )
    }

    private fun SlimeScene.cutTransitionEyes(
        frame: IntArray,
        value: SlimePhenotype,
        rotation: Float,
    ) {
        val baseRadius = ORB_DIAMETER * SHAPE_SCALE / 2f
        val eyeX =
            baseRadius *
                effectiveWidthScale(value) *
                SlimeFormGeometry.eyeSpacing(value.form)
        val eyeY =
            baseRadius *
                effectiveHeightScale(value) *
                SlimeFormGeometry.eyeOffset(value.form)
        listOf(-eyeX, eyeX).forEach { localX ->
            val rotatedX = localX * cos(rotation) - eyeY * sin(rotation)
            val rotatedY = localX * sin(rotation) + eyeY * cos(rotation)
            eraseTransitionEye(
                frame = frame,
                x = positionX + rotatedX,
                y = positionY + rotatedY,
                rotation = rotation,
            )
        }
    }

    private fun SlimeScene.eraseTransitionEye(
        frame: IntArray,
        x: Float,
        y: Float,
        rotation: Float,
    ) {
        repeat((2f * EYE_SCALE).roundToInt()) { offset ->
            val pixelX = (MATRIX_CENTER + (x - sin(rotation) * offset)).roundToInt()
            val pixelY = (MATRIX_CENTER + (y + cos(rotation) * offset)).roundToInt()
            if (isMatrixPixel(pixelX, pixelY)) {
                frame[indexOf(pixelX, pixelY)] = PIXEL_OFF
            }
        }
    }
}

private const val FORM_MORPH_START = 0.14f
private const val FORM_MORPH_END = 0.84f
private const val FORM_MORPH_ROTATION = 0.42f
private const val FORM_MORPH_DEPTH = 2.1f
private const val FORM_TARGET_SETTLE_START = 0.72f
private const val FORM_PARTICLE_MIN_VISIBILITY = 0.02f
private const val FORM_PARTICLE_MIN_BRIGHTNESS = 148f
private const val FORM_PARTICLE_BRIGHTNESS_RANGE = 92f
private const val FORM_EYES_VISIBLE_START = 0.16f
