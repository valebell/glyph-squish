package com.valebell.glyphsquish.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormDiscoveryTest : SquishUiTestFixture() {
    @Test
    fun discoveryGlowWaitsForVisibilityAndPersistsUntilSelection() {
        val runtime = setContent(DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 480.dp)))
        composeRule.runOnIdle { runtime.unlock(SlimeButtonAction.THUNDERBOLT) }
        advanceUi()
        composeRule.runOnIdle { assertTrue(CreatureForm.STAR !in runtime.creature.seenForms) }
        scrollGalleryTo("form-STAR")
        composeRule.onNodeWithText("New").assertDoesNotExist()
        composeRule
            .onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "New"),
            ).assertIsDisplayed()
        composeRule.runOnIdle {
            assertTrue(CreatureForm.STAR !in runtime.creature.seenForms)
            assertEquals(CreatureForm.BLOB, runtime.creature.currentForm)
        }
        scrollGalleryTo("form-BLOB")
        scrollGalleryTo("form-STAR")
        composeRule.onNodeWithText("New").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()
        composeRule.onNodeWithContentDescription("Show forms").performClick()
        advanceUi()
        scrollGalleryTo("form-STAR")
        val discovery = composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "New"))
        discovery.assertIsDisplayed().performClick()
        advanceUi()
        discovery.assertDoesNotExist()
        composeRule.runOnIdle { assertTrue(CreatureForm.STAR in runtime.creature.seenForms) }
        scrollGalleryTo("form-BLOB")
        composeRule.onNodeWithText("Blob").performClick()
        advanceUi()
        scrollGalleryTo("form-STAR")
        discovery.assertDoesNotExist()
    }

    @Test
    fun alreadySelectedNewFormNeverShowsDiscoveryEmphasis() {
        setContent(
            initialRuntime =
                FakeCreatureRuntime().apply {
                    unlock(SlimeButtonAction.THUNDERBOLT)
                    restoreUnseenSelection(CreatureForm.STAR)
                },
        )
        scrollGalleryTo("form-STAR")
        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "New"))
            .assertDoesNotExist()
        composeRule.onNodeWithText("New").assertDoesNotExist()
    }
}
