package com.valebell.glyphsquish.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.IOException

internal data class SettingsState<T>(
    val settings: T?,
    val hasError: Boolean,
    val save: (suspend () -> Unit) -> Unit,
    val onRetry: () -> Unit,
)

@Composable
internal fun <T> rememberSettingsState(source: Flow<T>): SettingsState<T> {
    val controller = remember(source) { SettingsController(source) }
    val scope = rememberCoroutineScope()
    val settings by controller.settingsFlow.collectAsStateWithLifecycle(initialValue = null)
    return SettingsState(
        settings = settings,
        hasError = controller.hasError,
        save = { write -> scope.launch { controller.save(write) } },
        onRetry = { scope.launch { controller.retry() } },
    )
}

internal class SettingsController<T>(
    private val source: Flow<T>,
) {
    var hasError by mutableStateOf(false)
        private set
    var settingsFlow by mutableStateOf(observeSettings())
        private set
    private var failedWrite: (suspend () -> Unit)? = null

    suspend fun save(write: suspend () -> Unit) {
        try {
            write()
            failedWrite = null
            hasError = false
        } catch (_: IOException) {
            failedWrite = write
            hasError = true
        }
    }

    suspend fun retry() {
        val write = failedWrite
        if (write != null) {
            save(write)
        } else {
            hasError = false
            settingsFlow = observeSettings()
        }
    }

    // Sonar's single-file analysis confuses Flow.catch with the deprecated SharedFlow overload.
    @Suppress("kotlin:S1874")
    private fun observeSettings(): Flow<T> =
        source.catch { error ->
            if (error !is IOException) throw error
            hasError = true
        }
}
