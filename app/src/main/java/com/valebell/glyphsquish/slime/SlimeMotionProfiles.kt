package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm

internal data class MotionProfile(
    val tiltAcceleration: Float = 58f,
    val centering: Float = 1f,
    val depthCentering: Float = 2.4f,
    val depthTiltRange: Float = 1.8f,
    val damping: Float = 1.35f,
    val bounce: Float = 0.46f,
    val idleDrift: IdleDrift = IdleDrift(),
) {
    companion object {
        fun forExpression(expression: SlimeExpression): MotionProfile = profiles.getValue(expression)

        private val profiles =
            SlimeExpression.entries.associateWith { expression ->
                when (expression) {
                    SlimeExpression.CURIOUS -> {
                        MotionProfile()
                    }

                    SlimeExpression.HAPPY -> {
                        MotionProfile(
                            tiltAcceleration = 72f,
                            centering = 0.8f,
                            depthCentering = 2f,
                            depthTiltRange = 2.2f,
                            damping = 1.05f,
                            bounce = 0.68f,
                            idleDrift = IdleDrift(acceleration = 1.2f, depthAcceleration = 1.5f, periodSeconds = 4.7f),
                        )
                    }

                    SlimeExpression.SURPRISED -> {
                        MotionProfile(
                            tiltAcceleration = 64f,
                            centering = 0.9f,
                            depthCentering = 2.1f,
                            depthTiltRange = 2f,
                            damping = 1.15f,
                            bounce = 0.55f,
                            idleDrift = IdleDrift(acceleration = 1f, depthAcceleration = 1.3f, periodSeconds = 5.2f),
                        )
                    }

                    SlimeExpression.FOCUSED -> {
                        MotionProfile(
                            tiltAcceleration = 70f,
                            centering = 0.75f,
                            depthCentering = 1.8f,
                            damping = 0.9f,
                            bounce = 0.52f,
                            idleDrift = IdleDrift(acceleration = 0.45f, depthAcceleration = 0.6f, periodSeconds = 6f),
                        )
                    }

                    SlimeExpression.DIZZY -> {
                        MotionProfile(
                            tiltAcceleration = 52f,
                            centering = 0.55f,
                            depthCentering = 1.5f,
                            depthTiltRange = 2.2f,
                            damping = 0.78f,
                            bounce = 0.72f,
                            idleDrift = IdleDrift(acceleration = 1.4f, depthAcceleration = 1.7f, periodSeconds = 3.8f),
                        )
                    }
                }
            }
    }
}

internal data class PlanarMotionProfile(
    val axisResponse: Float = 1f,
    val diagonalResponse: Float = 1f,
    val crossAxisRetention: Float = 1f,
    val centeringScale: Float = 1f,
    val dampingScale: Float = 1f,
    val floatScale: Float = 1f,
    val maxSpeed: Float = 17f,
) {
    companion object {
        fun forForm(form: CreatureForm): PlanarMotionProfile = profiles.getValue(form)

        private val profiles =
            CreatureForm.entries.associateWith { form ->
                when (form) {
                    CreatureForm.BLOB -> {
                        PlanarMotionProfile()
                    }

                    CreatureForm.SHARK -> {
                        PlanarMotionProfile(
                            axisResponse = 1.34f,
                            diagonalResponse = 0.96f,
                            centeringScale = 0.9f,
                            dampingScale = 0.86f,
                            floatScale = 0.75f,
                            maxSpeed = 20f,
                        )
                    }

                    CreatureForm.CAT -> {
                        PlanarMotionProfile(
                            axisResponse = 1.02f,
                            diagonalResponse = 1.02f,
                        )
                    }

                    CreatureForm.STAR -> {
                        PlanarMotionProfile(
                            axisResponse = 1.04f,
                            diagonalResponse = 0.84f,
                            crossAxisRetention = 0.24f,
                            centeringScale = 1.15f,
                            dampingScale = 1.25f,
                            floatScale = 0.28f,
                            maxSpeed = 14f,
                        )
                    }

                    CreatureForm.BUTTERFLY -> {
                        PlanarMotionProfile(
                            axisResponse = 0.84f,
                            diagonalResponse = 1.42f,
                            centeringScale = 0.95f,
                            dampingScale = 0.94f,
                            floatScale = 0.9f,
                            maxSpeed = 18.5f,
                        )
                    }
                }
            }
    }
}

internal data class IdleDrift(
    val acceleration: Float = 0.8f,
    val depthAcceleration: Float = 1.1f,
    val periodSeconds: Float = 6.2f,
)
