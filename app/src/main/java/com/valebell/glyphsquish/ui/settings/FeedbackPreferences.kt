package com.valebell.glyphsquish.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// One store per file for the whole process, including overlapping activities.
private val Context.feedbackDataStore by preferencesDataStore(name = "feedback_preferences")

internal data class FeedbackSettings(
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
)

internal class FeedbackPreferences(
    private val store: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.feedbackDataStore)

    val settings: Flow<FeedbackSettings> =
        store.data
            .map { preferences ->
                FeedbackSettings(
                    isSoundEnabled = preferences[SOUND_ENABLED] ?: true,
                    isVibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
                )
            }.distinctUntilChanged()

    suspend fun setSoundEnabled(enabled: Boolean) {
        store.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        store.edit { it[VIBRATION_ENABLED] = enabled }
    }

    private companion object {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }
}
