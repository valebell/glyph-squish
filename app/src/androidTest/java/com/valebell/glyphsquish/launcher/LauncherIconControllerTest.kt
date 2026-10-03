package com.valebell.glyphsquish.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.MainActivity
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeState
import com.valebell.glyphsquish.creatureRuntime
import com.valebell.glyphsquish.launcherIconSync
import com.valebell.glyphsquish.slime.SlimeToyService
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class LauncherIconControllerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val packageManager = context.packageManager

    @Test
    fun pendingIconChangesDoNotFinishRealLauncherActivity() {
        val savedForm =
            runBlocking {
                withTimeout(5.seconds) {
                    context
                        .creatureRuntime()
                        .state
                        .filterIsInstance<CreatureRuntimeState.Ready>()
                        .first()
                        .creature.currentForm
                }
            }
        val launchIntent = checkNotNull(packageManager.getLaunchIntentForPackage(context.packageName))
        val finalForm = CreatureForm.entries.first { it != savedForm }
        try {
            // Launch through the actual alias, as the home screen does, not MainActivity directly.
            ActivityScenario.launch<MainActivity>(launchIntent).use { scenario ->
                var original: MainActivity? = null
                scenario.onActivity { original = it }
                for (form in CreatureForm.entries + finalForm) {
                    scenario.onActivity { it.launcherIconSync().request(form) }
                    instrumentation.waitForIdleSync()
                    assertEquals(
                        launchIntent.component,
                        packageManager.getLaunchIntentForPackage(context.packageName)?.component,
                    )
                    assertEquals(Lifecycle.State.RESUMED, scenario.state)
                    scenario.onActivity {
                        assertSame(original, it)
                        assertFalse(it.isFinishing)
                    }
                }
                scenario.recreate()
                assertEquals(
                    launchIntent.component,
                    packageManager.getLaunchIntentForPackage(context.packageName)?.component,
                )
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
            }
            // The latest pending form is applied only after the activity has left the screen.
            assertEquals(
                LauncherIconController(context).component(finalForm),
                packageManager.getLaunchIntentForPackage(context.packageName)?.component,
            )
        } finally {
            instrumentation.runOnMainSync { context.launcherIconSync().request(savedForm) }
        }
    }

    @Test
    fun everyFormKeepsExactlyOneLauncherAndPreservesEntryPoints() {
        val original =
            runBlocking {
                withTimeout(5.seconds) {
                    context
                        .creatureRuntime()
                        .state
                        .filterIsInstance<CreatureRuntimeState.Ready>()
                        .first()
                        .creature
                }
            }
        val controller = LauncherIconController(context)
        val main = ComponentName(context, MainActivity::class.java)
        val toy = ComponentName(context, SlimeToyService::class.java)
        val toyBefore = packageManager.getServiceInfo(toy, PackageManager.GET_META_DATA)
        try {
            for (form in CreatureForm.entries + CreatureForm.BLOB) {
                instrumentation.runOnMainSync { controller.select(form) }
                val launchers =
                    packageManager.queryIntentActivities(
                        Intent(
                            Intent.ACTION_MAIN,
                        ).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName),
                        PackageManager.GET_META_DATA,
                    )
                assertEquals(listOf(controller.component(form).className), launchers.map { it.activityInfo.name })
                val activity = launchers.single().activityInfo
                assertEquals(main.className, activity.targetActivity)
                assertEquals(R.xml.shortcuts, activity.metaData.getInt("android.app.shortcuts"))
                assertTrue(packageManager.getActivityInfo(main, 0).enabled)
                val service = packageManager.getServiceInfo(toy, PackageManager.GET_META_DATA)
                assertTrue(service.enabled)
                assertEquals(
                    toyBefore.metaData.getInt("com.nothing.glyph.toy.image"),
                    service.metaData.getInt("com.nothing.glyph.toy.image"),
                )
                assertEquals(original, (context.creatureRuntime().state.value as CreatureRuntimeState.Ready).creature)
                // A fresh controller must recognize persisted component settings and stay idempotent.
                instrumentation.runOnMainSync { LauncherIconController(context).select(form) }
                assertEquals(
                    controller.component(form),
                    packageManager.getLaunchIntentForPackage(context.packageName)?.component,
                )
            }
        } finally {
            instrumentation.runOnMainSync { controller.select(original.currentForm) }
        }
    }

    @Test
    fun toyPreviewUsesPhoneThreeCanvasAndOpaqueWhitePixels() {
        val drawable = checkNotNull(context.getDrawable(R.drawable.toy_preview))
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 512, 512)
        drawable.draw(Canvas(bitmap))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(0, 0))
        assertEquals(Color.BLACK, bitmap.getPixel(256, 20))
        assertEquals(Color.rgb(28, 28, 28), bitmap.getPixel(256, 42))
        assertEquals(Color.WHITE, bitmap.getPixel(256, 256))
        val pixels = IntArray(512 * 512)
        bitmap.getPixels(pixels, 0, 512, 0, 0, 512, 512)
        val lit = pixels.count { it == Color.WHITE }
        assertTrue("The Blob must occupy a readable area of the matrix", lit > 10_000)
    }
}
