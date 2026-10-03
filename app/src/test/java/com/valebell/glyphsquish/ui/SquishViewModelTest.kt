package com.valebell.glyphsquish.ui

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.runtime.CreatureInputSource
import com.valebell.glyphsquish.creature.runtime.CreaturePersistenceError
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeContract
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeState
import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SquishViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `UI observes the runtime state without an intermediate copy`() {
        val runtime = FakeCreatureRuntime()
        val viewModel = SquishViewModel(runtime)

        assertSame(runtime.state, viewModel.uiState)
        assertEquals(CreatureRuntimeState.Loading, viewModel.uiState.value)

        val creature = CreatureProgression.initialState()
        runtime.mutableState.value = CreatureRuntimeState.Ready(creature)
        assertEquals(CreatureRuntimeState.Ready(creature), viewModel.uiState.value)

        runtime.mutableState.value = CreatureRuntimeState.LoadFailed("database unavailable")
        assertEquals(CreatureRuntimeState.LoadFailed("database unavailable"), viewModel.uiState.value)

        viewModel.retryLoad()
        assertEquals(1, runtime.retryCount)
    }

    @Test
    fun `preview and shake are explicitly companion input`() {
        val runtime = FakeCreatureRuntime(CreatureRuntimeState.Ready(CreatureProgression.initialState()))
        val viewModel = SquishViewModel(runtime)
        val interaction = CreatureInteraction.GlyphButtonPressed
        viewModel.handlePreviewInteraction(interaction)
        assertEquals(interaction, runtime.lastInteraction)
        assertEquals(CreatureInputSource.COMPANION, runtime.lastSource)
        runtime.cycledForm = CreatureForm.CAT
        assertEquals(CreatureForm.CAT, viewModel.cycleFormFromShake())
        assertEquals(CreatureInputSource.COMPANION, runtime.shakeSource)
    }

    @Test
    fun `persistence errors do not replay to a later collector`() =
        runTest {
            val runtime = FakeCreatureRuntime()
            val viewModel = SquishViewModel(runtime)
            runtime.mutablePersistenceErrors.emit(CreaturePersistenceError("stale"))

            val events = mutableListOf<CreaturePersistenceError>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.persistenceErrors.collect { event -> events.add(event) }
            }
            assertTrue(events.isEmpty())

            runtime.mutablePersistenceErrors.emit(CreaturePersistenceError("current"))
            assertEquals(listOf(CreaturePersistenceError("current")), events)
        }

    @Test
    fun `selection actions preserve runtime results`() {
        val runtime = FakeCreatureRuntime(CreatureRuntimeState.Ready(CreatureProgression.initialState()))
        val viewModel = SquishViewModel(runtime)
        runtime.selectionResult = false

        assertFalse(viewModel.selectForm(CreatureForm.SHARK))
        assertFalse(viewModel.selectMove(SlimeButtonAction.ECHO))
        assertEquals(CreatureForm.SHARK, runtime.selectedForm)
        assertEquals(SlimeButtonAction.ECHO, runtime.selectedMove)

        viewModel.retrySave()
        assertEquals(1, runtime.saveRetryCount)
    }
}

private class FakeCreatureRuntime(
    initialState: CreatureRuntimeState = CreatureRuntimeState.Loading,
) : CreatureRuntimeContract {
    val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<CreatureRuntimeState> = mutableState

    val mutablePersistenceErrors = MutableSharedFlow<CreaturePersistenceError>()
    override val persistenceErrors: SharedFlow<CreaturePersistenceError> = mutablePersistenceErrors

    var retryCount = 0
    var interactionResult: SlimeButtonAction? = null
    var lastInteraction: CreatureInteraction? = null
    var cycledForm: CreatureForm? = null
    var selectionResult = true
    var selectedForm: CreatureForm? = null
    var selectedMove: SlimeButtonAction? = null
    var lastSource: CreatureInputSource? = null
    var shakeSource: CreatureInputSource? = null
    var saveRetryCount = 0

    override fun retrySave() {
        saveRetryCount++
    }

    override fun retryLoad() {
        retryCount++
    }

    override fun handleInteraction(
        interaction: CreatureInteraction,
        source: CreatureInputSource,
    ): SlimeButtonAction? {
        lastInteraction = interaction
        lastSource = source
        return interactionResult
    }

    override fun handleGlyphButtonPress(): SlimeButtonAction = SlimeButtonAction.BURST

    override fun setGlyphMovePlaybackActive(active: Boolean) = Unit

    override fun selectMove(move: SlimeButtonAction): Boolean {
        selectedMove = move
        return selectionResult
    }

    override fun markFormsSeen(forms: Set<CreatureForm>) {}

    override fun selectForm(form: CreatureForm): Boolean {
        selectedForm = form
        return selectionResult
    }

    override fun cycleFormFromShake(source: CreatureInputSource): CreatureForm? {
        shakeSource = source
        return cycledForm
    }

    override fun resetMoveRecognition() = Unit
}
