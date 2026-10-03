package com.valebell.glyphsquish.ui.audio

import android.media.SoundPool
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphSoundPlayerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everyPackagedVariationLoadsAndPlays() =
        runBlocking {
            withContext(Dispatchers.Main) {
                val resources = GlyphSoundEffect.entries.flatMap { it.soundResources() }
                val pool = SoundPool.Builder().setMaxStreams(1).build()
                try {
                    val completed = CompletableDeferred<Unit>()
                    val statuses = mutableMapOf<Int, Int>()
                    pool.setOnLoadCompleteListener { _, sampleId, status ->
                        statuses[sampleId] = status
                        if (statuses.size == resources.size) completed.complete(Unit)
                    }
                    val samples =
                        resources.associateWith { resource ->
                            pool.load(context, resource, 1).also { assertTrue("Unable to queue $resource", it > 0) }
                        }
                    withTimeout(10_000) { completed.await() }
                    samples.forEach { (resource, sampleId) ->
                        val name = context.resources.getResourceEntryName(resource)
                        assertEquals("$name failed to decode", 0, statuses[sampleId])
                        val streamId = pool.play(sampleId, 1f, 1f, 1, 0, 1f)
                        assertTrue("$name did not play", streamId > 0)
                        pool.stop(streamId)
                    }
                } finally {
                    pool.release()
                }
            }
        }

    @Test
    fun packagedEffectsLoadAndPlayAndReleaseStopsPlayback() =
        runBlocking {
            withContext(Dispatchers.Main) {
                val player = GlyphSoundPlayer()
                try {
                    withTimeout(10_000) { player.prepare(context) }
                    GlyphSoundEffect.entries.forEach { effect ->
                        assertTrue("$effect did not play", player.play(effect) != 0)
                    }
                } finally {
                    player.release()
                }
                assertEquals(0, player.play(GlyphSoundEffect.SELECT))
                player.release()
            }
        }

    @Test
    fun cancellingPreparationAllowsAReplacementPlayerToLoad() =
        runBlocking {
            withContext(Dispatchers.Main) {
                val cancelled = GlyphSoundPlayer()
                val loading = launch(start = CoroutineStart.UNDISPATCHED) { cancelled.prepare(context) }
                loading.cancelAndJoin()
                cancelled.release()

                val replacement = GlyphSoundPlayer()
                try {
                    withTimeout(10_000) { replacement.prepare(context) }
                    assertTrue(replacement.play(GlyphSoundEffect.SELECT) != 0)
                } finally {
                    replacement.release()
                }
            }
        }
}
