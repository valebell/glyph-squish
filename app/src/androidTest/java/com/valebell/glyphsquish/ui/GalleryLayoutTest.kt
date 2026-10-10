package com.valebell.glyphsquish.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.slime.SlimeButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryLayoutTest : SquishUiTestFixture() {
    @Test
    fun formCardsUseConsistentAdaptiveWidths() {
        setContent()

        captureReviewScreenshot("forms")
        val statusSize =
            composeRule
                .onNodeWithText("Unlock Ricochet to reveal", useUnmergedTree = true)
                .fetchSemanticsNode()
                .size

        val cardSizes =
            listOf(
                "Blob form",
                "Shark form, locked. Unlock Ricochet to reveal.",
                "Cat form, locked. Unlock Echo to reveal.",
                "Star form, locked. Unlock Thunderbolt to reveal.",
                "Butterfly form, locked. Unlock Helix to reveal.",
            ).mapIndexed { index, description ->
                scrollGalleryTo("form-${CreatureForm.entries[index]}")
                composeRule
                    .onNodeWithContentDescription(description)
                    .fetchSemanticsNode()
                    .size
            }
        val firstCard = cardSizes.first()
        cardSizes.drop(1).forEach { size ->
            assertEquals(firstCard.width, size.width)
        }
        assertTrue(statusSize.height > 0)
        val formerFixedHeightPx =
            232f *
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext
                    .resources
                    .displayMetrics
                    .density
        assertTrue(firstCard.height < formerFixedHeightPx)
    }

    @Test
    fun scrollingTheGalleryMovesThePreviewWithTheScreen() {
        setContent()

        val preview =
            composeRule.onNodeWithContentDescription(
                "Live preview of the Blob form",
            )
        val initialPreviewTop = preview.fetchSemanticsNode().boundsInRoot.top

        scrollGalleryTo("form-BUTTERFLY")
        advanceUi()

        val scrolledPreviewTop = preview.fetchSemanticsNode().boundsInRoot.top
        assertTrue(scrolledPreviewTop < initialPreviewTop)
    }

    @Test
    fun landscapeKeepsPreviewFixedAndAtLeastTwoFormColumnsVisible() {
        setContent(
            configurationOverride =
                DeviceConfigurationOverride.WindowSize(
                    DpSize(800.dp, 400.dp),
                ),
        )

        val preview =
            composeRule.onNodeWithContentDescription(
                "Live preview of the Blob form",
            )
        val firstForm =
            composeRule.onNodeWithContentDescription("Blob form")
        val secondForm =
            composeRule.onNodeWithContentDescription("Shark form, locked. Unlock Ricochet to reveal.")

        preview.assertIsDisplayed()
        firstForm.assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show moves").assertIsDisplayed()

        val initialPreviewTop = preview.fetchSemanticsNode().boundsInRoot.top
        val firstFormBounds = firstForm.fetchSemanticsNode().boundsInRoot
        scrollGalleryTo("form-SHARK")
        advanceUi()
        val secondFormBounds = secondForm.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected a second column after x=${firstFormBounds.left}, but was x=${secondFormBounds.left}",
            secondFormBounds.left > firstFormBounds.left,
        )
        secondForm.assertIsDisplayed()

        scrollGalleryTo("form-BUTTERFLY")
        advanceUi()

        assertEquals(initialPreviewTop, preview.fetchSemanticsNode().boundsInRoot.top, 1f)
    }

    @Test
    fun landscapePlacesTheFullReplayActionBelowThePreviewRow() {
        setContent(
            configurationOverride =
                DeviceConfigurationOverride.WindowSize(
                    DpSize(800.dp, 400.dp),
                ),
        )

        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()

        // Short landscape windows can require scrolling the preview panel to its action.
        composeRule
            .onNode(hasScrollAction() and hasAnyDescendant(hasText("Replay move")))
            .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, Float.MAX_VALUE)
            }
        advanceUi()

        val preview =
            composeRule
                .onNodeWithContentDescription(
                    "Live preview of the Blob form",
                ).fetchSemanticsNode()
                .boundsInRoot
        val replay =
            composeRule
                .onNodeWithText("Replay move")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        val previewLabel =
            composeRule
                .onNodeWithText("Live Glyph preview")
                .fetchSemanticsNode()
                .boundsInRoot
        val previewIdentity =
            composeRule
                .onNodeWithText("Blob / Burst")
                .fetchSemanticsNode()
                .boundsInRoot

        assertTrue("Replay action should sit below the preview", replay.top >= preview.bottom)
        assertTrue(
            "Preview identity should sit below the label",
            previewIdentity.top >= previewLabel.bottom,
        )
    }

    @Test
    fun largeTextKeepsPreviewGalleryAndSwitcherVisible() {
        setContent(
            configurationOverride = DeviceConfigurationOverride.FontScale(1.8f),
        )

        composeRule
            .onNodeWithContentDescription(
                "Live preview of the Blob form",
            ).assertIsDisplayed()
        scrollGalleryTo("form-BLOB")
        composeRule
            .onNodeWithContentDescription("Blob form")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show moves").assertIsDisplayed()
    }

    @Test
    fun doubleTextSizeKeepsRequirementsAndSelectionLabelsComplete() {
        setContent(
            configurationOverride =
                DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 640.dp)) then
                    DeviceConfigurationOverride.FontScale(2f),
        )

        captureReviewScreenshot("large-text")
        assertTextFits("Blob / Burst")
        scrollGalleryTo("form-BLOB")
        assertTextFits("Selected")
        scrollGalleryTo("form-STAR")
        assertTextFits("Unlock Thunderbolt to reveal")
        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()
        scrollGalleryTo("move-THUNDERBOLT")
        assertTextFits("Bounce between opposite edges, hold the Glyph Button, then circle the rim after your move finishes")
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Expected text layout for $text", layouts.isNotEmpty())
        val layout = layouts.single()
        // Layout dimensions are integer pixels; paragraph dimensions can retain fractions.
        assertFalse("Text must not be ellipsized: $text", layout.multiParagraph.didExceedMaxLines)
        // Compare actual line widths; aligned paragraphs can include unused horizontal space.
        repeat(layout.lineCount) { line ->
            val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
            assertTrue("Text must fit horizontally: $text", lineWidth <= layout.size.width + 1f)
        }
        assertTrue("Text must fit vertically: $text", layout.multiParagraph.height <= layout.size.height + 1f)
    }

    @Test
    fun portraitSelectionsReturnToPreview() {
        verifySelectionScrolling(DpSize(320.dp, 640.dp), returnsToTop = true)
    }

    @Test
    fun landscapeSelectionsKeepGalleryPosition() {
        verifySelectionScrolling(DpSize(800.dp, 400.dp), returnsToTop = false)
    }

    @Test
    fun narrowLandscapeSelectionsKeepGalleryPosition() {
        verifySelectionScrolling(DpSize(480.dp, 320.dp), returnsToTop = false)
    }

    private fun verifySelectionScrolling(
        size: DpSize,
        returnsToTop: Boolean,
    ) {
        val runtime =
            FakeCreatureRuntime().apply {
                unlock(SlimeButtonAction.THUNDERBOLT)
                selectMove(SlimeButtonAction.BURST)
            }
        setContent(DeviceConfigurationOverride.WindowSize(size), initialRuntime = runtime)
        scrollGalleryTo("form-STAR")
        val formOffset = galleryScrollOffset()
        assertTrue(formOffset > 0f)
        composeRule
            .onNodeWithContentDescription("Star form")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        advanceUi(2000)
        assertEquals(CreatureForm.STAR, runtime.creature.currentForm)
        assertEquals(if (returnsToTop) 0f else formOffset, galleryScrollOffset(), 1f)
        if (returnsToTop) {
            composeRule.onNodeWithText("Live Glyph preview").assertIsDisplayed()
        }
        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()
        scrollGalleryTo("move-THUNDERBOLT")
        val moveOffset = galleryScrollOffset()
        assertTrue(moveOffset > 0f)
        composeRule.onNodeWithText("Thunderbolt").performSemanticsAction(SemanticsActions.OnClick) { it() }
        advanceUi(2000)
        assertEquals(SlimeButtonAction.THUNDERBOLT, runtime.creature.selectedMove)
        assertEquals(if (returnsToTop) 0f else moveOffset, galleryScrollOffset(), 1f)
    }

    @Test
    fun portraitLockedAndCurrentSelectionsDoNotScroll() {
        setContent(DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 640.dp)))
        scrollGalleryTo("form-STAR")
        val lockedOffset = galleryScrollOffset()
        composeRule.onNodeWithContentDescription("Star form, locked. Unlock Thunderbolt to reveal.").performClick()
        advanceUi(1000)
        assertEquals(lockedOffset, galleryScrollOffset(), 1f)
        scrollGalleryTo("form-BLOB")
        val currentOffset = galleryScrollOffset()
        composeRule.onNodeWithContentDescription("Blob form").performClick()
        advanceUi(1000)
        assertEquals(currentOffset, galleryScrollOffset(), 1f)
        composeRule.onNodeWithContentDescription("Show moves").performClick()
        advanceUi()
        scrollGalleryTo("move-THUNDERBOLT")
        val lockedMoveOffset = galleryScrollOffset()
        composeRule.onNodeWithText("Thunderbolt").performSemanticsAction(SemanticsActions.OnClick) { it() }
        advanceUi(1000)
        assertEquals(lockedMoveOffset, galleryScrollOffset(), 1f)
        scrollGalleryTo("move-BURST")
        val currentMoveOffset = galleryScrollOffset()
        composeRule
            .onNodeWithContentDescription(
                "Burst move. Burst into pixels, then spiral home.",
            ).performClick()
        advanceUi(1000)
        assertEquals(currentMoveOffset, galleryScrollOffset(), 1f)
    }
}
