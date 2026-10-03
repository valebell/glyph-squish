package com.valebell.glyphsquish.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.ui.SquishUiTestFixture
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test

class FeedbackSettingsUiTest : SquishUiTestFixture() {
    private val preferences = FeedbackPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
    private lateinit var original: FeedbackSettings

    @Before
    fun disableFeedback() =
        runBlocking {
            original = preferences.settings.first()
            preferences.setSoundEnabled(false)
            preferences.setVibrationEnabled(false)
        }

    @After
    fun restoreFeedback() =
        runBlocking {
            preferences.setSoundEnabled(original.isSoundEnabled)
            preferences.setVibrationEnabled(original.isVibrationEnabled)
        }

    @Test
    fun settingsReflectStoredValuesAndObserveChangesFromAnotherOwner() {
        setContent()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        advanceUi()
        // The animated preview is now disposed; let switch and scroll animations finish normally.
        composeRule.mainClock.autoAdvance = true
        val sound = hasText("Sound") and isToggleable()
        val vibration = hasText("Vibration") and isToggleable()
        composeRule.onNode(sound).assertIsOff()
        composeRule.onNode(vibration).assertIsOff()

        runBlocking {
            preferences.setSoundEnabled(true)
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(sound and isOn()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(sound).assertIsOn()
        composeRule.onNode(vibration).assertIsOff()

        composeRule.onNode(vibration).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(vibration and isOn()).fetchSemanticsNodes().isNotEmpty()
        }
        // First drain the Compose scope that initiated the write, then verify durable storage.
        runBlocking { withTimeout(5_000) { preferences.settings.first { it.isVibrationEnabled } } }
    }
}
