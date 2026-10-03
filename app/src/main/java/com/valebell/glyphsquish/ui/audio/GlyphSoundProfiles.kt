package com.valebell.glyphsquish.ui.audio

internal fun moveProfile(effect: GlyphSoundEffect): MoveProfile =
    when (effect) {
        GlyphSoundEffect.MOVE_BURST -> {
            MoveProfile(
                startFrequency = 620.0,
                endFrequency = 380.0,
                vibratoCycles = 1.0,
                vibratoDepth = 35.0,
                sustain = 0.05,
                noise = NoiseProfile(mix = 0.58, smoothing = 0.42),
                pulses =
                    arrayOf(
                        Pulse(0.05, 0.30, 28.0),
                        Pulse(0.245, 1.0, 40.0),
                        Pulse(0.49, 0.42, 24.0),
                        Pulse(0.72, 0.24, 18.0),
                    ),
            )
        }

        GlyphSoundEffect.MOVE_HELIX -> {
            MoveProfile(
                startFrequency = 510.0,
                endFrequency = 1_080.0,
                vibratoCycles = 5.0,
                vibratoDepth = 105.0,
                sustain = 0.15,
                noise = NoiseProfile(mix = 0.12, smoothing = 0.18),
                pulses =
                    arrayOf(
                        Pulse(0.03, 0.42, 22.0),
                        Pulse(0.38, 0.34, 18.0),
                        Pulse(0.70, 0.62, 24.0),
                    ),
            )
        }

        GlyphSoundEffect.MOVE_ECHO -> {
            MoveProfile(
                startFrequency = 760.0,
                endFrequency = 620.0,
                vibratoCycles = 1.5,
                vibratoDepth = 24.0,
                sustain = 0.025,
                noise = NoiseProfile(mix = 0.08, smoothing = 0.12),
                pulses =
                    arrayOf(
                        Pulse(0.08, 1.0, 34.0),
                        Pulse(0.31, 0.68, 30.0),
                        Pulse(0.55, 0.45, 27.0),
                        Pulse(0.79, 0.27, 24.0),
                    ),
            )
        }

        GlyphSoundEffect.MOVE_RICOCHET -> {
            MoveProfile(
                startFrequency = 880.0,
                endFrequency = 1_040.0,
                vibratoCycles = 2.0,
                vibratoDepth = 18.0,
                sustain = 0.018,
                noise = NoiseProfile(mix = 0.10, smoothing = 0.16),
                pulses =
                    arrayOf(
                        Pulse(0.08, 0.38, 45.0),
                        Pulse(0.31, 1.0, 52.0),
                        Pulse(0.39, 0.48, 46.0),
                        Pulse(0.59, 0.86, 52.0),
                        Pulse(0.67, 0.36, 44.0),
                    ),
            )
        }

        GlyphSoundEffect.MOVE_THUNDERBOLT -> {
            MoveProfile(
                startFrequency = 480.0,
                endFrequency = 1_240.0,
                vibratoCycles = 9.0,
                vibratoDepth = 150.0,
                sustain = 0.045,
                noise = NoiseProfile(mix = 0.52, smoothing = 0.55),
                pulses =
                    arrayOf(
                        Pulse(0.03, 0.38, 55.0),
                        Pulse(0.11, 0.55, 60.0),
                        Pulse(0.17, 0.62, 60.0),
                        Pulse(0.23, 0.72, 62.0),
                        Pulse(0.29, 0.86, 64.0),
                        Pulse(0.34, 1.0, 68.0),
                        Pulse(0.52, 0.62, 42.0),
                        Pulse(0.65, 0.82, 48.0),
                        Pulse(0.86, 0.42, 34.0),
                    ),
            )
        }

        else -> {
            error("$effect is not a move sound")
        }
    }

