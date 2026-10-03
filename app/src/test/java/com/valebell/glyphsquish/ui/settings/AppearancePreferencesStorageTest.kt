package com.valebell.glyphsquish.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppearancePreferencesStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val jobs = mutableListOf<Job>()

    @After
    fun closeStores() =
        runBlocking {
            jobs.forEach { it.cancelAndJoin() }
        }

    @Test
    fun newInstallsKeepSquishColors() =
        runBlocking {
            assertFalse(openStore().settings.first().useDeviceColors)
        }

    @Test
    fun deviceColorsSurviveClosingAndReopeningTheStore() =
        runBlocking {
            val preferences = openStore()
            preferences.setUseDeviceColors(true)
            jobs.single().cancelAndJoin()

            assertTrue(openStore().settings.first().useDeviceColors)
        }

    @Test
    fun switchingBackToSquishColorsSurvivesReopeningTheStore() =
        runBlocking {
            val preferences = openStore()
            preferences.setUseDeviceColors(true)
            preferences.setUseDeviceColors(false)
            jobs.single().cancelAndJoin()

            assertFalse(openStore().settings.first().useDeviceColors)
        }

    private fun openStore(dispatcher: CoroutineDispatcher = Dispatchers.IO): AppearancePreferences {
        val job = SupervisorJob().also(jobs::add)
        return AppearancePreferences(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + dispatcher),
                produceFile = { File(temporaryFolder.root, "appearance.preferences_pb") },
            ),
        )
    }
}
