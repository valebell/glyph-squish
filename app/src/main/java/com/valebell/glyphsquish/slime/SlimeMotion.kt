package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.ShellSector
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

internal class SlimeMotion(
    private val scene: SlimeScene,
) {
    fun step(delta: Float) = scene.integrate(delta)

    private fun SlimeScene.integrate(delta: Float) {
        val profile = MotionProfile.forExpression(expression)
        val planarProfile = PlanarMotionProfile.forForm(phenotype.form)
        val shapedTilt = shapePlanarInput(tiltX, tiltY, planarProfile)
        val shapedPhoneAcceleration =
            shapePlanarInput(
                phoneAccelerationX,
                phoneAccelerationY,
                planarProfile,
            )
        val pressMobility = 1f - pressAmount * 0.68f
        val phoneAccelerationMagnitude =
            sqrt(
                phoneAccelerationX * phoneAccelerationX +
                    phoneAccelerationY * phoneAccelerationY +
                    phoneAccelerationZ * phoneAccelerationZ,
            ).coerceIn(0f, 1f)
        val floatWeight = 1f - phoneAccelerationMagnitude
        val floatPeriod = profile.idleDrift.periodSeconds
        val floatPhase = elapsedSeconds / floatPeriod * TWO_PI
        velocityX += (
            shapedTilt.x * profile.tiltAcceleration -
                positionX * profile.centering * planarProfile.centeringScale -
                shapedPhoneAcceleration.x * PHONE_ACCELERATION_RESPONSE +
                sin(floatPhase) *
                profile.idleDrift.acceleration *
                planarProfile.floatScale *
                floatWeight
        ) * pressMobility * delta
        velocityY += (
            shapedTilt.y * profile.tiltAcceleration -
                positionY * profile.centering * planarProfile.centeringScale -
                shapedPhoneAcceleration.y * PHONE_ACCELERATION_RESPONSE +
                sin(floatPhase * FLOAT_Y_FREQUENCY_RATIO) *
                profile.idleDrift.acceleration *
                planarProfile.floatScale *
                FLOAT_Y_STRENGTH *
                floatWeight
        ) * pressMobility * delta
        val depthTarget = tiltZ * profile.depthTiltRange
        velocityZ += (
            (depthTarget - positionZ) * profile.depthCentering -
                phoneAccelerationZ * PHONE_ACCELERATION_RESPONSE +
                sin(floatPhase * FLOAT_Z_FREQUENCY_RATIO) *
                profile.idleDrift.depthAcceleration * floatWeight
        ) * pressMobility * delta

        val planarDecay =
            exp(
                (-profile.damping * planarProfile.dampingScale * delta).toDouble(),
            ).toFloat()
        val depthDecay = exp((-profile.damping * delta).toDouble()).toFloat()
        velocityX *= planarDecay
        velocityY *= planarDecay
        velocityZ *= depthDecay

        limitContinuousVelocity(planarProfile.maxSpeed)
        positionX += velocityX * delta
        positionY += velocityY * delta
        positionZ += velocityZ * delta
        resolveSphericalBoundary(
            profile.bounce.coerceIn(MIN_BOUNCE, MAX_BOUNCE),
        )
        emitInteraction(
            CreatureInteraction.CirclePosition(
                positionX / MOVEMENT_RADIUS,
                positionY / MOVEMENT_RADIUS,
                positionZ / MOVEMENT_RADIUS,
            ),
        )
    }

    private fun SlimeScene.limitContinuousVelocity(maxPlanarSpeed: Float) {
        val planarSpeed = sqrt(velocityX * velocityX + velocityY * velocityY)
        if (planarSpeed > maxPlanarSpeed) {
            val scale = maxPlanarSpeed / planarSpeed
            velocityX *= scale
            velocityY *= scale
        }
        velocityZ = velocityZ.coerceIn(-MAX_SPEED, MAX_SPEED)
    }

    private fun SlimeScene.resolveSphericalBoundary(bounce: Float) {
        val radius = MOVEMENT_RADIUS
        val distance =
            sqrt(
                positionX * positionX +
                    positionY * positionY +
                    positionZ * positionZ,
            )
        if (distance <= radius) return

        val normalX = positionX / distance
        val normalY = positionY / distance
        val normalZ = positionZ / distance
        positionX = normalX * radius
        positionY = normalY * radius
        positionZ = normalZ * radius

        val outwardVelocity =
            velocityX * normalX +
                velocityY * normalY +
                velocityZ * normalZ
        if (outwardVelocity <= 0f) return

        velocityX -= normalX * outwardVelocity * (1f + bounce)
        velocityY -= normalY * outwardVelocity * (1f + bounce)
        velocityZ -= normalZ * outwardVelocity * (1f + bounce)
        impactAmount =
            max(
                impactAmount,
                (outwardVelocity / IMPACT_SPEED_FOR_FULL_SQUISH).coerceIn(0.25f, 1f),
            )
        impactNormalX = normalX
        impactNormalY = normalY
        impactNormalZ = normalZ
        if (outwardVelocity >= HAPPY_IMPACT_SPEED) {
            setExpression(SlimeExpression.HAPPY, HAPPY_REACTION_SECONDS)
        }
        emitInteraction(
            CreatureInteraction.ShellContact(
                sector = shellSector(normalX, normalY, normalZ),
                strength =
                    (outwardVelocity / IMPACT_SPEED_FOR_FULL_SQUISH)
                        .coerceIn(0f, 1f),
                normalX = normalX,
                normalY = normalY,
                normalZ = normalZ,
                hasReliablePosition = true,
            ),
        )
    }

    private fun SlimeScene.shellSector(
        normalX: Float,
        normalY: Float,
        normalZ: Float,
    ): ShellSector =
        when {
            abs(normalX) >= max(abs(normalY), abs(normalZ)) -> {
                if (normalX < 0f) ShellSector.LEFT else ShellSector.RIGHT
            }

            abs(normalY) >= abs(normalZ) -> {
                if (normalY < 0f) ShellSector.TOP else ShellSector.BOTTOM
            }

            normalZ < 0f -> {
                ShellSector.BACK
            }

            else -> {
                ShellSector.FRONT
            }
        }
}

private const val MIN_BOUNCE = 0.2f
private const val MAX_BOUNCE = 0.9f
private const val PHONE_ACCELERATION_RESPONSE = 88f
private const val IMPACT_SPEED_FOR_FULL_SQUISH = 9f
private const val HAPPY_IMPACT_SPEED = 4f
private const val HAPPY_REACTION_SECONDS = 0.9f
private const val FLOAT_Y_FREQUENCY_RATIO = 0.73f
private const val FLOAT_Y_STRENGTH = 0.72f
private const val FLOAT_Z_FREQUENCY_RATIO = 0.57f
