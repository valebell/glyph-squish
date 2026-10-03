package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal data class SlimeFormPoint(
    val x: Float,
    val y: Float,
)

internal object SlimeFormGeometry {
    const val CANONICAL_SIZE = 11

    private const val MASK_SCALE = CANONICAL_SIZE / 2f
    private const val ANGLE_SAMPLE_COUNT = 720
    private const val RADIUS_SAMPLE_STEP = 0.01f
    private const val MAX_RADIAL_DISTANCE = 1.42f
    private const val LANDMARK_PARTICLE_COUNT = 16
    private const val PARTICLE_NUDGE_SCALE = 0.9f
    private const val PARTICLE_NUDGE_ATTEMPTS = 12
    private const val OUTSIDE_NORMALIZED_RADIUS = 2f
    private const val GOLDEN_ANGLE = 2.3999631f
    private val TWO_PI = 2f * PI.toFloat()

    private val formMasks =
        mapOf(
            CreatureForm.BLOB to
                mask(
                    """
                    ...####....
                    ..#######..
                    .#########.
                    .#########.
                    ###########
                    ###########
                    ###########
                    ###########
                    .#########.
                    ..#######..
                    ....###....
                    """,
                ),
            CreatureForm.SHARK to
                mask(
                    """
                    ....##.....
                    ...###.....
                    ..#######..
                    .########..
                    ###########
                    #########..
                    ###########
                    .########..
                    ..#######..
                    ...#####...
                    ...........
                    """,
                ),
            CreatureForm.CAT to
                mask(
                    """
                    ..##...##..
                    ..###.###..
                    .#########.
                    .#########.
                    ###########
                    ###########
                    ###########
                    .#########.
                    .#########.
                    ..#######..
                    ...#####...
                    """,
                ),
            CreatureForm.STAR to
                mask(
                    """
                    ....###....
                    ....###....
                    ...#####...
                    .#########.
                    ###########
                    ###########
                    .#########.
                    ..#######..
                    .#########.
                    .###...###.
                    ..##...##..
                    """,
                ),
            CreatureForm.BUTTERFLY to
                mask(
                    """
                    ..##...##..
                    .####.####.
                    ###########
                    .#########.
                    ..#######..
                    ..#######..
                    ..#######..
                    .#########.
                    ###########
                    .####.####.
                    ..##...##..
                    """,
                ),
        )

    private val radialRadii =
        Array(CreatureForm.entries.size) { formIndex ->
            val form = CreatureForm.entries[formIndex]
            lazy {
                FloatArray(ANGLE_SAMPLE_COUNT) { angleIndex ->
                    sampledRadius(
                        form = form,
                        angle = angleIndex.toFloat() / ANGLE_SAMPLE_COUNT * TWO_PI,
                    )
                }
            }
        }

    fun contains(
        form: CreatureForm,
        x: Float,
        y: Float,
    ): Boolean {
        if (!x.isFinite() || !y.isFinite()) return false
        val maskX = maskCoordinate(x)
        val maskY = maskCoordinate(y)
        if (maskX !in 0 until CANONICAL_SIZE || maskY !in 0 until CANONICAL_SIZE) {
            return false
        }
        return formMasks.getValue(form)[maskY * CANONICAL_SIZE + maskX]
    }

    fun point(
        form: CreatureForm,
        angle: Float,
        radialFraction: Float,
    ): SlimeFormPoint {
        var radius = radiusAt(form, angle) * radialFraction.coerceIn(0f, 1f)
        repeat(PARTICLE_NUDGE_ATTEMPTS) {
            val point =
                SlimeFormPoint(
                    x = cos(angle) * radius,
                    y = sin(angle) * radius,
                )
            if (contains(form, point.x, point.y)) return point
            radius *= PARTICLE_NUDGE_SCALE
        }
        return SlimeFormPoint(0f, 0f)
    }

    fun particle(
        form: CreatureForm,
        index: Int,
        count: Int,
    ): SlimeFormPoint {
        require(index in 0 until count) { "Particle index must be within the particle count" }
        require(count > LANDMARK_PARTICLE_COUNT) {
            "Particle count must leave room for interior samples"
        }
        val angle: Float
        val radialFraction: Float
        if (index < LANDMARK_PARTICLE_COUNT) {
            angle = -PI.toFloat() / 2f + index.toFloat() / LANDMARK_PARTICLE_COUNT * TWO_PI
            radialFraction = 0.98f
        } else {
            val interiorIndex = index - LANDMARK_PARTICLE_COUNT
            val interiorCount = count - LANDMARK_PARTICLE_COUNT
            angle = -PI.toFloat() / 2f + interiorIndex * GOLDEN_ANGLE
            radialFraction = sqrt((interiorIndex + 0.5f) / interiorCount) * 0.94f
        }
        return point(form, angle, radialFraction)
    }

    fun normalizedRadius(
        form: CreatureForm,
        x: Float,
        y: Float,
    ): Float {
        if (!contains(form, x, y)) return OUTSIDE_NORMALIZED_RADIUS
        val distance = sqrt(x * x + y * y)
        if (distance == 0f) return 0f
        return (distance / radiusAt(form, kotlin.math.atan2(y, x))).coerceAtMost(1f)
    }

    fun eyeOffset(form: CreatureForm): Float =
        when (form) {
            CreatureForm.BLOB -> -0.16f

            CreatureForm.SHARK,
            CreatureForm.CAT,
            CreatureForm.STAR,
            CreatureForm.BUTTERFLY,
            -> -0.12f
        }

    fun eyeSpacing(form: CreatureForm): Float =
        when (form) {
            CreatureForm.BLOB,
            CreatureForm.CAT,
            CreatureForm.STAR,
            -> 0.4f

            CreatureForm.SHARK,
            CreatureForm.BUTTERFLY,
            -> 0.25f
        }

    fun radiusAt(
        form: CreatureForm,
        angle: Float,
    ): Float {
        val normalizedAngle = ((angle % TWO_PI) + TWO_PI) % TWO_PI
        val index =
            (normalizedAngle / TWO_PI * ANGLE_SAMPLE_COUNT)
                .roundToInt() % ANGLE_SAMPLE_COUNT
        return radialRadii[form.ordinal].value[index]
    }

    private fun sampledRadius(
        form: CreatureForm,
        angle: Float,
    ): Float {
        val directionX = cos(angle)
        val directionY = sin(angle)
        var farthestInside = 0f
        var radius = 0f
        while (radius <= MAX_RADIAL_DISTANCE) {
            if (contains(form, directionX * radius, directionY * radius)) {
                farthestInside = radius
            }
            radius += RADIUS_SAMPLE_STEP
        }
        return farthestInside
    }

    private fun maskCoordinate(value: Float): Int = floor((value + 1f) * MASK_SCALE).toInt()

    private fun mask(pixels: String): BooleanArray {
        val rows = pixels.trimIndent().lines()
        require(rows.size == CANONICAL_SIZE)
        require(rows.all { it.length == CANONICAL_SIZE })
        return BooleanArray(CANONICAL_SIZE * CANONICAL_SIZE) { index ->
            rows[index / CANONICAL_SIZE][index % CANONICAL_SIZE] == '#'
        }
    }
}
