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
private val Context.appearanceDataStore by preferencesDataStore(name = "appearance_preferences")

internal data class AppearanceSettings(
    val useDeviceColors: Boolean = false,
)

internal class AppearancePreferences(
    private val store: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.appearanceDataStore)

    val settings: Flow<AppearanceSettings> =
        store.data
            .map { preferences ->
                AppearanceSettings(useDeviceColors = preferences[USE_DEVICE_COLORS] ?: false)
            }.distinctUntilChanged()

    suspend fun setUseDeviceColors(enabled: Boolean) {
        store.edit { it[USE_DEVICE_COLORS] = enabled }
    }

    private companion object {
        val USE_DEVICE_COLORS = booleanPreferencesKey("use_device_colors")
    }
}
