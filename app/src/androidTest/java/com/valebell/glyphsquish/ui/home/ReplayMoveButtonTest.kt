package com.valebell.glyphsquish.ui.home

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.ui.slime.SlimeMatrixState
import com.valebell.glyphsquish.ui.slime.rememberSlimeMatrixState
import com.valebell.glyphsquish.ui.theme.LocalPreviewColors
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReplayMoveButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun regularPreviewShowsPlaybackAndPreventsRepeatedReplays() {
        assertPreviewPlayback(compact = false)
    }

    @Test
    fun compactPreviewShowsPlaybackAndPreventsRepeatedReplays() {
        assertPreviewPlayback(compact = true)
    }

    @Test
    fun reducedMotionKeepsTheDottedShimmerStillAndReadable() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SquishTheme {
                Box(Modifier.background(LocalPreviewColors.current.container)) {
                    ReplayMoveButton(isPlaying = true, onClick = {}, animationsEnabled = false)
                }
            }
        }
        val playingButton = composeRule.onNodeWithText("Replay move")
        playingButton.assertIsNotEnabled()
        composeRule.mainClock.advanceTimeBy(1_000L)
        val before = playingButton.captureToImage().asAndroidBitmap()

        composeRule.mainClock.advanceTimeBy(1_000L)

        val after = playingButton.captureToImage().asAndroidBitmap()
        assertTrue("Reduced motion must keep the shimmer still", before.sameAs(after))
        assertTrue("Playback fades away the solid button fill", before.brightPixelFraction() < 0.25f)
        assertTrue("The shimmer should contain distinct dots", before.maxRowShadeTransitions() >= 8)
        playingButton.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Playing move"))
    }

    @Test
    fun playbackHidesTheContentAndShimmersWithoutChangingTheButtonSize() {
        val playing = mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SquishTheme {
                Box(Modifier.background(LocalPreviewColors.current.container)) {
                    ReplayMoveButton(isPlaying = playing.value, onClick = {})
                }
            }
        }
        val button = composeRule.onNodeWithText("Replay move")
        val readyBounds = button.fetchSemanticsNode().boundsInRoot
        assertTrue("Ready button shows its label and icon", button.captureToImage().asAndroidBitmap().hasLabelInk())

        composeRule.runOnIdle { playing.value = true }
        composeRule.mainClock.advanceTimeBy(400)
        val before = button.captureToImage().asAndroidBitmap()
        composeRule.mainClock.advanceTimeBy(400)
        val after = button.captureToImage().asAndroidBitmap()

        assertEquals(readyBounds, button.fetchSemanticsNode().boundsInRoot)
        assertFalse("The shimmer should move during playback", before.sameAs(after))
        assertTrue("The shimmer must remain easy to see", after.contentBrightnessRange() >= 90)
        assertTrue("Playback fades away the solid button fill", after.brightPixelFraction() < 0.25f)
        composeRule.onNodeWithText("Playing move").assertDoesNotExist()

        composeRule.runOnIdle { playing.value = false }
        composeRule.mainClock.advanceTimeBy(400)
        val restored = button.captureToImage().asAndroidBitmap()
        assertTrue("The button fill returns after playback", restored.brightPixelFraction() > 0.6f)
        assertTrue("The label and icon return after playback", restored.hasLabelInk())
    }

    private fun assertPreviewPlayback(compact: Boolean) {
        lateinit var state: SlimeMatrixState
        var replayCount = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            state = rememberSlimeMatrixState()
            SquishTheme {
                PreviewPanel(
                    creature = CreatureProgression.initialState(),
                    selectedSection = SquishSection.MOVES,
                    previewState = state,
                    previewSize = 100.dp,
                    compact = compact,
                    onReplayMove = {
                        replayCount++
                        state.triggerButtonAction(SlimeButtonAction.BURST)
                    },
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        val readyButton = composeRule.onNodeWithText("Replay move")
        val readyWidth = readyButton.fetchSemanticsNode().boundsInRoot.width

        readyButton.assertIsEnabled().performClick()
        composeRule.mainClock.advanceTimeByFrame()

        val playingButton = composeRule.onNodeWithText("Replay move")
        playingButton.assertIsNotEnabled().performClick()
        playingButton.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Playing move"))
        assertEquals(readyWidth, playingButton.fetchSemanticsNode().boundsInRoot.width, 0.1f)
        assertEquals(1, replayCount)

        composeRule.runOnIdle { repeat(100) { state.step(0.1f) } }
        composeRule.mainClock.advanceTimeByFrame()

        readyButton.assertIsEnabled()
        readyButton.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ready to replay"))
        readyButton.performClick()
        assertEquals(2, replayCount)
    }

    // The ready button's foreground ink is nearly black against its bright fill.
    private fun Bitmap.hasLabelInk(): Boolean =
        (height / 4 until height * 3 / 4).any { y ->
            (width / 10 until width * 9 / 10).any { x ->
                val pixel = getPixel(x, y)
                Color.red(pixel) < 48 && Color.green(pixel) < 48 && Color.blue(pixel) < 48
            }
        }

    private fun Bitmap.contentBrightnessRange(): Int {
        val brightness =
            (height / 4 until height * 3 / 4).flatMap { y ->
                (width / 10 until width * 9 / 10).map { x -> Color.red(getPixel(x, y)) }
            }
        return brightness.max() - brightness.min()
    }

    private fun Bitmap.brightPixelFraction(): Float {
        val pixels =
            (height / 4 until height * 3 / 4).flatMap { y ->
                (width / 10 until width * 9 / 10).map { x -> getPixel(x, y) }
            }
        return pixels.count { Color.red(it) > 60 }.toFloat() / pixels.size
    }

    private fun Bitmap.maxRowShadeTransitions(): Int =
        (height / 4 until height * 3 / 4).maxOf { y ->
            (width / 10 until width * 9 / 10)
                .map { x -> Color.red(getPixel(x, y)) > 60 }
                .zipWithNext()
                .count { (before, after) -> before != after }
        }
}
