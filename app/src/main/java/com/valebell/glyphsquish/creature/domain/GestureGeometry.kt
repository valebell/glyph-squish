package com.valebell.glyphsquish.creature.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal fun circleAngle(contact: CreatureInteraction.ShellContact): Double? {
    if (!contact.strength.isFinite()) return null
    return circleAngle(contact.normalX, contact.normalY, contact.normalZ)
}

internal fun circleAngle(
    x: Float,
    y: Float,
    z: Float,
): Double? {
    if (
        !x.isFinite() || !y.isFinite() || !z.isFinite()
    ) {
        return null
    }
    val planarMagnitude = hypot(x.toDouble(), y.toDouble())
    if (planarMagnitude < MINIMUM_PLANAR_NORMAL_MAGNITUDE) return null
    if (abs(z.toDouble()) >= planarMagnitude * DEPTH_DOMINANCE_RATIO) return null
    return atan2(y.toDouble(), x.toDouble())
}

internal fun CreatureInteraction.CirclePosition.planarRadiusSquared(): Float = x * x + y * y

internal fun crossesCircleCenter(
    from: CreatureInteraction.CirclePosition,
    to: CreatureInteraction.CirclePosition,
): Boolean {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared == 0f) return false
    val nearest = (-(from.x * dx + from.y * dy) / lengthSquared).coerceIn(0f, 1f)
    val x = from.x + dx * nearest
    val y = from.y + dy * nearest
    return x * x + y * y < MINIMUM_CIRCLE_TRACKING_RADIUS * MINIMUM_CIRCLE_TRACKING_RADIUS
}

internal fun diagonalEndpoint(contact: CreatureInteraction.ShellContact): DiagonalEndpoint? {
    val horizontal = abs(contact.normalX)
    val vertical = abs(contact.normalY)
    if (max(horizontal, vertical) < MINIMUM_DIAGONAL_COMPONENT) return null
    if (min(horizontal, vertical) / max(horizontal, vertical) < MINIMUM_DIAGONAL_BALANCE) return null
    if (abs(contact.normalZ) > max(horizontal, vertical)) return null
    return when {
        contact.normalX >= 0f && contact.normalY < 0f -> DiagonalEndpoint.NORTH_EAST
        contact.normalX < 0f && contact.normalY < 0f -> DiagonalEndpoint.NORTH_WEST
        contact.normalX >= 0f -> DiagonalEndpoint.SOUTH_EAST
        else -> DiagonalEndpoint.SOUTH_WEST
    }
}

internal fun ShellSector.isOpposite(other: ShellSector): Boolean =
    when (this) {
        ShellSector.LEFT -> other == ShellSector.RIGHT
        ShellSector.TOP -> other == ShellSector.BOTTOM
        ShellSector.RIGHT -> other == ShellSector.LEFT
        ShellSector.BOTTOM -> other == ShellSector.TOP
        ShellSector.FRONT -> other == ShellSector.BACK
        ShellSector.BACK -> other == ShellSector.FRONT
    }

internal fun signedAngularDelta(
    from: Double,
    to: Double,
): Double {
    var delta = to - from
    while (delta <= -PI) delta += TWO_PI
    while (delta > PI) delta -= TWO_PI
    return delta
}

internal sealed interface MotionPrimitive {
    val step: GestureStep
        get() =
            when (this) {
                Circle -> GestureStep.CIRCLE
                is Bounce -> GestureStep.BOUNCE
                is Diagonal -> GestureStep.DIAGONAL
                GlyphButton -> GestureStep.GLYPH_BUTTON
            }

    data object Circle : MotionPrimitive

    data class Bounce(
        val from: ShellSector,
        val to: ShellSector,
    ) : MotionPrimitive

    data class Diagonal(
        val axis: DiagonalAxis,
    ) : MotionPrimitive

    data object GlyphButton : MotionPrimitive
}

internal enum class DiagonalAxis {
    RISING,
    FALLING,
}

internal enum class CircleUpdate {
    STARTED,
    TRACKING,
    COMPLETED,
    INVALID,
}

internal enum class DiagonalEndpoint(
    val axis: DiagonalAxis,
) {
    NORTH_EAST(DiagonalAxis.RISING),
    SOUTH_WEST(DiagonalAxis.RISING),
    NORTH_WEST(DiagonalAxis.FALLING),
    SOUTH_EAST(DiagonalAxis.FALLING),
    ;

    fun isOpposite(other: DiagonalEndpoint): Boolean = axis == other.axis && this != other
}

private const val MINIMUM_PLANAR_NORMAL_MAGNITUDE = 0.05
private const val DEPTH_DOMINANCE_RATIO = 1.0
private const val MINIMUM_DIAGONAL_COMPONENT = 0.48f
private const val MINIMUM_DIAGONAL_BALANCE = 0.58f

internal const val TWO_PI = PI * 2.0
internal const val MINIMUM_CIRCLE_TRACKING_RADIUS = 0.45f
internal val DIRECTION_LOCK_RADIANS = Math.toRadians(45.0)
internal val MAXIMUM_ANGULAR_STEP_RADIANS = Math.toRadians(135.0)
