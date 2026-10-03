package com.valebell.glyphsquish.ui.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val SAMPLE_RATE = 44_100

internal object GlyphSynthesizer {
    fun synthesize(
        effect: GlyphSoundEffect,
        sampleRate: Int = SAMPLE_RATE,
        variation: Double = 0.0,
    ): ShortArray {
        require(sampleRate > 0) { "Sample rate must be positive" }
        require(variation in -1.0..1.0) { "Variation must be between -1 and 1" }
        return when (effect) {
            GlyphSoundEffect.SELECT -> synthesizeSelection(sampleRate)

            GlyphSoundEffect.FORM_BLOB,
            GlyphSoundEffect.FORM_SHARK,
            GlyphSoundEffect.FORM_CAT,
            GlyphSoundEffect.FORM_STAR,
            GlyphSoundEffect.FORM_BUTTERFLY,
            -> synthesizeDissolve(effect, sampleRate, variation)

            GlyphSoundEffect.SLIDE_FORWARD -> synthesizeSlide(sampleRate, forward = true)

            GlyphSoundEffect.SLIDE_BACKWARD -> synthesizeSlide(sampleRate, forward = false)

            GlyphSoundEffect.MOVE_BURST,
            GlyphSoundEffect.MOVE_HELIX,
            GlyphSoundEffect.MOVE_ECHO,
            GlyphSoundEffect.MOVE_RICOCHET,
            GlyphSoundEffect.MOVE_THUNDERBOLT,
            -> synthesizeMove(effect, sampleRate)
        }
    }

    private fun synthesizeSelection(sampleRate: Int): ShortArray {
        val sampleCount = sampleCount(GlyphSoundEffect.SELECT, sampleRate)
        var carrierPhase = 0.0
        return ShortArray(sampleCount) { index ->
            val time = index.toDouble() / sampleRate
            val progress = progress(index, sampleCount)
            val carrierFrequency = 920.0 + 480.0 * progress
            carrierPhase += 2.0 * PI * carrierFrequency / sampleRate

            val envelope =
                attack(time, 0.002) *
                    exp(-24.0 * time) *
                    release(progress, 0.12)
            val signal = harmonicSignal(carrierPhase) * envelope
            toPcm(signal, gain = 0.30)
        }
    }

    private fun synthesizeDissolve(
        effect: GlyphSoundEffect,
        sampleRate: Int,
        variation: Double,
    ): ShortArray {
        val profile = formProfile(effect)
        val sampleCount = sampleCount(effect, sampleRate)
        val durationSeconds = effect.durationMillis / 1_000.0
        val particlePitch = 1.0 + variation * 0.075
        val mechanismPitch = profile.mechanismPitch * (1.0 + variation * 0.055)
        val dissolveMix = 0.40 - variation * 0.045
        val mechanismMix = 0.70 + variation * 0.08
        var particlePhase = 0.0
        return ShortArray(sampleCount) { index ->
            val time = index.toDouble() / sampleRate
            val progress = progress(index, sampleCount)
            val particleFrequency =
                (
                    profile.startFrequency +
                        (profile.endFrequency - profile.startFrequency) * progress +
                        sin(2.0 * PI * profile.vibratoCycles * progress) * profile.vibratoDepth
                ) * particlePitch
            particlePhase += 2.0 * PI * particleFrequency / sampleRate

            val tonalDust =
                harmonicSignal(particlePhase) *
                    (
                        dissolveEnvelope(progress) * (0.78 - variation * 0.08) +
                            reformEnvelope(progress) * (0.22 + variation * 0.08)
                    )
            val envelope =
                attack(time, 0.003) *
                    release(progress, 0.04)
            val mechanism =
                mechanicalClick(
                    progress = progress,
                    durationSeconds = durationSeconds,
                    pitch = mechanismPitch,
                    clicks = profile.clicks,
                )
            val dissolve = tonalDust * envelope
            toPcm(
                signal = dissolve * dissolveMix + mechanism * mechanismMix,
                gain = 0.30,
            )
        }
    }

    private fun synthesizeSlide(
        sampleRate: Int,
        forward: Boolean,
    ): ShortArray {
        val effect =
            if (forward) {
                GlyphSoundEffect.SLIDE_FORWARD
            } else {
                GlyphSoundEffect.SLIDE_BACKWARD
            }
        val sampleCount = sampleCount(effect, sampleRate)
        val noise = SynthNoise(seed = if (forward) 0x5EED else 0xBACC)
        var filteredNoise = 0.0
        var sweepPhase = 0.0
        return ShortArray(sampleCount) { index ->
            val progress = progress(index, sampleCount)
            val directionProgress = if (forward) progress else 1.0 - progress
            val sweepFrequency = 280.0 + 620.0 * directionProgress
            sweepPhase += 2.0 * PI * sweepFrequency / sampleRate

            val smoothing = 0.06 + 0.22 * directionProgress
            filteredNoise += (noise.next() - filteredNoise) * smoothing
            val envelope = sin(PI * progress).coerceAtLeast(0.0).pow(0.72)
            val signal =
                (
                    filteredNoise * 0.58 +
                        harmonicSignal(sweepPhase) * 0.42
                ) * envelope
            toPcm(signal, gain = 0.24)
        }
    }

