package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.sqrt

internal fun litWidth(frame: IntArray): Int {
    val litColumns =
        frame.indices
            .filter { index -> frame[index] > 0 }
            .map { index -> index % SlimeEngine.MATRIX_SIZE }
    return litColumns.max() - litColumns.min() + 1
}

internal fun litHeight(frame: IntArray): Int {
    val litRows =
        frame.indices
            .filter { index -> frame[index] > 0 }
            .map { index -> index / SlimeEngine.MATRIX_SIZE }
    return litRows.max() - litRows.min() + 1
}

internal fun validGlyphIndices(): Set<Int> =
    (0 until SlimeEngine.PIXEL_COUNT)
        .filter { index ->
            val x = index % SlimeEngine.MATRIX_SIZE
            val y = index / SlimeEngine.MATRIX_SIZE
            val deltaX = x - SlimeEngine.MATRIX_SIZE / 2
            val deltaY = y - SlimeEngine.MATRIX_SIZE / 2
            deltaX * deltaX + deltaY * deltaY <= GLYPH_RADIUS_SQUARED
        }.toSet()

internal fun isGlyphRimIndex(index: Int): Boolean {
    val x = index % SlimeEngine.MATRIX_SIZE
    val y = index / SlimeEngine.MATRIX_SIZE
    val center = SlimeEngine.MATRIX_SIZE / 2
    val deltaX = x - center
    val deltaY = y - center
    if (deltaX * deltaX + deltaY * deltaY > GLYPH_RADIUS_SQUARED) return false
    return (-1..1).any { offsetY ->
        (-1..1).any { offsetX ->
            val neighborX = deltaX + offsetX
            val neighborY = deltaY + offsetY
            neighborX * neighborX + neighborY * neighborY > GLYPH_RADIUS_SQUARED
        }
    }
}

internal fun largestBrightComponent(frame: IntArray): Int {
    val visited = BooleanArray(frame.size)
    var largest = 0
    frame.indices.forEach { start ->
        if (visited[start] || frame[start] < BODY_SHADOW) return@forEach
        val count = brightComponentSize(frame, visited, start)
        largest = maxOf(largest, count)
    }
    return largest
}

internal fun earlyPhoneMovement(
    form: CreatureForm,
    x: Float,
    y: Float,
): SlimeSnapshot {
    val engine = engineFor(form)
    engine.setPhoneAcceleration(x, y, 0f)
    repeat(EARLY_MOVEMENT_STEPS) {
        engine.step(EARLY_MOVEMENT_STEP_SECONDS)
    }
    return engine.snapshot()
}

internal fun earlyTiltMovement(
    form: CreatureForm,
    x: Float,
    y: Float,
): SlimeSnapshot {
    val engine = engineFor(form)
    engine.setTilt(x, y)
    repeat(EARLY_MOVEMENT_STEPS) {
        engine.step(EARLY_MOVEMENT_STEP_SECONDS)
    }
    return engine.snapshot()
}

internal fun engineFor(form: CreatureForm): SlimeEngine =
    SlimeEngine(
        initialPhenotype = SlimePhenotype.DEFAULT.copy(form = form),
    )

internal fun axisSpeed(
    snapshot: SlimeSnapshot,
    x: Float,
    y: Float,
): Float = abs(snapshot.velocityX * x + snapshot.velocityY * y)

internal fun planarSpeed(snapshot: SlimeSnapshot): Float =
    sqrt(
        snapshot.velocityX * snapshot.velocityX +
            snapshot.velocityY * snapshot.velocityY,
    )

internal const val INVERSE_SQRT_TWO = 0.70710677f
internal const val EARLY_MOVEMENT_STEPS = 3
internal const val EARLY_MOVEMENT_STEP_SECONDS = 1f / 60f
internal const val BODY_SHADOW = 170
internal const val BODY_MIDTONE = 215
internal const val BODY_HIGHLIGHT = 255
internal const val RESTING_EYE_PIXELS = 4
internal const val MIN_READABLE_CORE_PIXELS = 30
internal const val GLYPH_RADIUS_SQUARED = 156
internal const val THUNDERBOLT_TEST_MAX_STEPS = 180
internal const val ACTION_TEST_STEP_SECONDS = 1f / 60f
internal const val MIN_THUNDERBOLT_RIM_PIXELS = 12

val BODY_RAMP = setOf(BODY_SHADOW, BODY_MIDTONE, BODY_HIGHLIGHT)
val EXPECTED_BODY_PIXELS =
    mapOf(
        CreatureForm.BLOB to 92,
        CreatureForm.SHARK to 71,
        CreatureForm.CAT to 91,
        CreatureForm.STAR to 77,
        CreatureForm.BUTTERFLY to 85,
    )

private fun brightComponentSize(
    frame: IntArray,
    visited: BooleanArray,
    start: Int,
): Int {
    val queue = ArrayDeque<Int>()
    queue += start
    visited[start] = true
    var count = 0
    while (queue.isNotEmpty()) {
        val index = queue.removeFirst()
        count += 1
        val x = index % SlimeEngine.MATRIX_SIZE
        val y = index / SlimeEngine.MATRIX_SIZE
        listOf(
            x - 1 to y,
            x + 1 to y,
            x to y - 1,
            x to y + 1,
        ).forEach { (neighborX, neighborY) ->
            if (
                neighborX !in 0 until SlimeEngine.MATRIX_SIZE ||
                neighborY !in 0 until SlimeEngine.MATRIX_SIZE
            ) {
                return@forEach
            }
            val neighbor = neighborY * SlimeEngine.MATRIX_SIZE + neighborX
            if (!visited[neighbor] && frame[neighbor] >= BODY_SHADOW) {
                visited[neighbor] = true
                queue += neighbor
            }
        }
    }
    return count
}
