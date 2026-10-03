package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import kotlin.math.sqrt

/** Mutable simulation state. Owned by one engine and confined to its calling thread. */
internal class SlimeScene(
    initialPhenotype: SlimePhenotype,
    val shadeBody: Boolean,
    private val interactionHandler: (CreatureInteraction) -> SlimeButtonAction?,
) {
    var phenotype = initialPhenotype
    val animationCoordinator = SlimeAnimationCoordinator()
    var expression = SlimeExpression.CURIOUS
    var expressionRemaining = 0f
    var activeButtonAction: SlimeButtonAction? = null
    var buttonActionElapsed = 0f
    var buttonActionRemaining = 0f
    var buttonActionPhase = -1
    var actionOriginX = 0f
    var actionOriginY = 0f
    var actionOriginZ = 0f
    var buttonActionDirection = 1f
    var tiltX = 0f
    var tiltY = 0f
    var tiltZ = 0f
    var phoneAccelerationX = 0f
    var phoneAccelerationY = 0f
    var phoneAccelerationZ = 0f
    var positionX = 0f
    var positionY = 0f
    var positionZ = 0f
    var velocityX = 0f
    var velocityY = 0f
    var velocityZ = 0f
    var elapsedSeconds = 0f
    var pressAmount = 0f
    var isPressed = false
    var impactAmount = 0f
    var impactNormalX = 0f
    var impactNormalY = 1f
    var impactNormalZ = 0f
    var trailEmissionSeconds = TRAIL_EMISSION_INTERVAL_SECONDS
    var trailSequence = 0
    val trailParticles = mutableListOf<TrailParticle>()
    val burstPixels = mutableListOf<BurstPixel>()
    val thunderboltParticles = mutableListOf<ThunderboltParticle>()
    var formTransition: ActiveFormTransition? = null
    var pendingButtonAction: SlimeButtonAction? = null

    val performanceInProgress: Boolean
        get() = activeButtonAction != null || pendingButtonAction != null || formTransition != null

    fun cancelTransientBodyAnimation() {
        activeButtonAction = null
        buttonActionElapsed = 0f
        buttonActionRemaining = 0f
        buttonActionPhase = -1
        burstPixels.clear()
        thunderboltParticles.clear()
        trailParticles.clear()
        velocityX = 0f
        velocityY = 0f
        velocityZ = 0f
    }

    fun setExpression(
        newExpression: SlimeExpression,
        duration: Float,
    ) {
        expression = newExpression
        expressionRemaining = duration
    }

    fun speed(): Float =
        sqrt(
            velocityX * velocityX +
                velocityY * velocityY +
                velocityZ * velocityZ,
        )

    fun limitVelocity() {
        val speed = speed()
        if (speed <= MAX_SPEED) return

        val scale = MAX_SPEED / speed
        velocityX *= scale
        velocityY *= scale
        velocityZ *= scale
    }

    fun emitInteraction(interaction: CreatureInteraction) {
        // Animation contacts and the remainder of a frame that queued a move are not gestures.
        if (performanceInProgress) return
        if (interaction is CreatureInteraction.ShellContact) {
            animationCoordinator.onShellContact(interaction.sector)
        }
        // The runtime requests automatic playback only for a move's first unlock.
        interactionHandler(interaction)?.let { unlockedMove ->
            pendingButtonAction = unlockedMove
        }
    }

    fun actionCompressionAmount(): Float =
        when (activeButtonAction) {
            SlimeButtonAction.BURST -> {
                when {
                    buttonActionElapsed < BURST_ANTICIPATION_END -> {
                        0f
                    }

                    buttonActionElapsed < BURST_COMPRESSION_END -> {
                        smoothSegment(buttonActionElapsed, BURST_ANTICIPATION_END, BURST_COMPRESSION_END)
                    }

                    buttonActionElapsed < BURST_EXPLOSION_START -> {
                        1f
                    }

                    else -> {
                        0f
                    }
                }
            }

            SlimeButtonAction.ECHO -> {
                if (buttonActionElapsed < ECHO_CROUCH_END) {
                    smoothSegment(buttonActionElapsed, 0f, ECHO_CROUCH_END)
                } else {
                    0f
                }
            }

            else -> {
                0f
            }
        }

    fun isBurstParticlePhase(): Boolean =
        activeButtonAction == SlimeButtonAction.BURST &&
            buttonActionElapsed >= BURST_EXPLOSION_START &&
            buttonActionElapsed < BURST_LOCK_END

    fun isThunderboltParticlePhase(): Boolean =
        activeButtonAction == SlimeButtonAction.THUNDERBOLT &&
            buttonActionElapsed >= THUNDERBOLT_PARTICLE_BREAK_START &&
            buttonActionElapsed < THUNDERBOLT_FORM_REFORM_END

    fun setActionPhase(
        phase: Int,
        actionExpression: SlimeExpression,
    ): Boolean {
        if (buttonActionPhase == phase) return false

        buttonActionPhase = phase
        setExpression(
            actionExpression,
            buttonActionRemaining + ACTION_EXPRESSION_PADDING_SECONDS,
        )
        return true
    }

    fun markActionImpact(
        normalX: Float,
        normalY: Float,
        normalZ: Float,
    ) {
        val length = sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ)
        impactNormalX = normalX / length
        impactNormalY = normalY / length
        impactNormalZ = normalZ / length
        impactAmount = 1f
    }

    fun setActionPosition(
        x: Float,
        y: Float,
        z: Float,
    ) {
        val distance = sqrt(x * x + y * y + z * z)
        if (distance <= MOVEMENT_RADIUS) {
            positionX = x
            positionY = y
            positionZ = z
            return
        }

        val scale = MOVEMENT_RADIUS / distance
        positionX = x * scale
        positionY = y * scale
        positionZ = z * scale
    }
}

private const val ACTION_EXPRESSION_PADDING_SECONDS = 0.08f
private const val BURST_COMPRESSION_END = 0.33f