internal fun formProfile(effect: GlyphSoundEffect): FormProfile =
    when (effect) {
        GlyphSoundEffect.FORM_BLOB -> {
            FormProfile(
                startFrequency = 820.0,
                endFrequency = 360.0,
                vibratoCycles = 1.0,
                vibratoDepth = 28.0,
                mechanismPitch = 0.82,
                clicks =
                    arrayOf(
                        MechanismClick(0.01, 0.90, 920.0),
                        MechanismClick(0.22, 0.34, 760.0),
                        MechanismClick(0.49, 0.28, 680.0),
                        MechanismClick(0.73, 0.42, 610.0),
                        MechanismClick(0.93, 1.0, 520.0),
                    ),
            )
        }

        GlyphSoundEffect.FORM_SHARK -> {
            FormProfile(
                startFrequency = 1_520.0,
                endFrequency = 310.0,
                vibratoCycles = 0.5,
                vibratoDepth = 42.0,
                mechanismPitch = 0.92,
                clicks =
                    arrayOf(
                        MechanismClick(0.01, 0.82, 1_240.0),
                        MechanismClick(0.46, 0.62, 820.0),
                        MechanismClick(0.93, 1.0, 470.0),
                    ),
            )
        }

        GlyphSoundEffect.FORM_CAT -> {
            FormProfile(
                startFrequency = 720.0,
                endFrequency = 1_060.0,
                vibratoCycles = 3.5,
                vibratoDepth = 118.0,
                mechanismPitch = 1.12,
                clicks =
                    arrayOf(
                        MechanismClick(0.01, 0.74, 1_420.0),
                        MechanismClick(0.13, 0.46, 1_680.0),
                        MechanismClick(0.77, 0.48, 1_520.0),
                        MechanismClick(0.93, 0.92, 1_780.0),
                    ),
            )
        }

        GlyphSoundEffect.FORM_STAR -> {
            FormProfile(
                startFrequency = 560.0,
                endFrequency = 1_680.0,
                vibratoCycles = 6.0,
                vibratoDepth = 82.0,
                mechanismPitch = 1.28,
                clicks =
                    arrayOf(
                        MechanismClick(0.01, 0.50, 1_100.0),
                        MechanismClick(0.19, 0.34, 1_300.0),
                        MechanismClick(0.37, 0.42, 1_500.0),
                        MechanismClick(0.55, 0.50, 1_700.0),
                        MechanismClick(0.73, 0.62, 1_900.0),
                        MechanismClick(0.93, 0.94, 2_160.0),
                    ),
            )
        }

        GlyphSoundEffect.FORM_BUTTERFLY -> {
            FormProfile(
                startFrequency = 460.0,
                endFrequency = 1_320.0,
                vibratoCycles = 10.0,
                vibratoDepth = 155.0,
                mechanismPitch = 1.18,
                clicks =
                    arrayOf(
                        MechanismClick(0.01, 0.42, 1_360.0),
                        MechanismClick(0.15, 0.32, 1_620.0),
                        MechanismClick(0.29, 0.38, 1_420.0),
                        MechanismClick(0.43, 0.34, 1_720.0),
                        MechanismClick(0.57, 0.42, 1_520.0),
                        MechanismClick(0.72, 0.48, 1_820.0),
                        MechanismClick(0.93, 0.88, 2_020.0),
                    ),
            )
        }

        else -> {
            error("$effect is not a form sound")
        }
    }

internal class FormProfile(
    val startFrequency: Double,
    val endFrequency: Double,
    val vibratoCycles: Double,
    val vibratoDepth: Double,
    val mechanismPitch: Double,
    val clicks: Array<MechanismClick>,
)

internal class MoveProfile(
    val startFrequency: Double,
    val endFrequency: Double,
    val vibratoCycles: Double,
    val vibratoDepth: Double,
    val sustain: Double,
    val noise: NoiseProfile,
    val pulses: Array<Pulse>,
)

internal data class Pulse(
    val start: Double,
    val amplitude: Double,
    val decay: Double,
)

internal data class MechanismClick(
    val start: Double,
    val amplitude: Double,
    val frequency: Double,
)

internal data class NoiseProfile(
    val mix: Double,
    val smoothing: Double,
)
