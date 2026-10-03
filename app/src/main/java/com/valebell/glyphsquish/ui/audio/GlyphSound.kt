package com.valebell.glyphsquish.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val MAX_STREAMS = 6
private const val TAG = "GlyphSound"

/** Creation, loading callbacks, playback and release all run on the main thread. */
@MainThread
internal class GlyphSoundPlayer {
    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
    private val sounds = mutableMapOf<GlyphSoundEffect, List<Int>>()
    private val loadedSounds = mutableSetOf<Int>()
    private var previousFormVariation: Int? = null
    private var released = false

    suspend fun prepare(context: Context) {
        suspendCancellableCoroutine { continuation ->
            val pending = mutableSetOf<Int>()
            pool.setOnLoadCompleteListener { _, soundId, status ->
                if (!continuation.isActive || released) return@setOnLoadCompleteListener
                if (status == 0) {
                    loadedSounds += soundId
                } else {
                    Log.w(TAG, "Unable to load sound $soundId: $status")
                }
                pending -= soundId
                if (pending.isEmpty()) continuation.resume(Unit)
            }
            pending += queueSounds(context)
            if (pending.isEmpty()) continuation.resume(Unit)
        }
        pool.setOnLoadCompleteListener(null)
    }

    private fun queueSounds(context: Context): Set<Int> {
        GlyphSoundEffect.entries.forEach { effect ->
            sounds[effect] = effect.soundResources().map { resource -> pool.load(context, resource, 1) }
        }
        return sounds.values.flatten().filterTo(mutableSetOf()) { it != 0 }
    }

    fun play(effect: GlyphSoundEffect): Int {
        if (released) return 0
        val available = sounds[effect].orEmpty().withIndex().filter { it.value in loadedSounds }
        if (available.isEmpty()) return 0
        val sound =
            if (effect.isFormChange) {
                val alternatives = available.filter { it.index != previousFormVariation }
                (alternatives.ifEmpty { available }).random().also { previousFormVariation = it.index }
            } else {
                available.first()
            }
        return pool.play(sound.value, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        if (released) return
        released = true
        pool.setOnLoadCompleteListener(null)
        pool.release()
        loadedSounds.clear()
        sounds.clear()
    }
}

@Composable
internal fun rememberGlyphSound(enabled: Boolean): (GlyphSoundEffect) -> Unit {
    val context = LocalContext.current.applicationContext
    val player by produceState<GlyphSoundPlayer?>(null, context, enabled) {
        if (!enabled) return@produceState
        val prepared = GlyphSoundPlayer()
        try {
            prepared.prepare(context)
            value = prepared
            awaitCancellation()
        } finally {
            value = null
            prepared.release()
        }
    }
    return remember(player, enabled) { { effect -> if (enabled) player?.play(effect) } }
}
