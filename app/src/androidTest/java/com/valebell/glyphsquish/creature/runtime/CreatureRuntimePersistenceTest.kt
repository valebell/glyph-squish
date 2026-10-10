package com.valebell.glyphsquish.creature.runtime

import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.domain.CreatureRepository
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreatureRuntimePersistenceTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After
    fun close() =
        runBlocking {
            scope.coroutineContext.job.cancelAndJoin()
        }

    @Test
    fun suspendedSavePreservesRevisionOrderAndImmediateUiUpdates() =
        onMain {
            val repository = SuspendedRepository()
            val runtime = CreatureRuntime(repository, scope)
            runtime.selectForm(CreatureForm.BUTTERFLY)
            runtime.selectForm(CreatureForm.BLOB)

            // The first write is suspended, but both selections are already visible.
            assertEquals(listOf(3L), repository.attemptedRevisions)
            assertEquals(4L, (runtime.state.value as CreatureRuntimeState.Ready).creature.revision)
            repository.resumeSave.complete(Unit)
            scope.coroutineContext.job.children
                .toList()
                .joinAll()

            assertEquals(listOf(3L, 4L), repository.attemptedRevisions)
            assertEquals((runtime.state.value as CreatureRuntimeState.Ready).creature, repository.stored)
        }

    @Test
    fun failedSuspendedSaveDoesNotBlockNewerStateOrRetry() =
        onMain {
            val repository = SuspendedRepository(failFirstSave = true)
            val runtime = CreatureRuntime(repository, scope)
            runtime.selectForm(CreatureForm.BUTTERFLY)
            runtime.selectForm(CreatureForm.BLOB)
            repository.resumeSave.complete(Unit)
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
            runtime.retrySave()
            scope.coroutineContext.job.children
                .toList()
                .joinAll()

            assertEquals(listOf(3L, 4L, 4L), repository.attemptedRevisions)
            assertEquals((runtime.state.value as CreatureRuntimeState.Ready).creature, repository.stored)
            assertEquals(null, runtime.pendingSaveFailure.value)
        }

    @Test
    fun saveFailureIsRetainedUntilAPersistSucceeds() =
        onMain {
            val repository = SuspendedRepository(failAllSaves = true)
            val runtime = CreatureRuntime(repository, scope)
            runtime.selectForm(CreatureForm.BUTTERFLY)
            repository.resumeSave.complete(Unit)
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
            assertEquals(1L, runtime.pendingSaveFailure.value)

            runtime.selectForm(CreatureForm.BLOB)
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
            assertEquals(2L, runtime.pendingSaveFailure.value)

            repository.failAllSaves = false
            runtime.retrySave()
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
            assertEquals(null, runtime.pendingSaveFailure.value)
            assertEquals((runtime.state.value as CreatureRuntimeState.Ready).creature, repository.stored)
        }

    @Test
    fun failedLoadCanBeRetriedWithoutDuplicateConcurrentLoads() =
        onMain {
            val resumeLoad = CompletableDeferred<Unit>()
            var attempts = 0
            val repository =
                object : CreatureRepository {
                    override suspend fun loadOrCreate(): CreatureState {
                        attempts++
                        resumeLoad.await()
                        if (attempts == 1) throw SQLiteException("Test load failure")
                        return CreatureProgression.initialState()
                    }

                    override suspend fun save(state: CreatureState) = Unit
                }
            val runtime = CreatureRuntime(repository, scope)
            runtime.retryLoad()
            assertEquals(1, attempts)
            resumeLoad.complete(Unit)
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
            assertTrue(runtime.state.value is CreatureRuntimeState.LoadFailed)

            runtime.retryLoad()
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
            assertEquals(2, attempts)
            assertEquals(CreatureRuntimeState.Ready(CreatureProgression.initialState()), runtime.state.value)
        }

    @Test
    fun cancellingScopeCancelsSuspendedAndQueuedSaves() =
        onMain {
            val repository = SuspendedRepository()
            val runtime = CreatureRuntime(repository, scope)
            runtime.selectForm(CreatureForm.BUTTERFLY)
            runtime.selectForm(CreatureForm.BLOB)
            scope.coroutineContext.job.cancelAndJoin()
            repository.resumeSave.complete(Unit)

            assertEquals(listOf(3L), repository.attemptedRevisions)
            assertEquals(2L, repository.stored.revision)
        }

    private fun onMain(block: suspend () -> Unit) =
        runBlocking {
            withTimeout(10_000) {
                withContext(Dispatchers.Main) { block() }
            }
        }

    private class SuspendedRepository(
        private val failFirstSave: Boolean = false,
        var failAllSaves: Boolean = false,
    ) : CreatureRepository {
        var stored = CreatureProgression.unlockMove(CreatureProgression.initialState(), SlimeButtonAction.HELIX)
        val attemptedRevisions = mutableListOf<Long>()
        val resumeSave = CompletableDeferred<Unit>()

        override suspend fun loadOrCreate(): CreatureState = stored

        override suspend fun save(state: CreatureState) {
            attemptedRevisions += state.revision
            if (attemptedRevisions.size == 1) {
                resumeSave.await()
                if (failFirstSave) throw SQLiteException("Test save failure")
            }
            if (failAllSaves) throw SQLiteException("Test save failure")
            stored = state
        }
    }
}
