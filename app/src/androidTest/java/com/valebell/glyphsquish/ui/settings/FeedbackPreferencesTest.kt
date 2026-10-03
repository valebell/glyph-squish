package com.valebell.glyphsquish.ui.settings

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackPreferencesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = FeedbackPreferences(context)

    @After
    fun resetPreferences() =
        runBlocking {
            preferences.setSoundEnabled(true)
            preferences.setVibrationEnabled(true)
        }

    @Test
    fun feedbackChoicesArePersisted() =
        runBlocking {
            preferences.setSoundEnabled(false)
            preferences.setVibrationEnabled(false)

            val restoredPreferences = FeedbackPreferences(context)

            assertFalse(restoredPreferences.settings.first().isSoundEnabled)
            assertFalse(restoredPreferences.settings.first().isVibrationEnabled)

            restoredPreferences.setSoundEnabled(true)
            restoredPreferences.setVibrationEnabled(true)

            assertTrue(FeedbackPreferences(context).settings.first().isSoundEnabled)
            assertTrue(FeedbackPreferences(context).settings.first().isVibrationEnabled)
        }
}
