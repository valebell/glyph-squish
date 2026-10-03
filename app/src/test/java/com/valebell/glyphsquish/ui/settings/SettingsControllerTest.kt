package com.valebell.glyphsquish.ui.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SettingsControllerTest {
    @Test
    fun failedReadEmitsNoFallbackAndRetryCollectsTheStoreAgain() =
        runBlocking {
            var reads = 0
            val controller =
                SettingsController(
                    flow {
                        if (++reads == 1) throw IOException("Unavailable")
                        emit(AppearanceSettings(useDeviceColors = true))
                    },
                )

            assertNull(controller.settingsFlow.firstOrNull())
            assertTrue(controller.hasError)
            controller.retry()

            assertFalse(controller.hasError)
            assertEquals(AppearanceSettings(useDeviceColors = true), controller.settingsFlow.first())
            assertEquals(2, reads)
        }

    @Test
    fun retryReplaysOnlyTheMostRecentFailedWriteUntilItSucceeds() =
        runBlocking {
            val controller = SettingsController(flowOf(FeedbackSettings()))
            var firstAttempts = 0
            var lastAttempts = 0
            var available = false
            controller.save {
                firstAttempts += 1
                throw IOException("First write failed")
            }
            controller.save {
                lastAttempts += 1
                if (!available) throw IOException("Last write failed")
            }
            controller.retry()
            assertTrue(controller.hasError)

            available = true
            controller.retry()
            assertFalse(controller.hasError)
            controller.retry()

            assertEquals(1, firstAttempts)
            assertEquals(3, lastAttempts)
        }

    @Test
    fun successfulNewWriteClearsTheFailedWrite() =
        runBlocking {
            val controller = SettingsController(flowOf(AppearanceSettings()))
            var failedAttempts = 0
            var saved: Boolean? = null
            controller.save {
                failedAttempts += 1
                throw IOException("Unavailable")
            }
            controller.save { saved = true }
            controller.retry()

            assertEquals(true, saved)
            assertEquals(1, failedAttempts)
            assertFalse(controller.hasError)
        }

    @Test
    fun unrelatedFailuresPropagateInsteadOfBecomingRetryableIoErrors() {
        val failure = IllegalStateException("Programming error")
        val controller = SettingsController(flow<Boolean> { throw failure })

        val readFailure =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { controller.settingsFlow.firstOrNull() }
            }
        val writeFailure =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { controller.save { throw failure } }
            }
        assertSame(failure, readFailure)
        assertSame(failure, writeFailure)
        assertFalse(controller.hasError)
    }

    @Test
    fun eachStoreKeepsItsOwnErrorAndPendingWrite() =
        runBlocking {
            val appearance = SettingsController(flowOf(AppearanceSettings()))
            val feedback = SettingsController(flowOf(FeedbackSettings()))
            var attempts = 0
            appearance.save {
                attempts += 1
                if (attempts == 1) throw IOException("Appearance store unavailable")
            }

            feedback.save {}
            feedback.retry()
            assertTrue(appearance.hasError)
            assertFalse(feedback.hasError)
            assertEquals(1, attempts)

            appearance.retry()
            assertFalse(appearance.hasError)
            assertEquals(2, attempts)
        }
}
