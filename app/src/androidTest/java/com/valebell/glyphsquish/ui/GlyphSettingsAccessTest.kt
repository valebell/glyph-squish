package com.valebell.glyphsquish.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphSettingsAccessTest : SquishUiTestFixture() {
    @Test
    fun homeKeepsGlyphSetupInSettings() {
        var launchCount = 0
        setContent(launchGlyphToys = {
            launchCount++
            true
        })
        assertHomeHasNoSetupCard()
        captureReviewScreenshot("home")
        composeRule.onNodeWithContentDescription("Settings").performClick()
        advanceUi()
        composeRule
            .onNodeWithText("Glyph Toys")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        advanceUi()
        assertEquals(1, launchCount)
        composeRule.onNodeWithContentDescription("Back to Glyph Squish").performClick()
        advanceUi()
        assertHomeHasNoSetupCard()
    }

    @Test
    fun landscapeHasNoSetupCard() {
        setContent(
            configurationOverride = DeviceConfigurationOverride.WindowSize(DpSize(800.dp, 400.dp)),
        )
        assertHomeHasNoSetupCard()
        captureReviewScreenshot("home-landscape")
    }

    @Test
    fun failedSettingsLaunchProvidesManualPathAtLargeText() {
        setContent(
            configurationOverride =
                DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 640.dp)) then
                    DeviceConfigurationOverride.FontScale(2f),
            launchGlyphToys = { false },
        )
        composeRule.onNodeWithContentDescription("Settings").performClick()
        advanceUi()
        // Settings scrolling animates; the live-preview test clock can advance here.
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("Glyph Toys").performScrollTo().performClick()
        advanceUi()
        val fallback = "Couldn’t open Glyph Toys. Go to Settings → Glyph Interface → Glyph Toys on your phone."
        composeRule.onNodeWithText(fallback).performScrollTo().assertIsDisplayed()
        assertTextFits(fallback)
        captureReviewScreenshot("glyph-settings-large-text")
    }

    private fun assertHomeHasNoSetupCard() {
        composeRule.onNodeWithText("Tilt, shake, squish!").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show forms").assertIsDisplayed()
        composeRule.onNodeWithText("Add Squish to your Glyph").assertDoesNotExist()
        composeRule.onNodeWithText("Open Glyph Toys").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Dismiss Glyph setup suggestion").assertDoesNotExist()
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertFalse(layout.multiParagraph.didExceedMaxLines)
        assertTrue(layout.multiParagraph.height <= layout.size.height + 1f)
        repeat(layout.lineCount) { line ->
            assertTrue(layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1f)
        }
    }
}
