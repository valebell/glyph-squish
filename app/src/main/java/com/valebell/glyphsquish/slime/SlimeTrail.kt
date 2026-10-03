package com.valebell.glyphsquish.slime

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

internal class SlimeTrail(
    private val scene: SlimeScene,
) {
    fun step(delta: Float) = scene.updateTrail(delta)

    fun render(frame: IntArray) = scene.renderTrail(frame)

    private fun SlimeScene.updateTrail(delta: Float) {
        val particleDecay = exp((-TRAIL_DRAG * delta).toDouble()).toFloat()
        val iterator = trailParticles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.ageSeconds += delta
            if (particle.ageSeconds >= TRAIL_LIFETIME_SECONDS) {
                iterator.remove()
                continue
            }
            particle.x += particle.velocityX * delta
            particle.y += particle.velocityY * delta
            particle.z += particle.velocityZ * delta
            particle.velocityX *= particleDecay
            particle.velocityY *= particleDecay
            particle.velocityZ *= particleDecay
        }

        trailEmissionSeconds += delta
        if (speed() < TRAIL_MIN_SPEED) {
            trailEmissionSeconds =
                min(trailEmissionSeconds, TRAIL_EMISSION_INTERVAL_SECONDS)
            return
        }

        while (trailEmissionSeconds >= TRAIL_EMISSION_INTERVAL_SECONDS) {
            emitTrailParticle()
            trailEmissionSeconds -= TRAIL_EMISSION_INTERVAL_SECONDS
        }
    }

    private fun SlimeScene.emitTrailParticle() {
        if (trailParticles.size >= MAX_TRAIL_PARTICLES) {
            trailParticles.removeAt(0)
        }

        val phase = trailSequence * GOLDEN_ANGLE
        val planarSpeed = sqrt(velocityX * velocityX + velocityY * velocityY)
        val backX =
            if (planarSpeed > MIN_PLANAR_TRAIL_SPEED) {
                -velocityX / planarSpeed
            } else {
                cos(phase)
            }
        val backY =
            if (planarSpeed > MIN_PLANAR_TRAIL_SPEED) {
                -velocityY / planarSpeed
            } else {
                sin(phase)
            }
        val tangentX = -backY
        val tangentY = backX
        val lateralOffset = sin(phase) * TRAIL_LATERAL_SPREAD
        val depthDirection =
            if (abs(velocityZ) > MIN_DEPTH_TRAIL_SPEED) {
                velocityZ.sign
            } else {
                0f
            }

        trailParticles +=
            TrailParticle(
                x =
                    positionX +
                        backX * TRAIL_SPAWN_DISTANCE * SHAPE_SCALE +
                        tangentX * lateralOffset,
                y =
                    positionY +
                        backY * TRAIL_SPAWN_DISTANCE * SHAPE_SCALE +
                        tangentY * lateralOffset,
                z = positionZ - depthDirection * TRAIL_DEPTH_OFFSET,
                velocityX =
                    velocityX * TRAIL_INHERITED_VELOCITY +
                        tangentX * cos(phase) * TRAIL_SIDE_VELOCITY,
                velocityY =
                    velocityY * TRAIL_INHERITED_VELOCITY +
                        tangentY * cos(phase) * TRAIL_SIDE_VELOCITY,
                velocityZ = velocityZ * TRAIL_INHERITED_VELOCITY,
                sequence = trailSequence,
            )
        trailSequence += 1
    }

    private fun SlimeScene.renderTrail(frame: IntArray) {
        trailParticles.forEach { particle ->
            val life =
                (1f - particle.ageSeconds / TRAIL_LIFETIME_SECONDS)
                    .coerceIn(0f, 1f)
            val projection = project(particle.x, particle.y, particle.z)
            val depthLight =
                TRAIL_FAR_LIGHT +
                    (TRAIL_NEAR_LIGHT - TRAIL_FAR_LIGHT) * ((projection.depth + 1f) / 2f)
            val brightness =
                (
                    TRAIL_MAX_BRIGHTNESS * life * life * depthLight
                ).roundToInt()
            if (brightness <= 0) return@forEach

            val x = projection.centerX.roundToInt()
            val y = projection.centerY.roundToInt()
            setFramePixel(frame, x, y, brightness)
            if (life > TRAIL_DOUBLE_PIXEL_LIFE) {
                val phase = particle.sequence * GOLDEN_ANGLE
                setFramePixel(
                    frame,
                    x + cos(phase).sign.roundToInt(),
                    y + sin(phase).sign.roundToInt(),
                    (brightness * TRAIL_SECOND_PIXEL_BRIGHTNESS).roundToInt(),
                )
            }
        }
    }
}

private const val TRAIL_MIN_SPEED = 1.8f
private const val TRAIL_LIFETIME_SECONDS = 0.72f
private const val TRAIL_DRAG = 2.6f
private const val TRAIL_SPAWN_DISTANCE = 4.4f
private const val TRAIL_LATERAL_SPREAD = 0.75f
private const val TRAIL_DEPTH_OFFSET = 0.65f
private const val TRAIL_INHERITED_VELOCITY = 0.14f
private const val TRAIL_SIDE_VELOCITY = 0.65f
private const val TRAIL_MAX_BRIGHTNESS = 132f
private const val TRAIL_FAR_LIGHT = 0.55f
private const val TRAIL_NEAR_LIGHT = 1f
private const val TRAIL_DOUBLE_PIXEL_LIFE = 0.72f
private const val TRAIL_SECOND_PIXEL_BRIGHTNESS = 0.58f
private const val MIN_PLANAR_TRAIL_SPEED = 0.5f
private const val MIN_DEPTH_TRAIL_SPEED = 0.35f
private const val MAX_TRAIL_PARTICLES = 16
