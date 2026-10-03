package com.valebell.glyphsquish.slime

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

internal fun segmentProgress(
    value: Float,
    start: Float,
    end: Float,
): Float = ((value - start) / (end - start)).coerceIn(0f, 1f)

internal fun smoothStep(value: Float): Float = value * value * (3f - 2f * value)

internal fun smoothSegment(
    value: Float,
    start: Float,
    end: Float,
): Float = smoothStep(segmentProgress(value, start, end))

internal fun easeInCubic(value: Float): Float = value * value * value

internal fun easeOutCubic(value: Float): Float {
    val inverse = 1f - value
    return 1f - inverse * inverse * inverse
}

internal fun lerp(
    start: Float,
    end: Float,
    progress: Float,
): Float = start + (end - start) * progress

internal fun actionDuration(action: SlimeButtonAction): Float =
    when (action) {
        SlimeButtonAction.BURST -> BURST_ACTION_SECONDS
        SlimeButtonAction.HELIX -> HELIX_ACTION_SECONDS
        SlimeButtonAction.ECHO -> ECHO_ACTION_SECONDS
        SlimeButtonAction.RICOCHET -> RICOCHET_ACTION_SECONDS
        SlimeButtonAction.THUNDERBOLT -> THUNDERBOLT_ACTION_SECONDS
    }

internal fun project(
    x: Float,
    y: Float,
    z: Float,
): Projection {
    val depth = (z / MOVEMENT_RADIUS).coerceIn(-1f, 1f)
    return Projection(
        centerX = MATRIX_CENTER + x,
        centerY = MATRIX_CENTER + y,
        depth = depth,
    )
}

internal fun setFramePixel(
    frame: IntArray,
    x: Int,
    y: Int,
    brightness: Int,
) {
    if (!isMatrixPixel(x, y)) return
    val index = indexOf(x, y)
    frame[index] = max(frame[index], brightness.coerceIn(PIXEL_OFF, 255))
}

internal fun isMatrixPixel(
    x: Int,
    y: Int,
): Boolean {
    if (!isInBounds(x, y)) return false
    val deltaX = x - MATRIX_CENTER
    val deltaY = y - MATRIX_CENTER
    return deltaX * deltaX + deltaY * deltaY <= MATRIX_RADIUS_SQUARED
}

internal fun isInBounds(
    x: Int,
    y: Int,
): Boolean = x in 0 until MATRIX_SIZE && y in 0 until MATRIX_SIZE

internal val MATRIX_PIXEL_INDICES =
    (0 until PIXEL_COUNT).filter { isMatrixPixel(it % MATRIX_SIZE, it / MATRIX_SIZE) }

internal fun indexOf(
    x: Int,
    y: Int,
): Int = y * MATRIX_SIZE + x

internal fun applyDeadZone(
    value: Float,
    deadZone: Float,
): Float {
    if (!value.isFinite() || abs(value) <= deadZone) return 0f
    val magnitude = (abs(value).coerceAtMost(1f) - deadZone) / (1f - deadZone)
    return magnitude * value.sign
}

internal fun shapePlanarInput(
    x: Float,
    y: Float,
    profile: PlanarMotionProfile,
): PlanarVector {
    val dominantMagnitude = max(abs(x), abs(y))
    if (dominantMagnitude == 0f) return PlanarVector(0f, 0f)

    val diagonalAmount = min(abs(x), abs(y)) / dominantMagnitude
    val response =
        lerp(
            profile.axisResponse,
            profile.diagonalResponse,
            smoothStep(diagonalAmount),
        )
    val minorAxisRetention =
        lerp(profile.crossAxisRetention, 1f, diagonalAmount)
    return when {
        abs(x) > abs(y) -> PlanarVector(x * response, y * response * minorAxisRetention)
        abs(y) > abs(x) -> PlanarVector(x * response * minorAxisRetention, y * response)
        else -> PlanarVector(x * response, y * response)
    }
}

internal fun approach(
    current: Float,
    target: Float,
    amount: Float,
): Float =
    when {
        current < target -> min(current + amount, target)
        current > target -> max(current - amount, target)
        else -> current
    }

internal fun oddDimension(value: Int): Int {
    var dimension = value.coerceIn(MIN_ORB_DIAMETER, MAX_ORB_DIAMETER)
    if (dimension % 2 == 0) {
        dimension += if (dimension < MAX_ORB_DIAMETER) 1 else -1
    }
    return dimension
}

internal fun effectiveWidthScale(value: SlimePhenotype): Float =
    value.widthScale.coerceIn(MIN_FORM_ASPECT_SCALE, MAX_FORM_ASPECT_SCALE)

internal fun effectiveHeightScale(value: SlimePhenotype): Float =
    value.heightScale.coerceIn(MIN_FORM_ASPECT_SCALE, MAX_FORM_ASPECT_SCALE)

internal fun formDimension(scale: Float): Int = oddDimension((BASE_FORM_DIAMETER * scale).roundToInt())

/** Stable row-major pixels: particle order controls deterministic scatter patterns. */
internal fun forEachFormPixel(
    phenotype: SlimePhenotype,
    visit: (Int, Int, Int) -> Unit,
) {
    val diameter = (ORB_DIAMETER * SHAPE_SCALE).roundToInt()
    val width = oddDimension((diameter * effectiveWidthScale(phenotype)).roundToInt())
    val height = oddDimension((diameter * effectiveHeightScale(phenotype)).roundToInt())
    var index = 0
    for (y in -(height / 2)..(height / 2)) {
        for (x in -(width / 2)..(width / 2)) {
            if (SlimeFormGeometry.contains(phenotype.form, x / (width / 2f), y / (height / 2f))) {
                visit(x, y, index++)
            }
        }
    }
}

private const val MIN_ORB_DIAMETER = 5
private const val MAX_ORB_DIAMETER = 17
private const val MIN_FORM_ASPECT_SCALE = 0.88f
private const val MAX_FORM_ASPECT_SCALE = 1.12f
private const val BURST_ACTION_SECONDS = BURST_ACTION_MILLIS / 1_000f
private const val THUNDERBOLT_ACTION_SECONDS = THUNDERBOLT_ACTION_MILLIS / 1_000f
private val BASE_FORM_DIAMETER = oddDimension((ORB_DIAMETER * SHAPE_SCALE).roundToInt())
