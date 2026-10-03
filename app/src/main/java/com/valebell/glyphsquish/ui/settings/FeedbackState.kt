package com.valebell.glyphsquish.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal data class FeedbackState(
    val settings: FeedbackSettings?,
    val hasError: Boolean,
    val actions: FeedbackActions,
)

@Composable
internal fun rememberFeedbackState(): FeedbackState {
    val context = LocalContext.current
    val preferences = remember(context) { FeedbackPreferences(context) }
    val state = rememberSettingsState(preferences.settings)
    return FeedbackState(
        settings = state.settings,
        hasError = state.hasError,
        actions =
            FeedbackActions(
                onSoundEnabledChange = { enabled -> state.save { preferences.setSoundEnabled(enabled) } },
                onVibrationEnabledChange = { enabled -> state.save { preferences.setVibrationEnabled(enabled) } },
                onRetry = state.onRetry,
            ),
    )
}
