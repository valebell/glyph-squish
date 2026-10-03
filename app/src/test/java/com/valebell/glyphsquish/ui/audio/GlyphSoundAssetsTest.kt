package com.valebell.glyphsquish.ui.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Locale

class GlyphSoundAssetsTest {
    @Test
    fun packagedFlacMetadataMatchesTheSynthesizedSoundsAndFitsSoundPool() {
        val expectedFiles = mutableSetOf<String>()
        GlyphSoundEffect.entries.forEach { effect ->
            val variations = if (effect.isFormChange) listOf(-1.0, 0.0, 1.0) else listOf(0.0)
            variations.forEachIndexed { index, variation ->
                val name = effect.name.lowercase(Locale.ROOT)
                val suffix = if (effect.isFormChange) "_$index" else ""
                val file = File("src/main/res/raw/glyph_$name$suffix.flac")
                expectedFiles += file.name
                val samples = GlyphSynthesizer.synthesize(effect, variation = variation)

                assertTrue(
                    "$effect exceeds SoundPool's decoded sample limit",
                    samples.size * Short.SIZE_BYTES < 1_048_576,
                )
                assertFlacMetadata(file, samples)
            }
        }
        val packagedFiles =
            File("src/main/res/raw")
                .listFiles()
                .orEmpty()
                .filter { it.name.startsWith("glyph_") }
                .map { it.name }
                .toSet()
        assertEquals("Package only the expected FLAC effects, without duplicate WAVs", expectedFiles, packagedFiles)
    }

    private fun assertFlacMetadata(
        file: File,
        samples: ShortArray,
    ) {
        val bytes = file.readBytes()
        val header = ByteBuffer.wrap(bytes)
        assertEquals("FLAC signature in $file", 0x664C6143, header.getInt(0))
        // RFC 9639: STREAMINFO is the first metadata block, type 0, length 34.
        assertEquals("STREAMINFO in $file", 34, header.getInt(4) and 0x7FFFFFFF)
        val audioInfo = header.getLong(18)
        assertEquals("Sample rate in $file", SAMPLE_RATE.toLong(), audioInfo ushr 44)
        assertEquals("Mono audio in $file", 1L, ((audioInfo ushr 41) and 7) + 1)
        assertEquals("16-bit audio in $file", 16L, ((audioInfo ushr 36) and 31) + 1)
        assertEquals("Sample count in $file", samples.size.toLong(), audioInfo and 0xFFFFFFFFFL)
        // FLAC stores the MD5 of the original little-endian PCM, independent of encoder version.
        assertArrayEquals(
            "Regenerate $file with :app:generateSoundAssets",
            MessageDigest.getInstance("MD5").digest(encodePcm(samples)),
            bytes.copyOfRange(26, 42),
        )
    }
}
