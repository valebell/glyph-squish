package com.valebell.glyphsquish.ui.audio

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal data class GlyphHapticCue(
    val progress: Double,
    val strength: Double,
)

private class GlyphHapticPlayer(
    context: Context,
) {
    private val vibrator: Vibrator? =
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    private val attributes =
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA)

    fun play(
        effect: GlyphSoundEffect,
        synchronizeWithSound: Boolean,
    ) {
        val activeVibrator = vibrator?.takeIf(Vibrator::hasVibrator) ?: return
        val waveform = buildWaveform(effect, synchronizeWithSound)
        activeVibrator.vibrate(
            VibrationEffect.createWaveform(
                waveform.timings,
                waveform.amplitudes,
                NO_REPEAT,
            ),
            attributes,
        )
    }

    fun cancel() {
        vibrator?.cancel()
    }
}

internal data class GlyphHapticWaveform(
    val timings: LongArray,
    val amplitudes: IntArray,
)

internal fun buildWaveform(
    effect: GlyphSoundEffect,
    synchronizeWithSound: Boolean,
): GlyphHapticWaveform {
    val cues = GlyphTone.hapticCues(effect)
    val timings = LongArray(cues.size * SEGMENTS_PER_CUE)
    val amplitudes = IntArray(timings.size)
    val audioOffset = if (synchronizeWithSound) AUDIO_OUTPUT_OFFSET_MILLIS else 0L
    var elapsedMillis = 0L

    cues.forEachIndexed { index, cue ->
        val segmentIndex = index * SEGMENTS_PER_CUE
        val cueStartMillis =
            audioOffset + (effect.durationMillis * cue.progress).roundToLong()
        val pulseDuration =
            (
                MIN_PULSE_MILLIS +
                    (MAX_PULSE_MILLIS - MIN_PULSE_MILLIS) * cue.strength
            ).roundToLong()

        timings[segmentIndex] = (cueStartMillis - elapsedMillis).coerceAtLeast(0L)
        timings[segmentIndex + 1] = pulseDuration
        amplitudes[segmentIndex + 1] =
            (
                MIN_AMPLITUDE +
                    (MAX_AMPLITUDE - MIN_AMPLITUDE) * cue.strength
            ).roundToInt()
        elapsedMillis = cueStartMillis + pulseDuration
    }

    return GlyphHapticWaveform(timings, amplitudes)
}

@Composable
internal fun rememberGlyphHaptic(): (GlyphSoundEffect, Boolean) -> Unit {
    val context = LocalContext.current
    val player =
        remember(context) {
            GlyphHapticPlayer(context.applicationContext)
        }
    DisposableEffect(player) {
        onDispose(player::cancel)
    }
    return remember(player) {
        { effect, synchronizeWithSound ->
            player.play(effect, synchronizeWithSound)
        }
    }
}

private const val AUDIO_OUTPUT_OFFSET_MILLIS = 20L
private const val SEGMENTS_PER_CUE = 2
private const val MIN_PULSE_MILLIS = 8.0
private const val MAX_PULSE_MILLIS = 16.0
private const val MIN_AMPLITUDE = 45.0
private const val MAX_AMPLITUDE = 205.0
private const val NO_REPEAT = -1
