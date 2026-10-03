package com.valebell.glyphsquish.ui.settings

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.ui.theme.SquishDarkBackground
import com.valebell.glyphsquish.ui.theme.SquishDarkRed
import com.valebell.glyphsquish.ui.theme.SquishLightBackground
import com.valebell.glyphsquish.ui.theme.SquishLightRed
import com.valebell.glyphsquish.ui.theme.SquishTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppearanceSettingsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = AppearancePreferences(context)
    private var original = AppearanceSettings()
    private val darkTheme = mutableStateOf(false)
    private lateinit var observedColors: ColorScheme
    private val deviceColorsSwitch = hasText("Use device colors") and isToggleable()

    @Before
    fun useSquishColors() =
        runBlocking {
            original = preferences.settings.first()
            preferences.setUseDeviceColors(false)
        }

    @After
    fun restoreAppearance() =
        runBlocking {
            preferences.setUseDeviceColors(original.useDeviceColors)
        }

    @Test
    fun settingAppliesDeviceColorsImmediatelyAndPersistsTheChoice() {
        setContent()
        composeRule.onNode(deviceColorsSwitch).assertIsOff()
        composeRule.runOnIdle {
            assertEquals(SquishLightRed, observedColors.secondary)
            assertEquals(SquishLightBackground, observedColors.background)
        }

        composeRule.onNode(deviceColorsSwitch).performClick()
        waitForDeviceColors(enabled = true)
        composeRule.onNode(deviceColorsSwitch).assertIsOn()
        assertPalette(dynamicLightColorScheme(context))
        runBlocking {
            withTimeout(5_000) {
                AppearancePreferences(context).settings.first { it.useDeviceColors }
            }
        }

        // Changes from another owner must update both the switch and the current theme.
        runBlocking { preferences.setUseDeviceColors(false) }
        waitForDeviceColors(enabled = false)
        composeRule.onNode(deviceColorsSwitch).assertIsOff()
        composeRule.runOnIdle {
            assertEquals(SquishLightRed, observedColors.secondary)
            assertEquals(SquishLightBackground, observedColors.background)
        }
    }

    @Test
    fun storedOptInUsesTheMatchingLightAndDarkDevicePalette() {
        runBlocking { preferences.setUseDeviceColors(true) }
        setContent()
        waitForDeviceColors(enabled = true)
        assertPalette(dynamicLightColorScheme(context))

        composeRule.runOnIdle { darkTheme.value = true }
        assertPalette(dynamicDarkColorScheme(context))

        composeRule.onNode(deviceColorsSwitch).performClick()
        waitForDeviceColors(enabled = false)
        composeRule.runOnIdle {
            assertEquals(SquishDarkRed, observedColors.secondary)
            assertEquals(SquishDarkBackground, observedColors.background)
        }
    }

    private fun setContent() {
        composeRule.setContent {
            val appearance = rememberAppearanceState()
            SquishTheme(
                darkTheme = darkTheme.value,
                useDeviceColors = appearance.settings?.useDeviceColors == true,
            ) {
                val colors = MaterialTheme.colorScheme
                SideEffect { observedColors = colors }
                SettingsScreen(
                    versionName = "1.0",
                    feedback =
                        FeedbackState(
                            settings = FeedbackSettings(),
                            hasError = false,
                            actions = FeedbackActions({}, {}, {}),
                        ),
                    appearance = appearance,
                    onOpenGlyphToys = { true },
                    onBack = {},
                )
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(deviceColorsSwitch and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForDeviceColors(enabled: Boolean) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(deviceColorsSwitch and isOn()).fetchSemanticsNodes().isNotEmpty() == enabled
        }
    }

    private fun assertPalette(expected: ColorScheme) {
        composeRule.runOnIdle {
            assertEquals(expected.primary, observedColors.primary)
            assertEquals(expected.secondary, observedColors.secondary)
            assertEquals(expected.background, observedColors.background)
            assertEquals(expected.surface, observedColors.surface)
        }
    }
}
