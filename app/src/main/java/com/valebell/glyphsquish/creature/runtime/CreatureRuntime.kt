package com.valebell.glyphsquish.creature.runtime

import android.database.sqlite.SQLiteException
import android.os.Looper
import android.os.SystemClock
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureForms
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.domain.CreatureRepository
import com.valebell.glyphsquish.creature.domain.CreatureSession
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CreatureRuntime(
    private val repository: CreatureRepository,
    private val scope: CoroutineScope,
    private val clockMillis: () -> Long = SystemClock::elapsedRealtime,
) : CreatureRuntimeContract {
    private val mutableState = MutableStateFlow<CreatureRuntimeState>(CreatureRuntimeState.Loading)
    override val state: StateFlow<CreatureRuntimeState> = mutableState.asStateFlow()

    private val mutablePersistenceErrors =
        MutableSharedFlow<CreaturePersistenceError>(
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val persistenceErrors: SharedFlow<CreaturePersistenceError> =
        mutablePersistenceErrors.asSharedFlow()

    private val mutablePendingSaveFailure = MutableStateFlow<Long?>(null)
    override val pendingSaveFailure: StateFlow<Long?> = mutablePendingSaveFailure.asStateFlow()

    private var session: CreatureSession? = null
    private var loadInProgress = false
    private var lastFormCycleMillis = Long.MIN_VALUE
    private var saveFailureSerial = 0L
    private val saveMutex = Mutex()

    init {
        load()
    }

    override fun retrySave() {
        checkMainThread()
        session?.state?.let(::persist)
    }

    override fun retryLoad() {
        checkMainThread()
        if (state.value is CreatureRuntimeState.LoadFailed) load()
    }

    override fun handleInteraction(
        interaction: CreatureInteraction,
        source: CreatureInputSource,
    ): SlimeButtonAction? {
        checkMainThread()
        val activeSession = session ?: return null
        if (source != CreatureInputSource.GLYPH) return null
        val before = activeSession.state
        val outcome = activeSession.handle(interaction, clockMillis())
        if (outcome.state != before) {
            notifyAndPersist(outcome.state)
        }
        // Glyph discovery plays once, as soon as the move is learned. Gallery
        // acknowledgement is independent; learned moves replay only on request.
        return outcome.unlockedMove
    }

    override fun handleGlyphButtonPress(): SlimeButtonAction {
        checkMainThread()
        val activeSession = session ?: return SlimeButtonAction.BURST
        handleInteraction(CreatureInteraction.GlyphButtonPressed, CreatureInputSource.GLYPH)
        // The selected move is part of Thunderbolt's choreography. Keep its
        // bounce/button prefix through playback while it can still be discovered.
        if (
            SlimeButtonAction.THUNDERBOLT in activeSession.state.unlockedMoves ||
            activeSession.recognition.recipes[SlimeButtonAction.THUNDERBOLT]?.completedSteps != 2
        ) {
            activeSession.resetRecognition(clockMillis())
        }
        return activeSession.state.selectedMove
    }

    override fun setGlyphMovePlaybackActive(active: Boolean) {
        checkMainThread()
        session?.setRecognitionPaused(active, clockMillis())
    }

    override fun selectMove(move: SlimeButtonAction): Boolean {
        checkMainThread()
        val activeSession = session ?: return false
        if (move !in activeSession.state.unlockedMoves) return false
        return replace(CreatureProgression.selectMove(activeSession.state, move))
    }

    override fun markFormsSeen(forms: Set<CreatureForm>) {
        checkMainThread()
        session?.state?.let { replace(CreatureProgression.markFormsSeen(it, forms)) }
    }

    override fun selectForm(form: CreatureForm): Boolean {
        checkMainThread()
        val activeSession = session ?: return false
        if (form !in CreatureForms.availableFor(activeSession.state.unlockedMoves)) return false
        return replace(CreatureProgression.selectForm(activeSession.state, form))
    }

    override fun cycleFormFromShake(source: CreatureInputSource): CreatureForm? {
        checkMainThread()
        val activeSession = session ?: return null
        val nowMillis = clockMillis()
        // A physical shake must interrupt recognition even when form cycling is debounced.
        if (source == CreatureInputSource.GLYPH) {
            activeSession.resetRecognition(
                nowMillis = nowMillis,
                suppressContactsForMillis = SHAKE_CONTACT_SUPPRESSION_MILLIS,
            )
        }
        if (
            lastFormCycleMillis != Long.MIN_VALUE &&
            nowMillis - lastFormCycleMillis < FORM_CYCLE_DEBOUNCE_MILLIS
        ) {
            return null
        }
        lastFormCycleMillis = nowMillis
        val next =
            CreatureForms.nextUnlocked(
                activeSession.state.currentForm,
                activeSession.state.unlockedMoves,
            )
        if (next == activeSession.state.currentForm) return null
        replace(CreatureProgression.selectForm(activeSession.state, next))
        return next
    }

    override fun resetMoveRecognition() {
        checkMainThread()
        session?.resetRecognition(clockMillis())
    }

    private fun replace(state: CreatureState): Boolean {
        val activeSession = session ?: return false
        if (state == activeSession.state) return true
        activeSession.replace(state)
        notifyAndPersist(state)
        return true
    }

    private fun notifyAndPersist(state: CreatureState) {
        mutableState.value = CreatureRuntimeState.Ready(state)
        persist(state)
    }

    private fun load() {
        if (loadInProgress) return
        loadInProgress = true
        mutableState.value = CreatureRuntimeState.Loading
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val loaded = repository.loadOrCreate()
                session = CreatureSession(loaded)
                loadInProgress = false
                mutableState.value = CreatureRuntimeState.Ready(loaded)
            } catch (error: CancellationException) {
                throw error
            } catch (error: SQLiteException) {
                completeLoadFailure("Unable to load Squish", error)
            } catch (error: IllegalStateException) {
                completeLoadFailure("Stored character data is invalid", error)
            } catch (error: IllegalArgumentException) {
                completeLoadFailure("Stored character data is invalid", error)
            }
        }
    }

    private fun persist(state: CreatureState) {
        scope.launch(Dispatchers.Main.immediate) {
            // Hold across suspension: newer revisions must never overtake an earlier save.
            saveMutex.withLock {
                try {
                    scope.ensureActive()
                    repository.save(state)
                    // A successful save persists the full state, so it also covers
                    // every earlier failed attempt: nothing is pending anymore.
                    mutablePendingSaveFailure.value = null
                } catch (error: CancellationException) {
                    throw error
                } catch (error: SQLiteException) {
                    failSave("Unable to save Squish", error)
                } catch (error: IllegalStateException) {
                    failSave("Character state changed unexpectedly", error)
                }
            }
        }
    }

    private fun completeLoadFailure(
        message: String,
        error: Exception,
    ) {
        session = null
        loadInProgress = false
        mutableState.value = CreatureRuntimeState.LoadFailed(errorMessage(message, error))
    }

    private fun failSave(
        message: String,
        error: Exception,
    ) {
        // Retain the failure until a save succeeds; the serial lets the UI tell
        // a new failure apart from one the user already dismissed or retried.
        mutablePendingSaveFailure.value = ++saveFailureSerial
        emitPersistenceError(message, error)
    }

    private fun emitPersistenceError(
        message: String,
        error: Exception,
    ) {
        mutablePersistenceErrors.tryEmit(CreaturePersistenceError(errorMessage(message, error)))
    }

    private fun errorMessage(
        message: String,
        error: Exception,
    ): String = "$message: ${error.message ?: error.javaClass.simpleName}"

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "CreatureRuntime must be used from the main thread"
        }
    }

    private companion object {
        const val FORM_CYCLE_DEBOUNCE_MILLIS = 1_500L
        const val SHAKE_CONTACT_SUPPRESSION_MILLIS = 900L
    }
}
