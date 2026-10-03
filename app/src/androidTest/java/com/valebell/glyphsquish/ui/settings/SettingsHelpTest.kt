package com.valebell.glyphsquish.ui.settings

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.ui.audio.GlyphSoundEffect
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsHelpTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val feedback = mutableListOf<GlyphSoundEffect>()

    @Test
    fun helpPlaysNavigationFeedbackOnceOnButtonDismissal() {
        setContent()
        openHelp()

        composeRule.onNodeWithText("Got it").performClick()

        assertHelpDismissed()
    }

    @Test
    fun helpPlaysNavigationFeedbackOnceOnAndroidBackDismissal() {
        setContent()
        openHelp()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        assertHelpDismissed()
    }

    private fun setContent() {
        composeRule.setContent {
            SquishTheme {
                SettingsScreen(
                    versionName = "1.0",
                    feedback =
                        FeedbackState(
                            settings = FeedbackSettings(),
                            hasError = false,
                            actions = FeedbackActions({}, {}, {}),
                        ),
                    appearance =
                        AppearanceState(
                            settings = AppearanceSettings(),
                            hasError = false,
                            actions = AppearanceActions({}, {}),
                        ),
                    onOpenGlyphToys = { true },
                    onBack = {},
                    playFeedback = { feedback.add(it) },
                )
            }
        }
    }

    private fun openHelp() {
        composeRule.onNodeWithText("How to play").performScrollTo().performClick()
        composeRule.onNode(isDialog()).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf(GlyphSoundEffect.SLIDE_FORWARD), feedback)
        }
    }

    private fun assertHelpDismissed() {
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("How to play").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(
                listOf(GlyphSoundEffect.SLIDE_FORWARD, GlyphSoundEffect.SLIDE_BACKWARD),
                feedback,
            )
        }
    }
}
