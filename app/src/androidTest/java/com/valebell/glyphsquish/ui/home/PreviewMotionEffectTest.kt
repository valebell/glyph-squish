package com.valebell.glyphsquish.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PreviewMotionEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startsOnlyWhileResumedAndStopsOnPauseAndDisposal() {
        val motion = installMotionEffect(Lifecycle.State.CREATED, ready = true)
        assertCalls(motion, starts = 0, stops = 0)

        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.STARTED }
        assertCalls(motion, starts = 0, stops = 0)
        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.RESUMED }
        assertCalls(motion, starts = 1, stops = 0)
        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.RESUMED }
        assertCalls(motion, starts = 1, stops = 0)

        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.STARTED }
        assertCalls(motion, starts = 1, stops = 1)
        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.CREATED }
        assertCalls(motion, starts = 1, stops = 1)
        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.RESUMED }
        assertCalls(motion, starts = 2, stops = 1)

        composeRule.runOnIdle { motion.showEffect.value = false }
        assertCalls(motion, starts = 2, stops = 2)
    }

    @Test
    fun loadingAndFailureDoNotCollectMotionAndRecoveryStartsWhileAlreadyResumed() {
        val motion = installMotionEffect(Lifecycle.State.RESUMED, ready = false)
        assertCalls(motion, starts = 0, stops = 0)

        composeRule.runOnIdle { motion.ready.value = true }
        assertCalls(motion, starts = 1, stops = 0)
        composeRule.runOnIdle { motion.ready.value = false }
        assertCalls(motion, starts = 1, stops = 1)
        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.STARTED }
        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.RESUMED }
        assertCalls(motion, starts = 1, stops = 1)

        composeRule.runOnIdle { motion.ready.value = true }
        assertCalls(motion, starts = 2, stops = 1)
        composeRule.runOnIdle { motion.lifecycle.currentState = Lifecycle.State.STARTED }
        composeRule.runOnIdle { motion.showEffect.value = false }
        assertCalls(motion, starts = 2, stops = 2)
    }

    @Test
    fun replacingSensorStopsTheOldSensorBeforeStartingTheNewOne() {
        val motion = installMotionEffect(Lifecycle.State.RESUMED, ready = true)
        composeRule.runOnIdle { motion.sensorKey.value = 2 }

        composeRule.runOnIdle {
            assertEquals(listOf("start 1", "stop 1", "start 2"), motion.events)
        }
        composeRule.runOnIdle { motion.showEffect.value = false }
        composeRule.runOnIdle {
            assertEquals(listOf("start 1", "stop 1", "start 2", "stop 2"), motion.events)
        }
    }

    private fun installMotionEffect(
        initialState: Lifecycle.State,
        ready: Boolean,
    ): MotionFixture {
        val motion = MotionFixture(ready)
        composeRule.runOnUiThread { motion.lifecycle.currentState = initialState }
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides motion) {
                if (motion.showEffect.value) {
                    val sensorKey = motion.sensorKey.value
                    PreviewMotionEffect(
                        ready = motion.ready.value,
                        sensorKey = sensorKey,
                        start = { motion.events += "start $sensorKey" },
                        stop = { motion.events += "stop $sensorKey" },
                    )
                }
            }
        }
        return motion
    }

    private fun assertCalls(
        motion: MotionFixture,
        starts: Int,
        stops: Int,
    ) {
        composeRule.runOnIdle {
            assertEquals(starts, motion.events.count { it.startsWith("start") })
            assertEquals(stops, motion.events.count { it.startsWith("stop") })
        }
    }

    private class MotionFixture(
        ready: Boolean,
    ) : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
        val ready = mutableStateOf(ready)
        val showEffect = mutableStateOf(true)
        val sensorKey = mutableStateOf(1)
        val events = mutableListOf<String>()
    }
}
