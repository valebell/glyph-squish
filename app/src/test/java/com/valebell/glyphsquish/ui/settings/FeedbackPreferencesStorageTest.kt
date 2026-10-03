package com.valebell.glyphsquish.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FeedbackPreferencesStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val jobs = mutableListOf<Job>()

    @After
    fun closeStores() =
        runBlocking {
            jobs.forEach { it.cancelAndJoin() }
        }

    @Test
    fun emptyStoreEnablesBothFeedbackOptions() =
        runBlocking {
            assertEquals(FeedbackSettings(), openStore().settings.first())
        }

    @Test
    fun choicesSurviveClosingAndReopeningTheStore() =
        runBlocking {
            val preferences = openStore()
            preferences.setSoundEnabled(false)
            preferences.setVibrationEnabled(false)
            jobs.single().cancelAndJoin()

            assertEquals(FeedbackSettings(false, false), openStore().settings.first())
        }

    @Test
    fun concurrentEditsKeepBothChoicesAndNotifyCollectors() =
        runBlocking {
            val preferences = openStore()
            val observed = async { preferences.settings.first { !it.isSoundEnabled && !it.isVibrationEnabled } }
            val soundWrite = async { preferences.setSoundEnabled(false) }
            val vibrationWrite = async { preferences.setVibrationEnabled(false) }
            soundWrite.await()
            vibrationWrite.await()

            assertEquals(FeedbackSettings(false, false), withTimeout(5_000) { observed.await() })
        }

    private fun openStore(dispatcher: CoroutineDispatcher = Dispatchers.IO): FeedbackPreferences {
        val job = SupervisorJob().also(jobs::add)
        return FeedbackPreferences(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + dispatcher),
                produceFile = { File(temporaryFolder.root, "feedback.preferences_pb") },
            ),
        )
    }
}
