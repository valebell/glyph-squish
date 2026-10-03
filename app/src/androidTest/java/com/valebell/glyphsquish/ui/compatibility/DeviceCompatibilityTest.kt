package com.valebell.glyphsquish.ui.compatibility

import androidx.compose.material3.Text
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeviceCompatibilityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun unsupportedDeviceNeverComposesTheCompanionAndCanClose() {
        compose.mainClock.autoAdvance = false
        var companionCreated = false
        var closed = false
        compose.setContent {
            SquishTheme {
                DeviceCompatibilityGate(isSupported = false, onClose = { closed = true }) {
                    companionCreated = true
                }
            }
        }
        compose.onNodeWithText("Only on Nothing Phone (3)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Close").performScrollTo().performClick()
        compose.runOnIdle {
            assertFalse(companionCreated)
            assertTrue(closed)
        }
    }

    @Test
    fun supportedDeviceEntersTheCompanionDirectly() {
        compose.setContent {
            SquishTheme {
                DeviceCompatibilityGate(isSupported = true, onClose = {}) {
                    Text("Companion content")
                }
            }
        }
        compose.onNodeWithText("Companion content").assertIsDisplayed()
        compose.onNodeWithText("Only on Nothing Phone (3)").assertDoesNotExist()
    }

    @Test
    fun reducedMotionAtLargeFontSizeKeepsTheMessageAndCloseReachable() {
        // The greeting is static; allow the clock to finish accessibility scroll animations.
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(DpSize(640.dp, 320.dp)),
            ) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    SquishTheme(darkTheme = true) {
                        UnsupportedDeviceScreen(onClose = {}, animationsEnabled = false)
                    }
                }
            }
        }
        compose.onNodeWithText("Hey! I’m Squish.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Only on Nothing Phone (3)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Close").performScrollTo().assertIsDisplayed()
    }
}