    private fun synthesizeMove(
        effect: GlyphSoundEffect,
        sampleRate: Int,
    ): ShortArray {
        val profile = moveProfile(effect)
        val sampleCount = sampleCount(effect, sampleRate)
        val noise = SynthNoise(seed = effect.ordinal * 0x1F31 + 0x6A09)
        var carrierPhase = 0.0
        var filteredNoise = 0.0
        return ShortArray(sampleCount) { index ->
            val time = index.toDouble() / sampleRate
            val progress = progress(index, sampleCount)
            val frequency =
                profile.startFrequency +
                    (profile.endFrequency - profile.startFrequency) * progress +
                    sin(2.0 * PI * profile.vibratoCycles * progress) * profile.vibratoDepth
            carrierPhase += 2.0 * PI * frequency / sampleRate

            filteredNoise += (noise.next() - filteredNoise) * profile.noise.smoothing
            val pulse = pulseEnvelope(progress, profile.pulses)
            val envelope =
                attack(time, 0.003) *
                    release(progress, 0.035) *
                    (profile.sustain * sin(PI * progress).coerceAtLeast(0.0) + pulse)
            val signal =
                (
                    harmonicSignal(carrierPhase) * (1.0 - profile.noise.mix) +
                        filteredNoise * profile.noise.mix
                ) * envelope
            toPcm(signal, gain = 0.25)
        }
    }

    private fun pulseEnvelope(
        progress: Double,
        pulses: Array<Pulse>,
    ): Double =
        pulses.maxOf { pulse ->
            if (progress < pulse.start) {
                0.0
            } else {
                pulse.amplitude * exp(-(progress - pulse.start) * pulse.decay)
            }
        }

    private fun dissolveEnvelope(progress: Double): Double =
        when {
            progress < 0.14 -> progress / 0.14
            progress < 0.72 -> 1.0 - (progress - 0.14) / 0.58 * 0.72
            else -> 0.28 * release(progress, 0.28)
        }

    private fun reformEnvelope(progress: Double): Double =
        if (progress < 0.72) {
            0.0
        } else {
            sin(PI * (progress - 0.72) / 0.28).coerceAtLeast(0.0)
        }

    private fun mechanicalClick(
        progress: Double,
        durationSeconds: Double,
        pitch: Double,
        clicks: Array<MechanismClick>,
    ): Double =
        clicks.sumOf { click ->
            val elapsed = (progress - click.start) * durationSeconds
            if (elapsed !in 0.0..MECHANICAL_CLICK_SECONDS) {
                0.0
            } else {
                val frequency = click.frequency * pitch
                val snap =
                    sin(2.0 * PI * frequency * 3.17 * elapsed) *
                        exp(-210.0 * elapsed)
                val metal =
                    sin(2.0 * PI * frequency * elapsed) *
                        exp(-82.0 * elapsed)
                val body =
                    sin(2.0 * PI * frequency * 0.43 * elapsed) *
                        exp(-42.0 * elapsed)
                click.amplitude * (snap * 0.42 + metal * 0.46 + body * 0.24)
            }
        }

    private fun harmonicSignal(phase: Double): Double =
        sin(phase) * 0.72 +
            sin(phase * 2.003) * 0.20 +
            sin(phase * 3.01) * 0.08

    private fun sampleCount(
        effect: GlyphSoundEffect,
        sampleRate: Int,
    ): Int = sampleRate * effect.durationMillis / 1_000

    private fun progress(
        index: Int,
        sampleCount: Int,
    ): Double = index.toDouble() / (sampleCount - 1).coerceAtLeast(1)

    private fun attack(
        time: Double,
        duration: Double,
    ): Double = (time / duration).coerceIn(0.0, 1.0)

    private fun release(
        progress: Double,
        durationFraction: Double,
    ): Double = ((1.0 - progress) / durationFraction).coerceIn(0.0, 1.0)

    private fun toPcm(
        signal: Double,
        gain: Double,
    ): Short {
        val quantized = (signal.coerceIn(-1.0, 1.0) * 96.0).roundToInt() / 96.0
        return (quantized * Short.MAX_VALUE * gain)
            .roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }

    private const val MECHANICAL_CLICK_SECONDS = 0.065

    private class SynthNoise(
        seed: Int,
    ) {
        private var state = seed

        fun next(): Double {
            state = state xor (state shl 13)
            state = state xor (state ushr 17)
            state = state xor (state shl 5)
            return (state ushr 1).toDouble() / Int.MAX_VALUE * 2.0 - 1.0
        }
    }
}
