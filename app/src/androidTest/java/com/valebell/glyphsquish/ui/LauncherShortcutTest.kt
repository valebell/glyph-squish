package com.valebell.glyphsquish.ui

import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.nothing.ketchum.Common
import com.valebell.glyphsquish.MainActivity
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeState
import com.valebell.glyphsquish.creatureRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LauncherShortcutTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun controlAnimationClock() {
        compose.mainClock.autoAdvance = false
    }

    @Test
    fun manifestShortcutsOpenBothGalleriesFromClosedActivity() {
        val shortcuts = context.getSystemService(ShortcutManager::class.java).manifestShortcuts
        assertEquals(setOf("forms", "moves"), shortcuts.map { it.id }.toSet())
        for (shortcut in shortcuts) {
            val label = checkNotNull(shortcut.shortLabel).toString()
            assertEquals(if (shortcut.id == "forms") "Forms" else "Moves", label)
            ActivityScenario.launch<MainActivity>(checkNotNull(shortcut.intent)).use {
                gallery(shortcut.id)
            }
        }
    }

    @Test
    fun warmIntentsReturnFromSettingsAndPreserveRuntimeAndSelection() {
        assumeTrue("Companion navigation requires Phone (3)", Common.is23112())
        ActivityScenario.launch<MainActivity>(intent("forms")).use { scenario ->
            gallery("forms")
            compose.waitUntil(5_000) { context.creatureRuntime().state.value is CreatureRuntimeState.Ready }
            val runtime = context.creatureRuntime()
            val before = (runtime.state.value as CreatureRuntimeState.Ready).creature
            var original: MainActivity? = null
            scenario.onActivity { original = it }
            // ActivityScenario matches lifecycle events against its original launch action.
            for (id in listOf("moves", "forms", "moves", "forms")) {
                compose.onNodeWithContentDescription("Settings").performClick()
                compose.waitForIdle()
                context.startActivity(
                    intent(id).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    ),
                )
                gallery(id)
                scenario.onActivity { assertSame(original, it) }
                assertSame(runtime, context.creatureRuntime())
                assertEquals(before, (runtime.state.value as CreatureRuntimeState.Ready).creature)
            }
            // A consumed shortcut must not override subsequent navigation on recreation.
            compose.onNodeWithContentDescription("Show moves").performClick()
            gallery("moves")
            scenario.recreate()
            gallery("moves")
        }
    }

    @Test
    fun launcherFlagsReplaceWarmActivityWithoutChangingCreature() {
        val shortcuts = context.getSystemService(ShortcutManager::class.java).manifestShortcuts
        ActivityScenario.launch<MainActivity>(intent("forms")).use {
            gallery("forms")
            compose.waitUntil(5_000) { context.creatureRuntime().state.value is CreatureRuntimeState.Ready }
            val before = (context.creatureRuntime().state.value as CreatureRuntimeState.Ready).creature
            for (id in listOf("moves", "forms")) {
                val shortcutIntent = checkNotNull(shortcuts.single { it.id == id }.intent)
                ActivityScenario.launch<MainActivity>(shortcutIntent).use {
                    gallery(id)
                    assertEquals(before, (context.creatureRuntime().state.value as CreatureRuntimeState.Ready).creature)
                }
            }
        }
    }

    private fun intent(id: String) =
        Intent(context, MainActivity::class.java).setAction("com.valebell.glyphsquish.OPEN_${id.uppercase()}")

    private fun gallery(id: String) {
        if (!Common.is23112()) {
            compose.onNodeWithText("Hey! I’m Squish.").assertIsDisplayed()
            compose.onNodeWithContentDescription("Show $id").assertDoesNotExist()
            return
        }
        compose.waitUntil(5_000) {
            compose.mainClock.advanceTimeBy(400)
            // A warm intent can briefly detach the root after the clock advances.
            compose
                .onAllNodesWithContentDescription("Show $id")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .singleOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.Selected) == true
        }
        compose.onNodeWithContentDescription("Show $id").assertIsSelected()
        compose.waitForIdle()
    }
}
