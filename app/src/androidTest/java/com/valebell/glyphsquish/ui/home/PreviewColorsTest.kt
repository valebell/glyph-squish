package com.valebell.glyphsquish.ui.home

import androidx.compose.foundation.layout.width
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.ui.slime.rememberSlimeMatrixState
import com.valebell.glyphsquish.ui.theme.SquishPreviewBackground
import com.valebell.glyphsquish.ui.theme.SquishPreviewContent
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PreviewColorsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deviceColorsThemeTheCardAndButtonWhileTheMatrixStaysMonochrome() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val deviceColors = mutableStateOf(false)
        val darkTheme = mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val preview = rememberSlimeMatrixState()
            SquishTheme(darkTheme = darkTheme.value, useDeviceColors = deviceColors.value) {
                PreviewPanel(
                    creature = CreatureProgression.initialState(),
                    selectedSection = SquishSection.MOVES,
                    previewState = preview,
                    previewSize = 100.dp,
                    compact = false,
                    onReplayMove = {},
                    modifier = Modifier.width(320.dp).testTag("preview-card"),
                )
            }
        }
        assertPreviewColors(SquishPreviewBackground, SquishPreviewContent)

        composeRule.runOnIdle { darkTheme.value = true }
        composeRule.mainClock.advanceTimeByFrame()
        assertPreviewColors(SquishPreviewBackground, SquishPreviewContent)
        composeRule.runOnIdle { darkTheme.value = false }

        composeRule.runOnIdle { deviceColors.value = true }
        composeRule.mainClock.advanceTimeByFrame()
        val lightColors = dynamicLightColorScheme(context)
        assertPreviewColors(lightColors.surfaceContainer, lightColors.primary)

        composeRule.runOnIdle { darkTheme.value = true }
        composeRule.mainClock.advanceTimeByFrame()
        val darkColors = dynamicDarkColorScheme(context)
        assertPreviewColors(darkColors.surfaceContainer, darkColors.primary)

        composeRule.runOnIdle { deviceColors.value = false }
        composeRule.mainClock.advanceTimeByFrame()
        assertPreviewColors(SquishPreviewBackground, SquishPreviewContent)
    }

    private fun assertPreviewColors(
        container: Color,
        button: Color,
    ) {
        val textLayouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Blob / Burst").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(textLayouts)
        }
        val identityColor =
            textLayouts
                .single()
                .layoutInput.style.color
        val contrast = ColorUtils.calculateContrast(identityColor.toArgb(), container.toArgb())
        assertTrue("Preview identity contrast must be at least 4.5:1; was $contrast", contrast >= 4.5)
        // Sample clear surface areas away from text, rounded corners, and the card's border.
        val card = composeRule.onNodeWithTag("preview-card").captureToImage().asAndroidBitmap()
        assertEquals("Preview card follows the chosen theme", container.toArgb(), card.getPixel(card.width / 2, 10))
        val replay = composeRule.onNodeWithText("Replay move").captureToImage().asAndroidBitmap()
        assertEquals("Replay button follows the chosen theme", button.toArgb(), replay.getPixel(replay.width / 2, 10))
        val matrix =
            composeRule
                .onNodeWithContentDescription(
                    "Live preview of the Blob form",
                ).captureToImage()
                .asAndroidBitmap()
        assertEquals(
            "Only the surrounding card is tinted, keeping the physical display faithful",
            SquishPreviewBackground.toArgb(),
            matrix.getPixel(matrix.width / 33, matrix.height / 2),
        )
    }
}
