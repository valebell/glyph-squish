package com.valebell.glyphsquish.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToKey
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.After
import org.junit.Rule

abstract class SquishUiTestFixture {
    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val viewModelStores = mutableListOf<ViewModelStore>()

    @After
    fun clearViewModels() {
        viewModelStores.forEach(ViewModelStore::clear)
    }

    protected fun galleryScrollOffset(): Float =
        composeRule
            .onNode(hasScrollToIndexAction())
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()

    protected fun setContent(
        configurationOverride: DeviceConfigurationOverride? = null,
        launchGlyphToys: () -> Boolean = { true },
        initialRuntime: FakeCreatureRuntime = FakeCreatureRuntime(),
    ): FakeCreatureRuntime {
        composeRule.mainClock.autoAdvance = false
        val runtime = initialRuntime
        val viewModelStore = ViewModelStore().also(viewModelStores::add)
        val viewModel =
            ViewModelProvider(
                viewModelStore,
                SquishViewModel.factory(runtime),
            )[SquishViewModel::class.java]
        composeRule.setContent {
            val app: @Composable () -> Unit = {
                SquishTheme {
                    SquishApp(
                        viewModel = viewModel,
                        launchGlyphToys = launchGlyphToys,
                    )
                }
            }
            if (configurationOverride == null) {
                app()
            } else {
                DeviceConfigurationOverride(configurationOverride, app)
            }
        }
        advanceUi()
        return runtime
    }

    protected fun scrollGalleryTo(key: String) {
        // Key-based scrolling also finds items that the lazy grid has not composed yet.
        composeRule.onNode(hasScrollToIndexAction()).performScrollToKey(key)
        advanceUi()
    }

    protected fun advanceUi(durationMillis: Long = 400L) {
        composeRule.mainClock.advanceTimeBy(durationMillis)
        composeRule.waitForIdle()
    }

    protected fun captureReviewScreenshot(
        name: String,
        dialog: Boolean = false,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val node = if (dialog) composeRule.onNode(isDialog()) else composeRule.onRoot()
        val bitmap = node.captureToImage().asAndroidBitmap()
        java.io
            .File(
                InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
                    ?: requireNotNull(context.getExternalFilesDir(null)).absolutePath,
                "ui-review-$name.png",
            ).outputStream()
            .use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
    }
}
