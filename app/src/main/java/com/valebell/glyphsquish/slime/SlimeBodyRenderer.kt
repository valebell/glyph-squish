package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

internal class SlimeBodyRenderer(
    private val scene: SlimeScene,
) {
    fun draw(
        frame: IntArray,
        bodyMask: BooleanArray,
        form: CreatureForm,
        bounds: RenderBounds,
        depth: Float,
        brightnessScale: Float = 1f,
    ) = scene.drawForm(frame, bodyMask, form, bounds, depth, brightnessScale)

    fun eyes(
        frame: IntArray,
        bodyMask: BooleanArray,
        centerX: Int,
        eyeY: Int,
        eyeSpacing: Int,
    ) = scene.cutOutEyes(frame, bodyMask, centerX, eyeY, eyeSpacing)

    private fun SlimeScene.drawForm(
        frame: IntArray,
        bodyMask: BooleanArray,
        form: CreatureForm,
        bounds: RenderBounds,
        depth: Float,
        brightnessScale: Float = 1f,
    ) {
        val centerX = (bounds.width - 1) / 2f
        val centerY = (bounds.height - 1) / 2f
        val radiusX = bounds.width / 2f
        val radiusY = bounds.height / 2f
        val depthMix = (depth + 1f) / 2f
        val peakBrightness =
            BODY_FAR_PEAK_BRIGHTNESS +
                (BODY_NEAR_PEAK_BRIGHTNESS - BODY_FAR_PEAK_BRIGHTNESS) * depthMix

        for (localY in 0 until bounds.height) {
            for (localX in 0 until bounds.width) {
                val normalizedX = (localX - centerX) / radiusX
                val normalizedY = (localY - centerY) / radiusY
                val brightness = bodyPixelBrightness(form, normalizedX, normalizedY, peakBrightness)
                if (brightness == PIXEL_OFF) continue

                val x = bounds.left + localX
                val y = bounds.top + localY
                if (!isMatrixPixel(x, y)) continue

                val index = indexOf(x, y)
                bodyMask[index] = true
                frame[index] =
                    max(
                        frame[index],
                        (brightness * brightnessScale).roundToInt(),
                    )
            }
        }
    }

    private fun SlimeScene.bodyPixelBrightness(
        form: CreatureForm,
        normalizedX: Float,
        normalizedY: Float,
        peakBrightness: Float,
    ): Int {
        // Physical LEDs need only the silhouette; drawForm still applies transition fades.
        if (!shadeBody) return if (SlimeFormGeometry.contains(form, normalizedX, normalizedY)) 255 else PIXEL_OFF
        val radius = SlimeFormGeometry.normalizedRadius(form, normalizedX, normalizedY)
        if (radius > 1f) return PIXEL_OFF
        val surfaceZ = sqrt(max(0f, 1f - radius * radius))
        val light = (normalizedX * LIGHT_X + normalizedY * LIGHT_Y + surfaceZ * LIGHT_Z).coerceIn(0f, 1f)
        val lighting = ORB_AMBIENT_LIGHT + ORB_DIFFUSE_LIGHT * light + ORB_CURVE_LIGHT * surfaceZ
        return quantizeBodyBrightness(peakBrightness * lighting)
    }

    private fun SlimeScene.quantizeBodyBrightness(value: Float): Int =
        when {
            value >= BODY_HIGHLIGHT_THRESHOLD -> BODY_HIGHLIGHT_BRIGHTNESS
            value >= BODY_MIDTONE_THRESHOLD -> BODY_MIDTONE_BRIGHTNESS
            else -> BODY_SHADOW_BRIGHTNESS
        }

    private fun SlimeScene.cutOutEyes(
        frame: IntArray,
        bodyMask: BooleanArray,
        centerX: Int,
        eyeY: Int,
        eyeSpacing: Int,
    ) {
        val leftEyeX = centerX - eyeSpacing
        val rightEyeX = centerX + eyeSpacing
        val speed = speed()

        when {
            pressAmount > 0.45f -> {
                cutHorizontalEye(frame, bodyMask, leftEyeX, eyeY)
                cutHorizontalEye(frame, bodyMask, rightEyeX, eyeY)
            }

            expression == SlimeExpression.HAPPY -> {
                cutHappyEye(frame, bodyMask, leftEyeX, eyeY)
                cutHappyEye(frame, bodyMask, rightEyeX, eyeY)
            }

            expression == SlimeExpression.DIZZY -> {
                cutDiagonalEye(frame, bodyMask, leftEyeX, eyeY, mirrored = false)
                cutDiagonalEye(frame, bodyMask, rightEyeX, eyeY, mirrored = true)
            }

            expression == SlimeExpression.FOCUSED -> {
                cutVerticalEye(frame, bodyMask, leftEyeX, eyeY)
                cutVerticalEye(frame, bodyMask, rightEyeX, eyeY)
            }

            expression == SlimeExpression.SURPRISED || speed > SURPRISE_SPEED_THRESHOLD -> {
                cutWideEye(frame, bodyMask, leftEyeX, eyeY)
                cutWideEye(frame, bodyMask, rightEyeX, eyeY)
            }

            else -> {
                cutCuriousEyes(frame, bodyMask, leftEyeX, rightEyeX, eyeY)
            }
        }
    }

    private fun SlimeScene.cutCuriousEyes(
        frame: IntArray,
        bodyMask: BooleanArray,
        leftEyeX: Int,
        rightEyeX: Int,
        eyeY: Int,
    ) {
        val blink =
            elapsedSeconds % CURIOUS_BLINK_SECONDS >
                CURIOUS_BLINK_SECONDS - BLINK_DURATION_SECONDS
        if (blink) {
            cutHorizontalEye(frame, bodyMask, leftEyeX, eyeY)
            cutHorizontalEye(frame, bodyMask, rightEyeX, eyeY)
        } else {
            val nearShell =
                sqrt(
                    positionX * positionX +
                        positionY * positionY +
                        positionZ * positionZ,
                ) > MOVEMENT_RADIUS * ANTICIPATION_RADIUS_FACTOR
            val gazeX =
                when {
                    abs(tiltX) > GAZE_THRESHOLD -> {
                        tiltX.sign.roundToInt()
                    }

                    nearShell && abs(positionX) > GAZE_THRESHOLD -> {
                        positionX.sign.roundToInt()
                    }

                    else -> {
                        0
                    }
                }
            val gazeY =
                when {
                    abs(tiltY) > GAZE_THRESHOLD -> {
                        tiltY.sign.roundToInt()
                    }

                    nearShell && abs(positionY) > GAZE_THRESHOLD -> {
                        positionY.sign.roundToInt()
                    }

                    else -> {
                        0
                    }
                }
            cutVerticalEye(frame, bodyMask, leftEyeX + gazeX, eyeY + gazeY)
            cutVerticalEye(frame, bodyMask, rightEyeX + gazeX, eyeY + gazeY)
        }
    }

    private fun SlimeScene.cutVerticalEye(
        frame: IntArray,
        bodyMask: BooleanArray,
        centerX: Int,
        centerY: Int,
    ) {
        cutEyeRectangle(
            frame,
            bodyMask,
            centerX,
            centerY,
            width = 1,
            height = (2f * EYE_SCALE).roundToInt(),
        )
    }

    private fun SlimeScene.cutHorizontalEye(
        frame: IntArray,
        bodyMask: BooleanArray,
        centerX: Int,
        centerY: Int,
    ) {
        cutEyeRectangle(
            frame,
            bodyMask,
            centerX,
            centerY,
            width = (BASE_EYE_WIDTH * EYE_SCALE).roundToInt(),
            height = 1,
        )
    }

    private fun SlimeScene.cutWideEye(
        frame: IntArray,
        bodyMask: BooleanArray,
        centerX: Int,
        centerY: Int,
    ) {
        val size = (2f * EYE_SCALE).roundToInt()
        cutEyeRectangle(frame, bodyMask, centerX, centerY, size, size)
    }

    private fun SlimeScene.cutHappyEye(
        frame: IntArray,
        bodyMask: BooleanArray,
        centerX: Int,
        centerY: Int,
    ) {
        val width = BASE_EYE_WIDTH
        val radius = width / 2
        for (xOffset in -radius..radius) {
            val yOffset =
                (abs(xOffset).toFloat() / radius)
                    .roundToInt()
                    .coerceAtMost(2)
            cutEyePixel(frame, bodyMask, centerX + xOffset, centerY + yOffset)
        }
    }

    private fun SlimeScene.cutDiagonalEye(
        frame: IntArray,
        bodyMask: BooleanArray,
        centerX: Int,
        centerY: Int,
        mirrored: Boolean,
    ) {
        val width = BASE_EYE_WIDTH
        val radius = width / 2
        for (xOffset in -radius..radius) {
            val diagonalOffset = if (mirrored) -xOffset else xOffset
            val yOffset =
                if (diagonalOffset > 0) {
                    (diagonalOffset.toFloat() / radius)
                        .roundToInt()
                        .coerceAtMost(2)
                } else {
                    0
                }
            cutEyePixel(frame, bodyMask, centerX + xOffset, centerY + yOffset)
        }
    }

    private fun SlimeScene.cutEyeRectangle(
        frame: IntArray,
        bodyMask: BooleanArray,
        centerX: Int,
        top: Int,
        width: Int,
        height: Int,
    ) {
        val left = centerX - width / 2
        repeat(height) { yOffset ->
            repeat(width) { xOffset ->
                cutEyePixel(frame, bodyMask, left + xOffset, top + yOffset)
            }
        }
    }

    private fun SlimeScene.cutEyePixel(
        frame: IntArray,
        bodyMask: BooleanArray,
        x: Int,
        y: Int,
    ) {
        if (!isInBounds(x, y)) return
        val index = indexOf(x, y)
        if (bodyMask[index]) {
            frame[index] = PIXEL_OFF
        }
    }
}

private const val BASE_EYE_WIDTH = 3
private const val SURPRISE_SPEED_THRESHOLD = 10.5f
private const val GAZE_THRESHOLD = 0.22f
private const val ANTICIPATION_RADIUS_FACTOR = 0.82f
private const val CURIOUS_BLINK_SECONDS = 3.4f
private const val BLINK_DURATION_SECONDS = 0.14f
private const val BODY_FAR_PEAK_BRIGHTNESS = 210f
private const val BODY_NEAR_PEAK_BRIGHTNESS = 255f
private const val BODY_SHADOW_BRIGHTNESS = 170
private const val BODY_MIDTONE_BRIGHTNESS = 215
private const val BODY_HIGHLIGHT_BRIGHTNESS = 255
private const val BODY_MIDTONE_THRESHOLD = 175f
private const val BODY_HIGHLIGHT_THRESHOLD = 220f
private const val LIGHT_X = -0.28f
private const val LIGHT_Y = -0.38f
private const val LIGHT_Z = 0.88f
private const val ORB_AMBIENT_LIGHT = 0.68f
private const val ORB_DIFFUSE_LIGHT = 0.2f
private const val ORB_CURVE_LIGHT = 0.12f
