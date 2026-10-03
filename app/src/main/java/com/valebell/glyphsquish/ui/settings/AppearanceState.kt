package com.valebell.glyphsquish.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal data class AppearanceState(
    val settings: AppearanceSettings?,
    val hasError: Boolean,
    val actions: AppearanceActions,
)

internal data class AppearanceActions(
    val onDeviceColorsChange: (Boolean) -> Unit,
    val onRetry: () -> Unit,
)

@Composable
internal fun rememberAppearanceState(): AppearanceState {
    val context = LocalContext.current
    val preferences = remember(context) { AppearancePreferences(context) }
    val state = rememberSettingsState(preferences.settings)
    return AppearanceState(
        settings = state.settings,
        hasError = state.hasError,
        actions =
            AppearanceActions(
                onDeviceColorsChange = { enabled -> state.save { preferences.setUseDeviceColors(enabled) } },
                onRetry = state.onRetry,
            ),
    )
}
