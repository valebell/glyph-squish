package com.valebell.glyphsquish.ui.audio

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.slime.SlimeEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphToneTest {
    @Test
    fun synthesizedEffectsHaveExpectedDurationAndSafeAmplitude() {
        val sampleRate = 44_100

        GlyphSoundEffect.entries.forEach { effect ->
            val samples = GlyphSynthesizer.synthesize(effect, sampleRate)

            assertEquals(sampleRate * effect.durationMillis / 1_000, samples.size)
            assertTrue(samples.any { it.toInt() != 0 })
            assertTrue(samples.maxOf { kotlin.math.abs(it.toInt()) } < Short.MAX_VALUE)
            assertEquals(0, samples.first().toInt())
            assertEquals(0, samples.last().toInt())
        }
    }

    @Test
    fun synthesizedCrackleIsDeterministic() {
        assertArrayEquals(
            GlyphSynthesizer.synthesize(GlyphSoundEffect.FORM_BLOB),
            GlyphSynthesizer.synthesize(GlyphSoundEffect.FORM_BLOB),
        )
    }

    @Test
    fun formDissolveVariationsKeepDurationButChangeTimbre() {
        val low = GlyphSynthesizer.synthesize(GlyphSoundEffect.FORM_BLOB, variation = -1.0)
        val neutral = GlyphSynthesizer.synthesize(GlyphSoundEffect.FORM_BLOB)
        val high = GlyphSynthesizer.synthesize(GlyphSoundEffect.FORM_BLOB, variation = 1.0)

        assertEquals(neutral.size, low.size)
        assertEquals(neutral.size, high.size)
        assertFalse(low.contentEquals(neutral))
        assertFalse(high.contentEquals(neutral))
        assertFalse(low.contentEquals(high))
        assertTrue(meanSampleDifference(low, neutral) > 600.0)
        assertTrue(meanSampleDifference(high, neutral) > 600.0)
    }

    @Test
    fun formDissolveHasMechanicalReleaseAndLockTransients() {
        val sampleRate = 44_100
        val samples = GlyphSynthesizer.synthesize(GlyphSoundEffect.FORM_BLOB, sampleRate)

        assertTrue(peakAround(samples, sampleRate, 0.01) > 3_000)
        assertTrue(peakAround(samples, sampleRate, 0.93) > 3_000)
    }

    @Test
    fun formAndMoveSoundsMatchTheirAnimationDurations() {
        CreatureForm.entries.forEach { form ->
            assertEquals(
                SlimeEngine.FORM_TRANSITION_MILLIS,
                GlyphSoundEffect.forForm(form).durationMillis,
            )
        }
        SlimeButtonAction.entries.forEach { move ->
            assertEquals(
                expectedMoveDuration(move),
                GlyphSoundEffect.forMove(move).durationMillis,
            )
        }
    }

    @Test
    fun everyMoveHasADistinctMotif() {
        val motifs =
            SlimeButtonAction.entries.map { move ->
                GlyphSynthesizer.synthesize(GlyphSoundEffect.forMove(move)).contentHashCode()
            }

        assertFalse(motifs.isEmpty())
        assertEquals(motifs.size, motifs.distinct().size)
    }

    @Test
    fun everyFormHasADistinctMotif() {
        val motifs =
            CreatureForm.entries.map { form ->
                GlyphSynthesizer.synthesize(GlyphSoundEffect.forForm(form)).contentHashCode()
            }

        assertEquals(CreatureForm.entries.size, motifs.distinct().size)
    }

    @Test
    fun hapticCuesAreOrderedAndFitTheirSounds() {
        GlyphSoundEffect.entries.forEach { effect ->
            val cues = GlyphTone.hapticCues(effect)

            assertFalse(cues.isEmpty())
            assertEquals(cues.sortedBy { it.progress }, cues)
            assertTrue(cues.all { it.progress in 0.0..1.0 })
            assertTrue(cues.all { it.strength in 0.0..1.0 })
        }
    }

    @Test
    fun soundSynchronizationOffsetsTheWholeHapticPattern() {
        GlyphSoundEffect.entries.forEach { effect ->
            val immediate = buildWaveform(effect, synchronizeWithSound = false)
            val synchronized = buildWaveform(effect, synchronizeWithSound = true)

            assertEquals(20L, synchronized.timings.first() - immediate.timings.first())
            assertArrayEquals(
                immediate.timings.copyOfRange(1, immediate.timings.size),
                synchronized.timings.copyOfRange(1, synchronized.timings.size),
            )
            assertArrayEquals(immediate.amplitudes, synchronized.amplitudes)
        }
    }

    private fun expectedMoveDuration(move: SlimeButtonAction): Int =
        when (move) {
            SlimeButtonAction.BURST -> SlimeEngine.BURST_ACTION_MILLIS
            SlimeButtonAction.HELIX -> SlimeEngine.HELIX_ACTION_MILLIS
            SlimeButtonAction.ECHO -> SlimeEngine.ECHO_ACTION_MILLIS
            SlimeButtonAction.RICOCHET -> SlimeEngine.RICOCHET_ACTION_MILLIS
            SlimeButtonAction.THUNDERBOLT -> SlimeEngine.THUNDERBOLT_ACTION_MILLIS
        }

    private fun peakAround(
        samples: ShortArray,
        sampleRate: Int,
        progress: Double,
    ): Int {
        val center = (samples.lastIndex * progress).toInt()
        val radius = sampleRate * 30 / 1_000
        val range =
            (center - radius).coerceAtLeast(0)..(center + radius).coerceAtMost(samples.lastIndex)
        return samples
            .sliceArray(range)
            .maxOf { kotlin.math.abs(it.toInt()) }
    }

    private fun meanSampleDifference(
        first: ShortArray,
        second: ShortArray,
    ): Double =
        first.indices
            .sumOf { index ->
                kotlin.math.abs(first[index].toInt() - second[index].toInt()).toLong()
            }.toDouble() / first.size
}
