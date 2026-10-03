package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.ShellSector
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class SlimeAnimationCue(
    internal val durationSeconds: Float,
) {
    INVITATION(1.6f),
    SHELL_SPARK(0.42f),
}

data class SlimeEffectPixel(
    val x: Int,
    val y: Int,
    val brightness: Int,
)

data class SlimeAnimationSnapshot(
    val activeCue: SlimeAnimationCue?,
    val progress: Float,
    val sparkCount: Int,
)

class SlimeAnimationCoordinator {
    private val sparks = mutableListOf<ShellSpark>()
    private var active: ActiveAnimation? = null

    fun onShellContact(sector: ShellSector) {
        sparks += ShellSpark(sector = sector, ageSeconds = 0f)
    }

    fun trigger(
        cue: SlimeAnimationCue,
        sector: ShellSector = ShellSector.RIGHT,
    ) {
        if (cue == SlimeAnimationCue.SHELL_SPARK) {
            onShellContact(sector)
            return
        }
        active = ActiveAnimation(cue = cue, elapsedSeconds = 0f)
    }

    fun step(deltaSeconds: Float) {
        val sparkIterator = sparks.iterator()
        while (sparkIterator.hasNext()) {
            val spark = sparkIterator.next()
            spark.ageSeconds += deltaSeconds
            if (spark.ageSeconds >= SlimeAnimationCue.SHELL_SPARK.durationSeconds) {
                sparkIterator.remove()
            }
        }

        val animation = active ?: return
        animation.elapsedSeconds += deltaSeconds
        if (animation.elapsedSeconds >= animation.cue.durationSeconds) {
            active = null
        }
    }

    fun renderPixels(): List<SlimeEffectPixel> =
        buildList {
            sparks.forEach { spark -> addAll(renderSpark(spark)) }
            active?.let { animation -> addAll(renderAnimation(animation)) }
        }

    fun snapshot(): SlimeAnimationSnapshot {
        val animation = active
        return SlimeAnimationSnapshot(
            activeCue = animation?.cue,
            progress =
                animation
                    ?.let { it.elapsedSeconds / it.cue.durationSeconds }
                    ?.coerceIn(0f, 1f)
                    ?: 0f,
            sparkCount = sparks.size,
        )
    }

    private fun renderSpark(spark: ShellSpark): List<SlimeEffectPixel> {
        val progress =
            (spark.ageSeconds / SlimeAnimationCue.SHELL_SPARK.durationSeconds)
                .coerceIn(0f, 1f)
        val origin = sectorPoint(spark.sector)
        val centerPull = progress * 3f
        val x = lerp(origin.first.toFloat(), MATRIX_CENTER.toFloat(), centerPull / MATRIX_RADIUS)
        val y = lerp(origin.second.toFloat(), MATRIX_CENTER.toFloat(), centerPull / MATRIX_RADIUS)
        val brightness = (255f * (1f - progress) * (1f - progress)).roundToInt()
        if (brightness <= 0) return emptyList()
        return listOf(
            SlimeEffectPixel(x.roundToInt(), y.roundToInt(), brightness),
            SlimeEffectPixel(
                lerp(origin.first.toFloat(), x, 0.45f).roundToInt(),
                lerp(origin.second.toFloat(), y, 0.45f).roundToInt(),
                (brightness * 0.48f).roundToInt(),
            ),
        )
    }

    private fun renderAnimation(animation: ActiveAnimation): List<SlimeEffectPixel> {
        val progress =
            (animation.elapsedSeconds / animation.cue.durationSeconds)
                .coerceIn(0f, 1f)
        return when (animation.cue) {
            SlimeAnimationCue.INVITATION -> renderInvitation(progress)
            SlimeAnimationCue.SHELL_SPARK -> emptyList()
        }
    }

    private fun renderInvitation(progress: Float): List<SlimeEffectPixel> {
        val outwardProgress =
            if (progress < 0.62f) {
                smoothStep(progress / 0.62f)
            } else {
                1f - smoothStep((progress - 0.62f) / 0.38f)
            }
        val x = lerp(MATRIX_CENTER.toFloat(), EDGE_MAX.toFloat(), outwardProgress)
        val brightness = pulseBrightness((progress * 1.25f).coerceAtMost(1f))
        return listOf(
            SlimeEffectPixel(x.roundToInt(), MATRIX_CENTER, brightness),
            SlimeEffectPixel(
                lerp(MATRIX_CENTER.toFloat(), x, 0.7f).roundToInt(),
                MATRIX_CENTER,
                (brightness * 0.45f).roundToInt(),
            ),
        )
    }

    private fun sectorPoint(sector: ShellSector): Pair<Int, Int> =
        when (sector) {
            ShellSector.LEFT -> EDGE_MIN to MATRIX_CENTER
            ShellSector.TOP -> MATRIX_CENTER to EDGE_MIN
            ShellSector.RIGHT -> EDGE_MAX to MATRIX_CENTER
            ShellSector.BOTTOM -> MATRIX_CENTER to EDGE_MAX
            ShellSector.FRONT -> MATRIX_CENTER + 6 to MATRIX_CENTER - 6
            ShellSector.BACK -> MATRIX_CENTER - 6 to MATRIX_CENTER + 6
        }

    private fun pulseBrightness(progress: Float): Int = (90f + 165f * pulse(progress)).roundToInt()

    private fun pulse(progress: Float): Float = sin(progress.coerceIn(0f, 1f) * PI).toFloat()

    private data class ShellSpark(
        val sector: ShellSector,
        var ageSeconds: Float,
    )

    private data class ActiveAnimation(
        val cue: SlimeAnimationCue,
        var elapsedSeconds: Float,
    )

    private companion object {
        const val MATRIX_CENTER = 12
        const val MATRIX_RADIUS = 11f
        const val EDGE_MIN = 1
        const val EDGE_MAX = 23
    }
}
