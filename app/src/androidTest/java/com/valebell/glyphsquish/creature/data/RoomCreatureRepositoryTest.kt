package com.valebell.glyphsquish.creature.data

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCreatureRepositoryTest {
    private lateinit var database: SquishDatabase
    private lateinit var repository: RoomCreatureRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room
                .inMemoryDatabaseBuilder(context, SquishDatabase::class.java)
                .build()
        repository = RoomCreatureRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seenFormsSurviveReopen() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "seen-forms-test.db"
            context.deleteDatabase(name)

            fun open() =
                Room
                    .databaseBuilder(context, SquishDatabase::class.java, name)
                    .build()
            try {
                open().useDatabase { db ->
                    val repo = RoomCreatureRepository(db)
                    val initial = repo.loadOrCreate()
                    assertEquals(setOf(CreatureForm.BLOB), initial.seenForms)
                    val butterflyUnlocked = CreatureProgression.unlockMove(initial, SlimeButtonAction.HELIX)
                    val seen = CreatureProgression.markFormsSeen(butterflyUnlocked, setOf(CreatureForm.BUTTERFLY))
                    val unlocked = CreatureProgression.unlockMove(seen, SlimeButtonAction.THUNDERBOLT)
                    repo.save(unlocked)
                }
                open().useDatabase { db ->
                    val repo = RoomCreatureRepository(db)
                    val fresh = repo.loadOrCreate()
                    assertEquals(setOf(CreatureForm.BLOB, CreatureForm.BUTTERFLY), fresh.seenForms)
                    repo.save(CreatureProgression.markFormsSeen(fresh, setOf(CreatureForm.STAR)))
                }
                open().useDatabase { db ->
                    val restored = RoomCreatureRepository(db).loadOrCreate()
                    assertEquals(
                        setOf(CreatureForm.BLOB, CreatureForm.BUTTERFLY, CreatureForm.STAR),
                        restored.seenForms,
                    )
                    assertEquals(CreatureForm.BLOB, restored.currentForm)
                    assertEquals(SlimeButtonAction.THUNDERBOLT, restored.selectedMove)
                }
            } finally {
                context.deleteDatabase(name)
            }
        }

    @Test
    fun unlockSelectionAndCurrentFormPersistTogether() =
        runBlocking {
            var state = withContext(Dispatchers.Main) { repository.loadOrCreate() }
            state = CreatureProgression.unlockMove(state, SlimeButtonAction.HELIX)
            state = CreatureProgression.selectForm(state, CreatureForm.BUTTERFLY)
            withContext(Dispatchers.Main) { repository.save(state) }

            val restored = repository.loadOrCreate()

            assertEquals(CreatureForm.BUTTERFLY, restored.currentForm)
            assertEquals(SlimeButtonAction.HELIX, restored.selectedMove)
            assertEquals(
                setOf(SlimeButtonAction.BURST, SlimeButtonAction.HELIX),
                restored.unlockedMoves,
            )
        }

    @Test
    fun legacyCloverFormLoadsAsButterflyAndSavesWithItsCurrentForm() =
        runBlocking {
            val dao = database.creatureDao()
            dao.saveCharacter(
                CreatureEntity(
                    currentForm = "CLOVER",
                    selectedMove = SlimeButtonAction.HELIX.name,
                    revision = 7L,
                ),
            )
            dao.saveUnlockedMoves(
                listOf(
                    UnlockedMoveEntity(SlimeButtonAction.BURST.name),
                    UnlockedMoveEntity(SlimeButtonAction.HELIX.name),
                ),
            )

            val restored = repository.loadOrCreate()

            assertEquals(CreatureForm.BUTTERFLY, restored.currentForm)
            repository.save(restored)
            assertEquals(CreatureForm.BUTTERFLY.name, dao.loadCharacter()?.currentForm)
        }

    @Test
    fun unchangedMembershipSavesWithoutUnlockTableMutationsRegardlessOfOrder() =
        runBlocking {
            val unlocked = CreatureProgression.unlockMove(repository.loadOrCreate(), SlimeButtonAction.HELIX)
            repository.save(unlocked)
            rejectUnlockTableMutations()

            val selected = CreatureProgression.selectMove(unlocked, SlimeButtonAction.BURST)
            val reordered = selected.copy(unlockedMoves = linkedSetOf(SlimeButtonAction.HELIX, SlimeButtonAction.BURST))
            repository.save(reordered)

            assertEquals(selected, repository.loadOrCreate())
        }

    @Test
    fun replacingMembershipRemovesMovesThatAreNoLongerPresent() =
        runBlocking {
            val initial = repository.loadOrCreate()
            val unlocked = CreatureProgression.unlockMove(initial, SlimeButtonAction.HELIX)
            repository.save(unlocked)
            val replacement = initial.copy(revision = unlocked.revision + 1)

            repository.save(replacement)

            assertEquals(replacement, repository.loadOrCreate())
        }

    @Test
    fun staleRevisionsCannotOverwriteCharacterOrUnlocks() {
        val initial = runBlocking { repository.loadOrCreate() }
        val unlocked = CreatureProgression.unlockMove(initial, SlimeButtonAction.HELIX)
        runBlocking { repository.save(unlocked) }

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.save(initial) }
        }

        assertEquals(unlocked, runBlocking { repository.loadOrCreate() })
    }

    @Test
    fun repositoryInstancesCompareCurrentStoredMembership() =
        runBlocking {
            val initial = repository.loadOrCreate()
            val otherRepository = RoomCreatureRepository(database)
            assertEquals(initial, otherRepository.loadOrCreate())
            val unlocked = CreatureProgression.unlockMove(initial, SlimeButtonAction.HELIX)
            repository.save(unlocked)
            rejectUnlockTableMutations()
            val selected = CreatureProgression.selectMove(unlocked, SlimeButtonAction.BURST)

            otherRepository.save(selected)

            assertEquals(selected, repository.loadOrCreate())
        }

    @Test
    fun failedMoveInsertRollsBackCharacterAndUnlockedMoves() =
        runBlocking {
            val initial = repository.loadOrCreate()
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_helix BEFORE INSERT ON unlocked_moves " +
                    "WHEN NEW.move = 'HELIX' BEGIN SELECT RAISE(ABORT, 'Test insert failure'); END",
            )
            val updated = CreatureProgression.unlockMove(initial, SlimeButtonAction.HELIX)
            var failed = false
            try {
                repository.save(updated)
            } catch (_: SQLiteException) {
                failed = true
            }

            assertTrue(failed)
            assertEquals(initial, repository.loadOrCreate())
        }

    private fun rejectUnlockTableMutations() {
        val writableDatabase = database.openHelper.writableDatabase
        listOf("INSERT", "UPDATE", "DELETE").forEach { operation ->
            writableDatabase.execSQL(
                "CREATE TRIGGER reject_unlock_$operation BEFORE $operation ON unlocked_moves " +
                    "BEGIN SELECT RAISE(ABORT, 'Unexpected unlock table mutation'); END",
            )
        }
    }
}

private suspend fun SquishDatabase.useDatabase(block: suspend (SquishDatabase) -> Unit) {
    try {
        block(this)
    } finally {
        close()
    }
}
