package com.valebell.glyphsquish.creature.runtime

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.valebell.glyphsquish.creature.data.RoomCreatureRepository
import com.valebell.glyphsquish.creature.data.SquishDatabase
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.domain.CreatureRepository
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.creature.domain.ShellSector
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.slime.SlimeEngine
import com.valebell.glyphsquish.ui.SquishViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class GlyphUnlockBoundaryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var database: SquishDatabase
    private lateinit var repository: CountingRepository
    private lateinit var runtime: CreatureRuntime
    private var now = 0L
    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Before
    fun setUp() {
        val context = instrumentation.targetContext
        context.deleteDatabase(DATABASE_NAME)
        openDatabase()
        main { runtime = newRuntime() }
    }

    @After
    fun close() {
        runtimeScope.cancel()
        database.close()
        instrumentation.targetContext.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun learnedRecipesStaySilentBeforeGalleryVisitAfterReloadAndAfterAcknowledgement() {
        for ((move, contacts) in recipes()) {
            assertEquals(listOf(move), feed(contacts, CreatureInputSource.GLYPH))
        }
        assertEquals(setOf(CreatureForm.BLOB), state().seenForms)
        assertLearnedRecipesStaySilent()

        val saved = state()
        database.close()
        openDatabase()
        main { runtime = newRuntime() }
        assertEquals(saved, state())
        assertLearnedRecipesStaySilent()

        main { runtime.markFormsSeen(CreatureForm.entries.toSet()) }
        assertEquals(CreatureForm.entries.toSet(), state().seenForms)
        assertLearnedRecipesStaySilent()
    }

    @Test
    fun learnedThunderboltDoesNotConsumeSelectedMoveAfterBounce() {
        feed(circle() + circle(), CreatureInputSource.GLYPH)
        feed(recipes().getValue(SlimeButtonAction.THUNDERBOLT), CreatureInputSource.GLYPH)
        main { runtime.selectMove(SlimeButtonAction.HELIX) }
        val saved = state()
        val writes = repository.saveAttempts
        repeat(2) {
            feed(listOf(contact(180.0), contact(0.0)), CreatureInputSource.GLYPH)
            main { assertEquals(SlimeButtonAction.HELIX, runtime.handleGlyphButtonPress()) }
            assertTrue(feed(circle(), CreatureInputSource.GLYPH).isEmpty())
        }
        assertEquals(saved, state())
        assertEquals(writes, repository.saveAttempts)
    }

    @Test
    fun longPressReplaysSelectionWhilePreservingLiveThunderboltPrefix() {
        feed(circle() + circle(), CreatureInputSource.GLYPH)
        val saved = state()
        main { assertEquals(SlimeButtonAction.HELIX, runtime.handleGlyphButtonPress()) }
        feed(listOf(contact(180.0), contact(0.0)), CreatureInputSource.GLYPH)
        main { assertEquals(SlimeButtonAction.HELIX, runtime.handleGlyphButtonPress()) }
        feed(circle(), CreatureInputSource.GLYPH)
        assertTrue(SlimeButtonAction.THUNDERBOLT in state().unlockedMoves)
        main { runtime.selectMove(saved.selectedMove) }
        val writes = repository.saveAttempts
        feed(listOf(contact(180.0), contact(0.0)), CreatureInputSource.GLYPH)
        main {
            now += 4_001
            assertEquals(SlimeButtonAction.HELIX, runtime.handleGlyphButtonPress())
        }
        assertEquals(writes, repository.saveAttempts)
    }

    @Test
    fun verticalBounceAndRepeatedButtonPlaybackStillUnlockThunderbolt() {
        feed(listOf(contact(90.0), contact(270.0)), CreatureInputSource.GLYPH)
        val initial = state()
        main {
            val engine =
                SlimeEngine(
                    interactionHandler = { runtime.handleInteraction(it, CreatureInputSource.GLYPH) },
                    onMovePlaybackChanged = runtime::setGlyphMovePlaybackActive,
                )
            repeat(2) {
                engine.handleGlyphButtonPress(runtime::handleGlyphButtonPress)
                assertEquals(SlimeButtonAction.BURST, engine.snapshot().activeButtonAction)
                // Slow rendering must not spend the user's four-second circle window.
                while (engine.isMovePlaying) {
                    now += 200
                    engine.step(1f / 30f)
                }
                assertEquals(initial, state())
            }
            now += 3_000
        }
        assertEquals(listOf(SlimeButtonAction.THUNDERBOLT), feed(circle(), CreatureInputSource.GLYPH))
        assertEquals(1, repository.saveAttempts)
    }

    @Test
    fun circleWindowStillExpiresAfterSelectedMovePlaybackEnds() {
        feed(listOf(contact(90.0), contact(270.0)), CreatureInputSource.GLYPH)
        main {
            assertEquals(SlimeButtonAction.BURST, runtime.handleGlyphButtonPress())
            runtime.setGlyphMovePlaybackActive(true)
            now += 10_000
            runtime.setGlyphMovePlaybackActive(false)
            now += 4_001
            assertEquals(SlimeButtonAction.BURST, runtime.handleGlyphButtonPress())
        }
        assertTrue(feed(circle(), CreatureInputSource.GLYPH).isEmpty())
        assertEquals(setOf(SlimeButtonAction.BURST), state().unlockedMoves)
    }

    @Test
    fun companionCannotPerformAlreadyLearnedRecipesOrWriteProgression() {
        recipes().values.forEach { feed(it, CreatureInputSource.GLYPH) }
        val saved = state()
        val writes = repository.saveAttempts
        main {
            recipes().values.flatten().forEach {
                assertNull(runtime.handleInteraction(it, CreatureInputSource.COMPANION))
                now += 100
            }
        }
        assertEquals(saved, state())
        assertEquals(writes, repository.saveAttempts)
    }

    @Test
    fun everyRecipeIsIgnoredInCompanionButLearnedOnGlyph() {
        val viewModel = SquishViewModel(runtime)
        for ((_, contacts) in recipes()) {
            main {
                contacts.forEach {
                    assertNull(viewModel.handlePreviewInteraction(it))
                    now += 100
                }
            }
        }
        assertEquals(CreatureProgression.initialState(), state())
        assertEquals(0, repository.saveAttempts)
        for ((move, contacts) in recipes()) {
            feed(contacts, CreatureInputSource.GLYPH)
            assertTrue(move in state().unlockedMoves)
            assertEquals(move, state().selectedMove)
        }
        assertEquals(4, repository.saveAttempts)
        assertEquals(state(), runBlocking { repository.loadOrCreate() })
    }

    @Test
    fun companionInputCannotStartCompleteOrInterruptAGlyphRecipe() {
        feed(circle(), CreatureInputSource.COMPANION)
        feed(circle(), CreatureInputSource.GLYPH)
        assertEquals(setOf(SlimeButtonAction.BURST), state().unlockedMoves)
        feed(listOf(contact(90.0), CreatureInteraction.GlyphButtonPressed), CreatureInputSource.COMPANION)
        main { runtime.cycleFormFromShake(CreatureInputSource.COMPANION) }
        feed(circle(), CreatureInputSource.GLYPH)
        assertTrue(SlimeButtonAction.HELIX in state().unlockedMoves)
        assertEquals(1, repository.saveAttempts)
    }

    @Test
    fun glyphInterruptionStillResetsTheRecipe() {
        feed(circle(), CreatureInputSource.GLYPH)
        main { runtime.resetMoveRecognition() }
        feed(circle(), CreatureInputSource.GLYPH)
        assertEquals(setOf(SlimeButtonAction.BURST), state().unlockedMoves)
        assertEquals(0, repository.saveAttempts)
    }

    @Test
    fun glyphShakeResetsEvenIfCompanionAlreadyDebouncedFormCycling() {
        feed(circle(), CreatureInputSource.GLYPH)
        main {
            runtime.cycleFormFromShake(CreatureInputSource.COMPANION)
            runtime.cycleFormFromShake(CreatureInputSource.GLYPH)
            now += 1_000
        }
        feed(circle(), CreatureInputSource.GLYPH)
        assertEquals(setOf(SlimeButtonAction.BURST), state().unlockedMoves)
        assertEquals(0, repository.saveAttempts)
    }

    @Test
    fun physicalGlyphButtonCompletesThunderboltAndUnlockSurvivesDatabaseReopen() {
        feed(listOf(contact(180.0), contact(0.0)), CreatureInputSource.GLYPH)
        main { runtime.handleGlyphButtonPress() }
        feed(circle(), CreatureInputSource.GLYPH)
        assertTrue(SlimeButtonAction.THUNDERBOLT in state().unlockedMoves)
        val saved = state()
        database.close()
        openDatabase()
        main { runtime = newRuntime() }
        assertEquals(saved, state())
    }

    @Test
    fun retrySaveWritesCurrentStateWithoutLearningAnotherMove() {
        repository.failSaves = true
        feed(circle() + circle(), CreatureInputSource.GLYPH)
        assertTrue(SlimeButtonAction.HELIX in state().unlockedMoves)
        assertEquals(setOf(SlimeButtonAction.BURST), runBlocking { repository.loadOrCreate() }.unlockedMoves)
        repository.failSaves = false
        main { runtime.retrySave() }
        assertEquals(state(), runBlocking { repository.loadOrCreate() })
        assertEquals(2, repository.saveAttempts)
    }

    private fun openDatabase() {
        database =
            Room
                .databaseBuilder(instrumentation.targetContext, SquishDatabase::class.java, DATABASE_NAME)
                .build()
        repository = CountingRepository(RoomCreatureRepository(database))
    }

    private fun newRuntime() = CreatureRuntime(repository, runtimeScope) { now }

    private fun state() = (runtime.state.value as CreatureRuntimeState.Ready).creature

    private fun feed(
        interactions: List<CreatureInteraction>,
        source: CreatureInputSource,
    ): List<SlimeButtonAction> {
        val actions = mutableListOf<SlimeButtonAction>()
        main {
            interactions.forEach {
                runtime.handleInteraction(it, source)?.let(actions::add)
                now += 100
            }
        }
        return actions
    }

    private fun assertLearnedRecipesStaySilent() {
        val saved = state()
        val writes = repository.saveAttempts
        main {
            val engine =
                SlimeEngine(interactionHandler = {
                    runtime.handleInteraction(it, CreatureInputSource.GLYPH)
                })
            recipes().values.flatten().forEach { interaction ->
                engine.simulateInteraction(interaction)
                now += 100
                assertFalse(engine.isMovePlaying)
            }
            engine.step(1f / 30f)
            assertNull(engine.snapshot().activeButtonAction)
        }
        assertEquals(saved, state())
        assertEquals(writes, repository.saveAttempts)
        assertEquals(saved, runBlocking { repository.loadOrCreate() })
    }

    private fun main(block: () -> Unit) {
        var result: Result<Unit>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        checkNotNull(result).getOrThrow()
        runBlocking {
            withTimeout(10_000) {
                runtimeScope.coroutineContext.job.children
                    .toList()
                    .joinAll()
            }
        }
        instrumentation.waitForIdleSync()
    }

    private fun recipes(): Map<SlimeButtonAction, List<CreatureInteraction>> =
        mapOf(
            SlimeButtonAction.HELIX to circle() + circle(),
            SlimeButtonAction.ECHO to listOf(180.0, 0.0, 180.0, 0.0).map(::contact),
            SlimeButtonAction.RICOCHET to listOf(315.0, 135.0, 315.0, 135.0).map(::contact),
            SlimeButtonAction.THUNDERBOLT to
                listOf(contact(180.0), contact(0.0), CreatureInteraction.GlyphButtonPressed) + circle(),
        )

    private fun circle() = (0..16).map { contact(it * 22.5) }

    private fun contact(degrees: Double): CreatureInteraction.ShellContact {
        val x = cos(Math.toRadians(degrees)).toFloat()
        val y = sin(Math.toRadians(degrees)).toFloat()
        val sector =
            if (abs(x) >= abs(y)) {
                if (x < 0) ShellSector.LEFT else ShellSector.RIGHT
            } else {
                if (y < 0) ShellSector.TOP else ShellSector.BOTTOM
            }
        return CreatureInteraction.ShellContact(sector, 0.8f, x, y, 0f)
    }

    private class CountingRepository(
        private val backing: CreatureRepository,
    ) : CreatureRepository {
        var saveAttempts = 0
        var failSaves = false

        override suspend fun loadOrCreate(): CreatureState = backing.loadOrCreate()

        override suspend fun save(state: CreatureState) {
            saveAttempts++
            if (failSaves) throw SQLiteException("Test save failure")
            backing.save(state)
        }
    }

    private companion object {
        const val DATABASE_NAME = "glyph-unlock-boundary-test.db"
    }
}
