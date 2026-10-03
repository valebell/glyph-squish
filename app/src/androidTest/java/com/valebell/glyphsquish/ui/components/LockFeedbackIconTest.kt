package com.valebell.glyphsquish.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LockFeedbackIconTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lockWigglesWithoutMovingItsSlotAndRapidTapsSettleWithoutQueuing() {
        val trigger = mutableIntStateOf(0)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SquishTheme {
                Box(
                    modifier = Modifier.size(72.dp).testTag("lock-slot"),
                    contentAlignment = Alignment.Center,
                ) {
                    LockFeedbackIcon(trigger.intValue, Modifier.size(26.dp))
                }
            }
        }
        val slot = compose.onNodeWithTag("lock-slot")

        fun frame() = slot.captureToImage().asAndroidBitmap()

        fun advance(millis: Long) {
            compose.mainClock.advanceTimeBy(millis)
            compose.waitForIdle()
        }

        val still = frame()
        val bounds = slot.fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { trigger.intValue += 1 }
        advance(96)
        assertFalse("A locked tap should visibly wiggle the icon", still.sameAs(frame()))
        assertEquals("The surrounding layout stays fixed", bounds, slot.fetchSemanticsNode().boundsInRoot)

        compose.runOnIdle { trigger.intValue += 1 }
        advance(96)
        assertFalse("A second tap restarts the feedback", still.sameAs(frame()))
        advance(272)
        assertTrue("Rapid taps must settle after one wiggle instead of queueing", still.sameAs(frame()))
        assertEquals(bounds, slot.fetchSemanticsNode().boundsInRoot)
    }
}
