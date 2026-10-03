package com.valebell.glyphsquish.ui

import android.app.LocaleConfig
import android.app.LocaleManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale
import android.os.LocaleList as AndroidLocaleList

class LocalizationTest : SquishUiTestFixture() {
    // Apply the platform locale before the Compose activity starts, including its dialog windows.
    @get:Rule(order = 0)
    val appLocale = NativeAppLocaleRule()

    @Test
    fun packagedLocalesResolveTranslatedStringsAndPluralCounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locales = requireNotNull(LocaleConfig(context).supportedLocales)
        assertEquals(11, locales.size())
        val english = resourcesFor("en")
        repeat(locales.size()) { index ->
            val locale = locales[index]
            val resources = resourcesFor(locale.toLanguageTag())
            assertEquals(english.getString(R.string.app_name), resources.getString(R.string.app_name))
            assertEquals(english.getString(R.string.toy_name), resources.getString(R.string.toy_name))
            if (locale.language != "en") {
                assertNotEquals(
                    english.getString(R.string.settings_title),
                    resources.getString(R.string.settings_title),
                )
                assertNotEquals(english.getString(R.string.load_error), resources.getString(R.string.load_error))
            }
            for (count in listOf(0, 1, 2, 3, 11, 100)) {
                for (id in listOf(R.plurals.form_gallery_discovered_format, R.plurals.move_gallery_unlocked_format)) {
                    val formatted = resources.getQuantityString(id, count, count, 100)
                    assertFalse("Unformatted count for $locale", formatted.contains('%'))
                    assertTrue(formatted.isNotBlank())
                }
            }
        }
    }

    @Test
    fun arabicMirrorsNavigationAndDisplaysHelpWithoutClipping() {
        verifyRtlNavigation("ar")
    }

    @Test
    fun urduMirrorsNavigationAndDisplaysHelpWithoutClipping() {
        verifyRtlNavigation("ur")
    }

    @Test
    fun arabicLandscapeMirrorsPanesAndGalleryColumns() {
        val resources = resourcesFor("ar")
        setContent(
            DeviceConfigurationOverride.WindowSize(DpSize(800.dp, 400.dp)),
        )
        val preview =
            composeRule.onNodeWithContentDescription(
                resources.getString(R.string.matrix_preview_description, resources.getString(R.string.form_blob)),
            )
        val firstForm =
            composeRule.onNodeWithContentDescription(
                resources.getString(
                    R.string.form_gallery_card_description_unlocked_format,
                    resources.getString(R.string.form_blob),
                ),
            )
        preview.assertIsDisplayed()
        firstForm.assertIsDisplayed()
        val previewBounds = preview.fetchSemanticsNode().boundsInRoot
        val firstBounds = firstForm.fetchSemanticsNode().boundsInRoot
        assertTrue("RTL preview pane belongs to the right of the gallery", previewBounds.left > firstBounds.right)
        val lockedShark =
            resources.getString(
                R.string.form_gallery_card_description_locked_format,
                resources.getString(R.string.form_shark),
                resources.getString(
                    R.string.form_gallery_status_locked_format,
                    resources.getString(R.string.button_action_ricochet),
                ),
            )
        scrollGalleryTo("form-SHARK")
        val secondBounds = composeRule.onNodeWithContentDescription(lockedShark).fetchSemanticsNode().boundsInRoot
        assertTrue("Second RTL column belongs to the left of the first", secondBounds.right < firstBounds.left)
        captureReviewScreenshot("ar-landscape")
    }

    private fun verifyRtlNavigation(language: String) {
        val resources = resourcesFor(language)
        setContent(
            DeviceConfigurationOverride.WindowSize(DpSize(360.dp, 800.dp)) then
                DeviceConfigurationOverride.FontScale(1.3f),
        )
        val forms = composeRule.onNodeWithContentDescription(resources.getString(R.string.show_forms))
        val moves = composeRule.onNodeWithContentDescription(resources.getString(R.string.show_moves))
        val formsBounds = forms.fetchSemanticsNode().boundsInRoot
        val movesBounds = moves.fetchSemanticsNode().boundsInRoot
        assertTrue("Forms is the leading (right) RTL tab", formsBounds.left > movesBounds.left)
        assertTextFits(resources.getString(R.string.form_gallery_title))
        assertTextFits(resources.getString(R.string.move_gallery_title))
        captureReviewScreenshot("$language-gallery")
        composeRule.onNodeWithContentDescription(resources.getString(R.string.open_settings)).performClick()
        advanceUi()
        val back = composeRule.onNodeWithContentDescription(resources.getString(R.string.settings_back))
        val title = composeRule.onNodeWithText(resources.getString(R.string.settings_title))
        assertTrue(
            "RTL back action is right of the settings title",
            back.fetchSemanticsNode().boundsInRoot.left > title.fetchSemanticsNode().boundsInRoot.left,
        )
        captureReviewScreenshot("$language-settings")
        // Scroll semantics await an animated scroll, so let its frames advance.
        composeRule.mainClock.autoAdvance = true
        val help = composeRule.onNodeWithText(resources.getString(R.string.settings_how_to_play))
        help.performScrollTo()
        composeRule.mainClock.autoAdvance = false
        help.performClick()
        advanceUi()
        captureReviewScreenshot("$language-help", dialog = true)
        val confirmation = composeRule.onNodeWithText(resources.getString(R.string.help_close))
        confirmation.assertIsDisplayed()
        assertTextFits(resources.getString(R.string.controls_glyph_context))
        assertTextFits(resources.getString(R.string.controls_squish_description))
        confirmation.performClick()
        advanceUi()
        back.performClick()
        advanceUi()
        forms.assertIsDisplayed()
    }

    private fun assertTextFits(text: String) {
        val nodes = composeRule.onAllNodesWithText(text, useUnmergedTree = true)
        val count = nodes.fetchSemanticsNodes().size
        assertTrue("Expected text: $text", count > 0)
        repeat(count) { index ->
            val layouts = mutableListOf<TextLayoutResult>()
            nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse("Text must not be ellipsized: $text", layout.multiParagraph.didExceedMaxLines)
            repeat(layout.lineCount) { line ->
                assertTrue(
                    "Text must fit: $text",
                    layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1f,
                )
            }
            assertTrue("Text must fit vertically: $text", layout.multiParagraph.height <= layout.size.height + 1f)
        }
    }

    private fun resourcesFor(language: String): Resources {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration)
        config.setLocales(AndroidLocaleList(Locale.forLanguageTag(language)))
        return context.createConfigurationContext(config).resources
    }
}

class NativeAppLocaleRule : TestWatcher() {
    private var previousLocales: AndroidLocaleList? = null

    override fun starting(description: Description) {
        val language =
            when {
                description.methodName.startsWith("arabic") -> "ar"
                description.methodName.startsWith("urdu") -> "ur"
                else -> return
            }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val manager = requireNotNull(instrumentation.targetContext.getSystemService(LocaleManager::class.java))
        previousLocales = manager.applicationLocales
        manager.applicationLocales = AndroidLocaleList.forLanguageTags(language)
        instrumentation.waitForIdleSync()
    }

    override fun finished(description: Description) {
        previousLocales?.let { locales ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val manager = requireNotNull(instrumentation.targetContext.getSystemService(LocaleManager::class.java))
            manager.applicationLocales = locales
            instrumentation.waitForIdleSync()
        }
    }
}
