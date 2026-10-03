package com.valebell.glyphsquish.slime

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GlyphFrameDeliveryTest {
    private val delivery = GlyphFrameDelivery()
    private val sentFrames = mutableListOf<IntArray>()

    @Test
    fun equalEncodedFramesAreSentOnlyOnce() {
        send(intArrayOf(0, 4095, 0))
        send(intArrayOf(0, 4095, 0))

        assertEquals(1, sentFrames.size)
        assertArrayEquals(intArrayOf(0, 4095, 0), sentFrames.single())
    }

    @Test
    fun changingOnePixelSendsTheNewFrame() {
        send(intArrayOf(0, 4095, 0))
        send(intArrayOf(0, 4094, 0))

        assertEquals(2, sentFrames.size)
        assertArrayEquals(intArrayOf(0, 4094, 0), sentFrames.last())
    }

    @Test
    fun brightnessChangesAreDeliveredForTheSameEnginePixels() {
        val frame = intArrayOf(0, 255, 128)
        val activeFrame = GlyphFrameEncoder.encode(frame, 0.85f)
        val dimmedFrame = GlyphFrameEncoder.encode(frame, 0.25f)

        send(activeFrame)
        send(dimmedFrame)
        send(dimmedFrame)

        assertEquals(2, sentFrames.size)
        assertArrayEquals(activeFrame, sentFrames.first())
        assertArrayEquals(dimmedFrame, sentFrames.last())
    }

    @Test
    fun resettingForAnotherSessionResendsAnIdenticalFrame() {
        val frame = intArrayOf(0, 4095, 0)
        send(frame)
        delivery.reset()
        send(frame)

        assertEquals(2, sentFrames.size)
    }

    @Test
    fun failedFirstSubmissionCanBeRetried() {
        val frame = intArrayOf(0, 4095, 0)
        assertThrows(IllegalStateException::class.java) {
            delivery.sendIfNeeded(frame, nowMillis = 0L) { error("Connection lost") }
        }

        send(frame)

        assertEquals(1, sentFrames.size)
        assertArrayEquals(frame, sentFrames.single())
    }

    @Test
    fun failedSubmissionKeepsThePreviousSuccessfulFrame() {
        val previousFrame = intArrayOf(0, 4095, 0)
        val nextFrame = intArrayOf(0, 1024, 0)
        send(previousFrame)
        assertThrows(IllegalStateException::class.java) {
            delivery.sendIfNeeded(nextFrame, nowMillis = 0L) { error("Connection lost") }
        }

        send(previousFrame)
        send(nextFrame)

        assertEquals(2, sentFrames.size)
        assertArrayEquals(nextFrame, sentFrames.last())
    }

    @Test
    fun callerMutationDoesNotChangeTheRememberedFrame() {
        val frame = intArrayOf(0, 4095, 0)
        send(frame)
        frame[1] = 1024
        send(frame)

        assertEquals(2, sentFrames.size)
        assertArrayEquals(intArrayOf(0, 4095, 0), sentFrames.first())
        assertArrayEquals(intArrayOf(0, 1024, 0), sentFrames.last())
    }

    @Test
    fun silentSdkFailureIsRetriedEvenWhenPixelsNeverChange() {
        val frame = intArrayOf(0, 4095, 0)
        var submissions = 0
        val sdk: (IntArray) -> Unit = {
            submissions += 1
            // The SDK logs RemoteException and returns normally when a transaction fails.
            if (submissions > 1) sentFrames.add(it.copyOf())
        }
        delivery.sendIfNeeded(frame, nowMillis = 0L, send = sdk)
        delivery.sendIfNeeded(frame, nowMillis = 999L, send = sdk)
        assertEquals(1, submissions)
        assertEquals(0, sentFrames.size)

        delivery.sendIfNeeded(frame, nowMillis = 1_000L, send = sdk)
        delivery.sendIfNeeded(frame, nowMillis = 1_999L, send = sdk)

        assertEquals(2, submissions)
        assertArrayEquals(frame, sentFrames.single())
    }

    @Test
    fun changedPixelsResetTheRetryDeadline() {
        send(intArrayOf(0, 4095, 0), nowMillis = 0L)
        val changed = intArrayOf(0, 1024, 0)
        send(changed, nowMillis = 500L)
        send(changed, nowMillis = 1_000L)
        assertEquals(2, sentFrames.size)

        send(changed, nowMillis = 1_500L)

        assertEquals(3, sentFrames.size)
    }

    private fun send(
        frame: IntArray,
        nowMillis: Long = 0L,
    ) {
        delivery.sendIfNeeded(frame, nowMillis) { sentFrames.add(it.copyOf()) }
    }
}
