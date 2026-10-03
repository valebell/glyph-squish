package com.valebell.glyphsquish.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.creature.runtime.CreaturePersistenceError
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.ui.audio.GlyphSoundEffect
import com.valebell.glyphsquish.ui.home.HomeRoute
import com.valebell.glyphsquish.ui.home.SquishSection
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class SquishAppTest : SquishUiTestFixture() {
    @Test
    fun settingsHelpKeepsGestureGuidanceAvailableWithoutAStandingInstructionBlock() {
        setContent()
        composeRule.onNodeWithText("Tilt, shake, squish!").assertIsDisplayed()
        composeRule.onNodeWithText("Tilt your phone to move Squish.").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        advanceUi()
        composeRule
            .onNodeWithText("How to play")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        advanceUi()
        composeRule.onNodeWithText("Tilt your phone to move Squish.").assertIsDisplayed()
        composeRule
            .onNodeWithText("With Glyph Squish active on your Glyph Matrix:")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Hold the Glyph Button to squish and play your selected move.")
            .assertIsDisplayed()
        captureReviewScreenshot("help", dialog = true)
        composeRule.onNodeWithText("Got it").performClick()
        advanceUi()
        composeRule.onNodeWithText("Tilt your phone to move Squish.").assertDoesNotExist()
    }

    @Test
    fun startsOnFormsAndSwitchesTheGalleryAroundThePersistentPreview() {
        setContent()

        composeRule.onNodeWithContentDescription("Show forms").assertIsSelected()
        val preview =
            composeRule
                .onNodeWithContentDescription(
                    "Live preview of the Blob form",
                ).assertIsDisplayed()
        val previewTop = preview.fetchSemanticsNode().boundsInRoot.top

        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()

        composeRule.onNodeWithContentDescription("Show moves").assertIsSelected()
        preview.assertIsDisplayed()
        assertEquals(previewTop, preview.fetchSemanticsNode().boundsInRoot.top, 1f)
        composeRule.onNodeWithText("Replay move").assertIsDisplayed()
    }

    @Test
    fun lockedFormKeepsItsInlineRequirementWithoutOpeningADialog() {
        val runtime = setContent()
        val before = runtime.state.value

        val card =
            composeRule
                .onNodeWithContentDescription("Shark form, locked. Unlock Ricochet to reveal.")
                .assertHasClickAction()
                .assertIsDisplayed()
        composeRule.onNodeWithText("Unlock Ricochet to reveal", useUnmergedTree = true).assertIsDisplayed()
        card.performClick()
        advanceUi()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Unlock Ricochet to reveal", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(before, runtime.state.value)
        card.assertIsDisplayed()
    }

    @Test
    fun livePreviewExposesAnActionableSemanticClick() {
        setContent()

        composeRule
            .onNodeWithContentDescription(
                "Live preview of the Blob form",
            ).assertHasClickAction()
            .performClick()
    }

    @Test
    fun movesGalleryReplaysAndExplainsLockedMove() {
        setContent()

        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()
        composeRule.onNodeWithText("Replay move").assertIsDisplayed().performClick()
        val lockedMove =
            composeRule
                .onNodeWithContentDescription(
                    "Helix move, locked. Coil into an accelerating corkscrew. " +
                        "Recipe: Complete two circles around the rim in a row.",
                )
        scrollGalleryTo("move-HELIX")
        advanceUi()
        lockedMove.assertHasClickAction().assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        java.io
            .File(instrumentation.targetContext.getExternalFilesDir(null), "moves-reviewed.png")
            .outputStream()
            .use {
                instrumentation.uiAutomation.takeScreenshot().compress(
                    android.graphics.Bitmap.CompressFormat.PNG,
                    100,
                    it,
                )
            }
        composeRule
            .onNodeWithText(
                "Complete two circles around the rim in a row",
                useUnmergedTree = true,
            ).assertIsDisplayed()
    }

    @Test
    fun glyphToysActionLivesInSettings() {
        var launchCount = 0
        setContent(
            launchGlyphToys = {
                launchCount += 1
                true
            },
        )

        composeRule
            .onNodeWithContentDescription("Settings")
            .assertHasClickAction()
            .performClick()
        advanceUi()
        composeRule.onNodeWithText("Glyph Toys").performScrollTo().performClick()
        advanceUi()

        assertEquals(1, launchCount)
    }

    @Test
    fun settingsShowsVersionAndReturnsHome() {
        setContent()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        advanceUi()
        composeRule.onNodeWithText("Version").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1.0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to Glyph Squish").performClick()
        composeRule.waitUntil(5_000) {
            advanceUi()
            composeRule.onNodeWithContentDescription("Settings").isDisplayed()
        }
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun movesRemainInTheGalleryWithoutTrainingActions() {
        setContent()
        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()
        for (move in SlimeButtonAction.entries) {
            scrollGalleryTo("move-$move")
            composeRule.onAllNodes(hasText("Practice", substring = true)).assertCountEquals(0)
        }
        composeRule.onNodeWithContentDescription("Show moves").assertIsSelected()
    }

    @Test
    fun glyphLaunchErrorStaysInlineInSettings() {
        setContent(launchGlyphToys = { false })
        composeRule.onNodeWithContentDescription("Settings").performClick()
        advanceUi()
        composeRule.onNodeWithText("Glyph Toys").performScrollTo().performClick()
        advanceUi()
        composeRule
            .onNodeWithText(
                "Couldn’t open Glyph Toys. Go to Settings → Glyph Interface → Glyph Toys on your phone.",
            ).assertIsDisplayed()
        advanceUi(6_000)
        composeRule
            .onNodeWithText(
                "Couldn’t open Glyph Toys. Go to Settings → Glyph Interface → Glyph Toys on your phone.",
            ).assertIsDisplayed()
    }

    @Test
    fun saveErrorsUseAnInlineRetryAndDismissCard() {
        val runtime = setContent()
        composeRule.runOnIdle { runtime.mutablePersistenceErrors.tryEmit(CreaturePersistenceError("failed")) }
        advanceUi()
        composeRule.onNodeWithText("Could not save your changes").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        advanceUi()
        assertEquals(1, runtime.saveRetryCount)
        composeRule.onNodeWithText("Could not save your changes").assertDoesNotExist()
        composeRule.runOnIdle { runtime.mutablePersistenceErrors.tryEmit(CreaturePersistenceError("still failed")) }
        advanceUi()
        composeRule.onNodeWithText("Dismiss").performClick()
        advanceUi()
        composeRule.onNodeWithText("Could not save your changes").assertDoesNotExist()
    }

    @Test
    fun lockedTapsKeepTheirSoundWithoutChangingProgression() {
        composeRule.mainClock.autoAdvance = false
        val sounds = mutableListOf<GlyphSoundEffect>()
        val runtime = FakeCreatureRuntime()
        val viewModel = SquishViewModel(runtime)
        composeRule.setContent {
            SquishTheme {
                val state by runtime.state.collectAsState()
                var section by remember { mutableStateOf(SquishSection.FORMS) }
                HomeRoute(
                    uiState = state,
                    selectedSection = section,
                    onSectionChange = { section = it },
                    viewModel = viewModel,
                    playSound = { sounds.add(it) },
                    onOpenSettings = {},
                )
            }
        }
        advanceUi()
        val initialState = runtime.state.value
        composeRule.onNodeWithContentDescription("Shark form, locked. Unlock Ricochet to reveal.").performClick()
        advanceUi()
        assertEquals(initialState, runtime.state.value)
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()
        composeRule.onNodeWithText("Replay move").performClick()
        advanceUi()
        scrollGalleryTo("move-HELIX")
        composeRule
            .onNodeWithContentDescription(
                "Helix move, locked. Coil into an accelerating corkscrew. " +
                    "Recipe: Complete two circles around the rim in a row.",
            ).performClick()
        advanceUi()
        assertEquals(initialState, runtime.state.value)
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Show forms").performClick()
        advanceUi()
        assertEquals(
            listOf(
                GlyphSoundEffect.SELECT,
                GlyphSoundEffect.SLIDE_FORWARD,
                GlyphSoundEffect.forMove(SlimeButtonAction.BURST),
                GlyphSoundEffect.SELECT,
                GlyphSoundEffect.SLIDE_BACKWARD,
            ),
            sounds,
        )
    }
}
