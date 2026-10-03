package com.valebell.glyphsquish

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nothing.ketchum.Common
import com.valebell.glyphsquish.ui.SquishApp
import com.valebell.glyphsquish.ui.SquishViewModel
import com.valebell.glyphsquish.ui.compatibility.DeviceCompatibilityGate
import com.valebell.glyphsquish.ui.home.SquishSection
import com.valebell.glyphsquish.ui.settings.rememberAppearanceState
import com.valebell.glyphsquish.ui.theme.SquishTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SquishViewModel by
        viewModels {
            SquishViewModel.factory(creatureRuntime())
        }

    private var shortcutSection by mutableStateOf<SquishSection?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutSection = sectionForShortcut(intent.action)
    }

    override fun onStart() {
        // Keep the launch alias enabled while its activity is visible.
        launcherIconSync().companionStarted(this)
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        launcherIconSync().companionStopped(this, isChangingConfigurations)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) shortcutSection = sectionForShortcut(intent.action)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        setContent {
            val appearance = rememberAppearanceState()
            SquishTheme(useDeviceColors = appearance.settings?.useDeviceColors == true) {
                DeviceCompatibilityGate(
                    isSupported = Common.is23112(),
                    onClose = ::finish,
                ) {
                    SquishApp(
                        viewModel = viewModel,
                        appearance = appearance,
                        shortcutSection = shortcutSection,
                        onShortcutHandled = { shortcutSection = null },
                        launchGlyphToys = ::launchGlyphToys,
                    )
                }
            }
        }
    }

    private fun launchGlyphToys(): Boolean {
        val intent =
            Intent().setComponent(
                ComponentName(GLYPH_TOYS_PACKAGE, GLYPH_TOYS_MANAGER_ACTIVITY),
            )
        if (intent.resolveActivity(packageManager) == null) return false
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private companion object {
        const val GLYPH_TOYS_PACKAGE = "com.nothing.thirdparty"
        const val GLYPH_TOYS_MANAGER_ACTIVITY =
            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
    }
}

internal fun sectionForShortcut(action: String?): SquishSection? =
    when (action) {
        "com.valebell.glyphsquish.OPEN_FORMS" -> SquishSection.FORMS
        "com.valebell.glyphsquish.OPEN_MOVES" -> SquishSection.MOVES
        else -> null
    }
