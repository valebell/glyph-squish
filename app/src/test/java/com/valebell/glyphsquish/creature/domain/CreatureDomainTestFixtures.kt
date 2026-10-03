package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlin.math.cos
import kotlin.math.sin

internal fun circle(
    clockwise: Boolean = false,
    strength: Float = 0.8f,
    startDegrees: Double = 11.0,
): List<CreatureInteraction.ShellContact> {
    val direction = if (clockwise) -1.0 else 1.0
    return angularTrace(
        angles =
            (0..16).map { step ->
                startDegrees + direction * step * 22.5
            },
        strength = strength,
    )
}

internal fun angularTrace(
    angles: List<Double>,
    strength: Float = 0.8f,
    sector: ShellSector? = null,
): List<CreatureInteraction.ShellContact> =
    angles.map { angle ->
        val radians = Math.toRadians(angle)
        contact(
            sector = sector ?: sectorForAngle(angle),
            normalX = cos(radians).toFloat(),
            normalY = sin(radians).toFloat(),
            strength = strength,
        )
    }

internal fun sectorForAngle(angle: Double): ShellSector {
    val normalized = ((angle % 360.0) + 360.0) % 360.0
    return when {
        normalized < 45.0 || normalized >= 315.0 -> ShellSector.RIGHT
        normalized < 135.0 -> ShellSector.BOTTOM
        normalized < 225.0 -> ShellSector.LEFT
        else -> ShellSector.TOP
    }
}

internal fun <T> List<T>.repeatValues(times: Int): List<T> =
    buildList {
        repeat(times) {
            addAll(this@repeatValues)
        }
    }

internal fun diagonalRising(strength: Float = 0.8f): List<CreatureInteraction.ShellContact> =
    listOf(
        contact(ShellSector.RIGHT, 0.7f, -0.7f, strength),
        contact(ShellSector.LEFT, -0.7f, 0.7f, strength),
    )

internal fun diagonalFalling(strength: Float = 0.8f): List<CreatureInteraction.ShellContact> =
    listOf(
        contact(ShellSector.LEFT, -0.7f, -0.7f, strength),
        contact(ShellSector.RIGHT, 0.7f, 0.7f, strength),
    )

internal fun cardinal(
    sector: ShellSector,
    strength: Float = 0.8f,
): CreatureInteraction.ShellContact =
    when (sector) {
        ShellSector.LEFT -> contact(sector, -1f, 0f, strength)
        ShellSector.TOP -> contact(sector, 0f, -1f, strength)
        ShellSector.RIGHT -> contact(sector, 1f, 0f, strength)
        ShellSector.BOTTOM -> contact(sector, 0f, 1f, strength)
        ShellSector.FRONT -> contact(sector, 0f, 0f, strength, 1f)
        ShellSector.BACK -> contact(sector, 0f, 0f, strength, -1f)
    }

internal fun contact(
    sector: ShellSector,
    normalX: Float,
    normalY: Float,
    strength: Float = 0.8f,
    normalZ: Float = 0f,
) = CreatureInteraction.ShellContact(
    sector = sector,
    strength = strength,
    normalX = normalX,
    normalY = normalY,
    normalZ = normalZ,
)

internal fun feed(
    recognizer: MoveUnlockRecognizer,
    interactions: List<CreatureInteraction>,
    startMillis: Long,
    unlocked: Set<SlimeButtonAction>,
): Long {
    var now = startMillis
    interactions.forEach { interaction ->
        recognizer.accept(interaction, now, unlocked)
        now += 100L
    }
    return now
}

internal fun feedForResult(
    recognizer: MoveUnlockRecognizer,
    interactions: List<CreatureInteraction>,
    startMillis: Long,
    unlocked: Set<SlimeButtonAction>,
): SlimeButtonAction? {
    var now = startMillis
    var result: SlimeButtonAction? = null
    interactions.forEach { interaction ->
        result = recognizer.accept(interaction, now, unlocked) ?: result
        now += 100L
    }
    return result
}

// Older regressions assert first unlocks; production recognition also returns learned moves.
internal fun MoveUnlockRecognizer.accept(
    interaction: CreatureInteraction,
    nowMillis: Long,
    unlockedMoves: Set<SlimeButtonAction>,
): SlimeButtonAction? = accept(interaction, nowMillis)?.takeUnless { it in unlockedMoves }
