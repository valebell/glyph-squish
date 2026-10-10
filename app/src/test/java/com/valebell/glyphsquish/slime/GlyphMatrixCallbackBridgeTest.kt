package com.valebell.glyphsquish.slime

import org.junit.Assert.assertEquals
import org.junit.Test

class GlyphMatrixCallbackBridgeTest {
    private val events = mutableListOf<String>()

    private inner class RecordingSession : GlyphMatrixSessionCallbacks {
        override fun onGlyphServiceConnected() {
            events += "connected"
        }

        override fun onGlyphServiceDisconnected() {
            events += "disconnected"
        }
    }

    @Test
    fun forwardsSdkCallbacksToTheSession() {
        val bridge = GlyphMatrixCallbackBridge(RecordingSession())

        bridge.onServiceConnected(null)
        bridge.onServiceDisconnected(null)

        assertEquals(listOf("connected", "disconnected"), events)
    }

    @Test
    fun detachedBridgeDeliversNothing() {
        val bridge = GlyphMatrixCallbackBridge(RecordingSession())
        bridge.onServiceConnected(null)
        bridge.detach()

        bridge.onServiceConnected(null)
        bridge.onServiceDisconnected(null)

        assertEquals(listOf("connected"), events)
    }
}
