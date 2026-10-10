package com.valebell.glyphsquish.ui

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Test

class OffscreenPlaybackTest : SquishUiTestFixture() {
    @Test
    fun scrollingThePreviewAwayMidMoveLetsPlaybackFinishOffscreen() {
        setContent(
            configurationOverride =
                DeviceConfigurationOverride.WindowSize(
                    DpSize(420.dp, 900.dp),
                ),
        )

        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()

        composeRule.onNodeWithText("Replay move").assertIsEnabled().performClick()
        advanceUi()
        composeRule.onNodeWithText("Replay move").assertIsNotEnabled()
        scrollGalleryTo("move-THUNDERBOLT")
        advanceUi(3_000L)

        scrollGalleryTo("preview")
        advanceUi()

        composeRule
            .onNodeWithText("Replay move")
            .assertIsEnabled()
    }
}
