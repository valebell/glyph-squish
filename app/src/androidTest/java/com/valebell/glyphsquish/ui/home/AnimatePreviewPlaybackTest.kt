package com.valebell.glyphsquish.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.slime.SlimePhenotype
import com.valebell.glyphsquish.ui.slime.SlimeMatrixState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnimatePreviewPlaybackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun finishesPlaybackWhileThePreviewIsNotComposed() {
        val state = newState()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AnimatePreviewPlayback(state, ready = true)
        }

        composeRule.runOnIdle {
            state.triggerButtonAction(SlimeButtonAction.BURST)
            assertTrue(state.isMovePlaying)
        }
        composeRule.mainClock.advanceTimeBy(3_000L)

        composeRule.runOnIdle {
            assertFalse("offscreen playback must finish so shakes are accepted", state.isMovePlaying)
            assertNull(state.snapshot().activeButtonAction)
        }
    }

    @Test
    fun holdsPlaybackWhileTheScreenIsPausedAndFinishesAfterResume() {
        val state = newState()
        val fixture = PlaybackFixture()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides fixture) {
                AnimatePreviewPlayback(state, ready = true)
            }
        }

        composeRule.runOnIdle {
            fixture.lifecycle.currentState = Lifecycle.State.STARTED
            state.triggerButtonAction(SlimeButtonAction.BURST)
        }
        composeRule.mainClock.advanceTimeBy(3_000L)
        composeRule.runOnIdle { assertTrue("paused screens must not step the engine", state.isMovePlaying) }

        composeRule.runOnIdle { fixture.lifecycle.currentState = Lifecycle.State.RESUMED }
        composeRule.mainClock.advanceTimeBy(3_000L)
        composeRule.runOnIdle { assertFalse(state.isMovePlaying) }
    }

    @Test
    fun doesNotStepUntilReady() {
        val state = newState()
        val ready = mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AnimatePreviewPlayback(state, ready = ready.value)
        }

        composeRule.runOnIdle { state.triggerButtonAction(SlimeButtonAction.BURST) }
        composeRule.mainClock.advanceTimeBy(3_000L)
        composeRule.runOnIdle { assertTrue("unready screens must not step the engine", state.isMovePlaying) }

        composeRule.runOnIdle { ready.value = true }
        composeRule.mainClock.advanceTimeBy(3_000L)
        composeRule.runOnIdle { assertFalse(state.isMovePlaying) }
    }

    private fun newState() = SlimeMatrixState(SlimePhenotype.DEFAULT) { null }

    private class PlaybackFixture : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }
}
