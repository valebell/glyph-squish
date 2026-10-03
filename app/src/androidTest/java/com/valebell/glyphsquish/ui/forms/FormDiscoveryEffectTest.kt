package com.valebell.glyphsquish.ui.forms

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FormDiscoveryEffectTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun burstSettlesIntoBreathingGlowAndSelectionRemovesAllEmphasis() {
        val visible = mutableStateOf(false)
        val isNew = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SquishTheme {
                Box(
                    Modifier
                        .size(160.dp, 200.dp)
                        .testTag("card")
                        .formDiscoveryEffect(
                            isNew.value,
                            visible.value,
                            rememberFormRevealProgress(isNew.value, visible.value),
                        ).background(Color.Black),
                )
            }
        }

        fun frame() = compose.onNodeWithTag("card").captureToImage().asAndroidBitmap()

        fun advance(millis: Long) {
            compose.mainClock.advanceTimeBy(millis)
            compose.waitForIdle()
        }

        fun interior(bitmap: Bitmap) = Bitmap.createBitmap(bitmap, 30, 30, bitmap.width - 60, bitmap.height - 60)
        val plain = frame()
        compose.runOnIdle { visible.value = true }
        advance(160)
        assertTrue("The lock hold must precede particles and glow", plain.sameAs(frame()))
        advance(650)
        val burst = frame()
        advance(1200)
        val glow = frame()
        assertFalse("Burst pixels must appear inside the card", interior(plain).sameAs(interior(burst)))
        assertTrue("Burst pixels must animate away", interior(plain).sameAs(interior(glow)))
        advance(1000)
        assertFalse("The border should slowly breathe", glow.sameAs(frame()))
        assertFalse("An unselected discovery keeps a border glow", plain.sameAs(glow))
        compose.runOnIdle { visible.value = false }
        advance(32)
        assertTrue(plain.sameAs(frame()))
        compose.runOnIdle { visible.value = true }
        advance(160)
        assertTrue("Scrolling back must not replay the burst", interior(glow).sameAs(interior(frame())))
        compose.runOnIdle { isNew.value = false }
        advance(32)
        assertTrue("Selection removes the glow immediately", plain.sameAs(frame()))
    }

    @Test
    fun lockIsHeldBeforeCreatureRevealAndDoesNotReplayOnReturn() {
        val visible = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SquishTheme {
                FormDiscoveryThumbnail(
                    CreatureForm.STAR,
                    unlocked = true,
                    revealProgress = rememberFormRevealProgress(true, visible.value),
                    modifier = Modifier.size(72.dp).testTag("thumbnail"),
                )
            }
        }

        fun frame() = compose.onNodeWithTag("thumbnail").captureToImage().asAndroidBitmap()

        fun advance(millis: Long) {
            compose.mainClock.advanceTimeBy(millis)
            compose.waitForIdle()
        }
        val lock = frame()
        compose.runOnIdle { visible.value = true }
        advance(300)
        assertTrue("Give the lock time to register", lock.sameAs(frame()))
        advance(600)
        val transition = frame()
        assertFalse(lock.sameAs(transition))
        advance(1000)
        val creature = frame()
        assertFalse(lock.sameAs(creature))
        assertFalse("Creature should continue resolving after the lock fades", transition.sameAs(creature))
        compose.runOnIdle { visible.value = false }
        advance(32)
        compose.runOnIdle { visible.value = true }
        advance(300)
        assertTrue("A completed reveal must not replay on scroll return", creature.sameAs(frame()))
    }
}
