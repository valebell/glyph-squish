package com.valebell.glyphsquish.slime

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

internal class SlimeRenderer(
    private val scene: SlimeScene,
) {
    private val body = SlimeBodyRenderer(scene)
    private val transitionRenderer = SlimeTransitionRenderer(scene)
    private val burst = SlimeBurst(scene)
    private val thunderbolt = SlimeThunderbolt(scene)
    private val trail = SlimeTrail(scene)

    fun render(): IntArray = scene.renderFrame()

    private fun SlimeScene.renderFrame(): IntArray {
        val frame = IntArray(PIXEL_COUNT)
        trail.render(frame)
        renderEchoRing(frame)
        formTransition?.let { transition ->
            transitionRenderer.render(frame, transition)
            renderSemanticAnimations(frame)
            return frame
        }
        if (isBurstParticlePhase()) {
            burst.render(frame)
            renderSemanticAnimations(frame)
            return frame
        }
        if (isThunderboltParticlePhase()) {
            thunderbolt.render(frame)
            renderSemanticAnimations(frame)
            return frame
        }

        val bodyMask = BooleanArray(PIXEL_COUNT)
        val deformation = calculateDeformation()
        val projection =
            project(x = positionX, y = positionY, z = positionZ - pressAmount * PRESS_DEPTH_OFFSET)
        val width = oddDimension(formDimension(effectiveWidthScale(phenotype)) + deformation.widthDelta)
        val height = oddDimension(formDimension(effectiveHeightScale(phenotype)) + deformation.heightDelta)
        val centerX = projection.centerX.roundToInt()
        val centerY = projection.centerY.roundToInt()
        val top = centerY - height / 2
        val left = centerX - width / 2

        body.draw(
            frame = frame,
            bodyMask = bodyMask,
            form = phenotype.form,
            bounds = RenderBounds(left, top, width, height),
            depth = projection.depth,
        )
        val eyeY =
            centerY +
                (
                    height / 2f *
                        SlimeFormGeometry.eyeOffset(phenotype.form)
                ).roundToInt()
        val maximumEyeSpacing = max(1, width / 3)
        body.eyes(
            frame = frame,
            bodyMask = bodyMask,
            centerX = centerX,
            eyeY = eyeY,
            eyeSpacing =
                (
                    width / 2f *
                        SlimeFormGeometry.eyeSpacing(phenotype.form)
                ).roundToInt().coerceIn(1, maximumEyeSpacing),
        )
        renderSemanticAnimations(frame)
        return frame
    }

    private fun SlimeScene.calculateDeformation(): Deformation {
        var widthDelta = (pressAmount * 2f).roundToInt()
        var heightDelta = -(pressAmount * 2f).roundToInt()

        if (impactAmount > 0f) {
            val impactPixels = (impactAmount * IMPACT_DEFORMATION_PIXELS).roundToInt()
            when {
                abs(impactNormalZ) > max(abs(impactNormalX), abs(impactNormalY)) -> {
                    widthDelta += impactPixels
                    heightDelta += impactPixels
                }

                abs(impactNormalX) > abs(impactNormalY) -> {
                    widthDelta -= impactPixels
                    heightDelta += impactPixels
                }

                else -> {
                    widthDelta += impactPixels
                    heightDelta -= impactPixels
                }
            }
        }

        val speedX = abs(velocityX)
        val speedY = abs(velocityY)
        val speedZ = abs(velocityZ)
        if (max(speedZ, max(speedX, speedY)) > STRETCH_SPEED_THRESHOLD) {
            when {
                speedZ > max(speedX, speedY) -> {
                    val depthStretch = if (velocityZ > 0f) 2 else -2
                    widthDelta += depthStretch
                    heightDelta += depthStretch
                }

                speedX > speedY -> {
                    widthDelta += 2
                    heightDelta -= 1
                }

                else -> {
                    widthDelta -= 2
                    heightDelta += 1
                }
            }
        }

        if (expression == SlimeExpression.HAPPY) {
            val phase = elapsedSeconds / HAPPY_PULSE_SECONDS * TWO_PI
            if (cos(phase) > 0.7f) {
                widthDelta += 2
                heightDelta += 2
            }
        }

        val compressionPixels =
            (actionCompressionAmount() * ACTION_COMPRESSION_PIXELS).roundToInt()
        widthDelta += compressionPixels
        heightDelta -= compressionPixels

        return Deformation(widthDelta, heightDelta)
    }

    private fun SlimeScene.renderEchoRing(frame: IntArray) {
        if (activeButtonAction != SlimeButtonAction.ECHO) return
        val time = buttonActionElapsed
        if (time < ECHO_CROUCH_END || time >= ECHO_RING_BREAK_END) return

        val radius: Float
        val brightness: Float
        val breakProgress: Float
        when {
            time < ECHO_REAR_END -> {
                val progress =
                    smoothSegment(time, ECHO_CROUCH_END, ECHO_REAR_END)
                radius = lerp(ECHO_RING_START_RADIUS, ECHO_RING_REAR_RADIUS, progress)
                brightness =
                    lerp(ECHO_RING_START_BRIGHTNESS, ECHO_RING_REAR_BRIGHTNESS, progress)
                breakProgress = 0f
            }

            time < ECHO_REAR_HOLD_END -> {
                radius = ECHO_RING_REAR_RADIUS
                brightness = ECHO_RING_REAR_BRIGHTNESS
                breakProgress = 0f
            }

            time < ECHO_RETURN_END -> {
                val progress =
                    smoothSegment(time, ECHO_REAR_HOLD_END, ECHO_RETURN_END)
                radius = lerp(ECHO_RING_REAR_RADIUS, ECHO_RING_FRONT_RADIUS, progress)
                brightness =
                    lerp(ECHO_RING_REAR_BRIGHTNESS, ECHO_RING_FRONT_BRIGHTNESS, progress)
                breakProgress = 0f
            }

            else -> {
                val progress =
                    smoothSegment(time, ECHO_RETURN_END, ECHO_RING_BREAK_END)
                radius = lerp(ECHO_RING_FRONT_RADIUS, ECHO_RING_BREAK_RADIUS, progress)
                brightness = lerp(ECHO_RING_FRONT_BRIGHTNESS, 0f, progress)
                breakProgress = progress
            }
        }

        repeat(ECHO_RING_PIXEL_COUNT) { pixelIndex ->
            val shouldBreak =
                breakProgress > 0f &&
                    pixelIndex % ECHO_RING_BREAK_STRIDE <
                    (breakProgress * ECHO_RING_BREAK_STRIDE).roundToInt()
            if (shouldBreak) return@repeat

            val angle = pixelIndex.toFloat() / ECHO_RING_PIXEL_COUNT * TWO_PI
            setFramePixel(
                frame,
                (MATRIX_CENTER + cos(angle) * radius).roundToInt(),
                (MATRIX_CENTER + sin(angle) * radius).roundToInt(),
                brightness.roundToInt(),
            )
        }
    }

    private fun SlimeScene.renderSemanticAnimations(frame: IntArray) {
        animationCoordinator.renderPixels().forEach { pixel ->
            setFramePixel(frame, pixel.x, pixel.y, pixel.brightness)
        }
    }
}

private const val PRESS_DEPTH_OFFSET = 1.4f
private const val ACTION_COMPRESSION_PIXELS = 4f
private const val ECHO_RING_BREAK_END = 1.07f
private const val ECHO_RING_START_RADIUS = 3.5f
private const val ECHO_RING_REAR_RADIUS = 2f
private const val ECHO_RING_FRONT_RADIUS = 7.5f
private const val ECHO_RING_BREAK_RADIUS = 9.5f
private const val ECHO_RING_START_BRIGHTNESS = 150f
private const val ECHO_RING_REAR_BRIGHTNESS = 70f
private const val ECHO_RING_FRONT_BRIGHTNESS = 185f
private const val ECHO_RING_PIXEL_COUNT = 16
private const val ECHO_RING_BREAK_STRIDE = 4
private const val IMPACT_DEFORMATION_PIXELS = 3f
private const val STRETCH_SPEED_THRESHOLD = 6f
private const val HAPPY_PULSE_SECONDS = 1.1f
