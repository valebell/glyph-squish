package com.valebell.glyphsquish.ui.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.Locale

/** Requires FFmpeg on PATH; run ./gradlew :app:generateSoundAssets after editing sounds. */
fun main(args: Array<String>) {
    val directory = File(args.single())
    check(directory.isDirectory || directory.mkdirs())
    val temporary = Files.createTempDirectory("glyph-sounds-").toFile()
    try {
        // Finish encoding and verifying every sample before replacing checked-in assets.
        val assets =
            GlyphSoundEffect.entries.flatMap { effect ->
                val variations = if (effect.isFormChange) listOf(-1.0, 0.0, 1.0) else listOf(0.0)
                variations.mapIndexed { index, variation ->
                    val name = effect.name.lowercase(Locale.ROOT)
                    val suffix = if (effect.isFormChange) "_$index" else ""
                    "glyph_$name$suffix" to
                        encodeFlac(GlyphSynthesizer.synthesize(effect, variation = variation), temporary)
                }
            }
        assets.forEach { (name, bytes) ->
            File(directory, "$name.flac").writeBytes(bytes)
            val oldWave = File(directory, "$name.wav")
            check(!oldWave.exists() || oldWave.delete()) { "Unable to remove $oldWave" }
        }
    } finally {
        temporary.deleteRecursively()
    }
}

private fun encodeFlac(
    samples: ShortArray,
    directory: File,
): ByteArray {
    val wave = File(directory, "source.wav")
    val flac = File(directory, "encoded.flac")
    val decoded = File(directory, "decoded.pcm")
    wave.writeBytes(encodeWave(samples))
    runFfmpeg(
        "-i",
        wave.absolutePath,
        "-map_metadata",
        "-1",
        "-c:a",
        "flac",
        "-compression_level",
        "8",
        "-flags:a",
        "+bitexact",
        "-metadata_header_padding",
        "0",
        flac.absolutePath,
    )
    runFfmpeg("-i", flac.absolutePath, "-f", "s16le", "-c:a", "pcm_s16le", decoded.absolutePath)
    check(encodePcm(samples).contentEquals(decoded.readBytes())) { "FLAC changed the synthesized samples" }
    return flac.readBytes()
}

private fun runFfmpeg(vararg arguments: String) {
    val command = listOf("ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y") + arguments
    val exitCode = ProcessBuilder(command).inheritIO().start().waitFor()
    check(exitCode == 0) { "FFmpeg failed with exit code $exitCode" }
}

internal fun encodePcm(samples: ShortArray): ByteArray =
    ByteBuffer
        .allocate(samples.size * Short.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply { samples.forEach(::putShort) }
        .array()

internal fun encodeWave(samples: ShortArray): ByteArray {
    val dataSize = samples.size * Short.SIZE_BYTES
    return ByteBuffer
        .allocate(44 + dataSize)
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1) // Linear PCM.
            putShort(1) // Mono.
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * Short.SIZE_BYTES)
            putShort(Short.SIZE_BYTES.toShort())
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
            put(encodePcm(samples))
        }.array()
}
