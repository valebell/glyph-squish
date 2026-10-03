package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm

data class SlimeSnapshot(
    val motion: SlimeMotionSnapshot,
    val appearance: SlimeAppearanceSnapshot,
    val activeButtonAction: SlimeButtonAction?,
    val activeSemanticAnimation: SlimeAnimationCue?,
    val isPressed: Boolean,
    val trailCount: Int,
) {
    val x get() = motion.x
    val y get() = motion.y
    val z get() = motion.z
    val velocityX get() = motion.velocityX
    val velocityY get() = motion.velocityY
    val velocityZ get() = motion.velocityZ
    val expression get() = appearance.expression
    val form get() = appearance.form
    val isFormTransitioning get() = appearance.isFormTransitioning
}

data class SlimeMotionSnapshot(
    val x: Float,
    val y: Float,
    val z: Float,
    val velocityX: Float,
    val velocityY: Float,
    val velocityZ: Float,
)

data class SlimeAppearanceSnapshot(
    val expression: SlimeExpression,
    val form: CreatureForm,
    val isFormTransitioning: Boolean,
)

data class RenderBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal data class Deformation(
    val widthDelta: Int,
    val heightDelta: Int,
)

internal data class Projection(
    val centerX: Float,
    val centerY: Float,
    val depth: Float,
)

internal data class PlanarVector(
    val x: Float,
    val y: Float,
)

internal data class BurstPixel(
    val compressedX: Float,
    val compressedY: Float,
    val targetX: Float,
    val targetY: Float,
    val scatterX: Float,
    val scatterY: Float,
    val scatterZ: Float,
)

internal data class ThunderboltParticle(
    val sourceX: Float,
    val sourceY: Float,
    val rimX: Float,
    val rimY: Float,
)

internal class ActiveFormTransition(
    val source: SlimePhenotype,
    var target: SlimePhenotype,
    val originX: Float,
    val originY: Float,
    val originZ: Float,
    var elapsedSeconds: Float = 0f,
) {
    // Form geometry stays fixed during a transition; scaling is applied when drawing.
    val sourcePoints =
        Array(FORM_PARTICLE_COUNT) { index ->
            SlimeFormGeometry.particle(source.form, index, FORM_PARTICLE_COUNT)
        }
    val targetPoints =
        Array(FORM_PARTICLE_COUNT) { index ->
            SlimeFormGeometry.particle(target.form, index, FORM_PARTICLE_COUNT)
        }
}

internal data class TrailParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var velocityX: Float,
    var velocityY: Float,
    var velocityZ: Float,
    val sequence: Int,
) {
    var ageSeconds = 0f
}
