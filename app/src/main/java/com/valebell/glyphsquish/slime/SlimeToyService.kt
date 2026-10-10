package com.valebell.glyphsquish.slime

import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.creature.runtime.CreatureInputSource
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeState
import com.valebell.glyphsquish.creatureRuntime
import com.valebell.glyphsquish.launcherIconSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SlimeToyService :
    Service(),
    GlyphMatrixSessionCallbacks {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var glyphMatrixManager: GlyphMatrixManager? = null
    private var engine = SlimeEngine(shadeBody = false)
    private val idleBrightness = GlyphIdleBrightness()
    private val frameDelivery = GlyphFrameDelivery()
    private var sessionActive = false
    private var matrixReady = false
    private var lastFrameTime = 0L
    private var displayedCreature: CreatureState? = null
    private val creatureRuntime by lazy { applicationContext.creatureRuntime() }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var runtimeCollectionJobs = emptyList<Job>()

    private val motionSensor by lazy {
        SpatialMotionSensor(
            context = applicationContext,
            viewedFromBack = true,
            followsDisplayRotation = false,
            onMotionChanged = ::applyMotion,
            onShake = ::handleShake,
        )
    }

    private val glyphEventHandler =
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                if (message.what != GlyphToy.MSG_GLYPH_TOY) {
                    super.handleMessage(message)
                    return
                }

                when (message.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)) {
                    GlyphToy.EVENT_ACTION_DOWN -> {
                        idleBrightness.wake(SystemClock.uptimeMillis())
                        engine.setPressed(true)
                    }

                    GlyphToy.EVENT_ACTION_UP -> {
                        idleBrightness.wake(SystemClock.uptimeMillis())
                        engine.setPressed(false)
                    }

                    GlyphToy.EVENT_CHANGE -> {
                        idleBrightness.wake(SystemClock.uptimeMillis())
                        engine.handleGlyphButtonPress(creatureRuntime::handleGlyphButtonPress)
                    }
                }
            }
        }

    private val serviceMessenger = Messenger(glyphEventHandler)

    private var callbackBridge: GlyphMatrixCallbackBridge? = null

    override fun onGlyphServiceConnected() {
        if (!sessionActive) return
        val manager = glyphMatrixManager ?: return

        frameDelivery.reset()
        try {
            if (!manager.register(Glyph.DEVICE_23112)) {
                Log.e(LOG_TAG, "Phone (3) registration was rejected")
                stopRendering()
                return
            }
            matrixReady = true
            idleBrightness.reset(SystemClock.uptimeMillis())
            motionSensor.start()
            lastFrameTime = 0L
            mainHandler.post(frameLoop)
        } catch (error: GlyphException) {
            handleMatrixFailure("Unable to register Phone (3)", error)
        }
    }

    override fun onGlyphServiceDisconnected() {
        stopRendering()
        Log.w(LOG_TAG, "Glyph Matrix service disconnected")
    }

    private val frameLoop = Runnable(::sendNextFrame)

    override fun onBind(intent: Intent?): IBinder? {
        if (!Common.is23112()) return null
        launcherIconSync().setGlyphActive(true)
        if (!sessionActive) {
            creatureRuntime.resetMoveRecognition()
            sessionActive = true
            displayedCreature =
                (creatureRuntime.state.value as? CreatureRuntimeState.Ready)?.creature
            engine =
                SlimeEngine(
                    shadeBody = false,
                    initialPhenotype =
                        displayedCreature
                            ?.let(SlimePhenotype::from)
                            ?: SlimePhenotype.DEFAULT,
                    interactionHandler = { creatureRuntime.handleInteraction(it, CreatureInputSource.GLYPH) },
                    onMovePlaybackChanged = creatureRuntime::setGlyphMovePlaybackActive,
                )
            val manager = GlyphMatrixManager.getInstance(applicationContext)
            if (manager == null) {
                Log.e(LOG_TAG, "Glyph Matrix manager is unavailable")
            } else {
                glyphMatrixManager = manager
                val bridge = GlyphMatrixCallbackBridge(this)
                callbackBridge = bridge
                manager.init(bridge)
            }
        }
        collectRuntime()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        runtimeCollectionJobs.forEach { job -> job.cancel() }
        runtimeCollectionJobs = emptyList()
        releaseSession()
        return false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        runtimeCollectionJobs = emptyList()
        releaseSession()
        super.onDestroy()
    }

    private fun collectRuntime() {
        if (runtimeCollectionJobs.size == 2 && runtimeCollectionJobs.all { job -> job.isActive }) return
        runtimeCollectionJobs.forEach { job -> job.cancel() }
        runtimeCollectionJobs =
            listOf(
                serviceScope.launch {
                    creatureRuntime.state.collect { runtimeState ->
                        when (runtimeState) {
                            CreatureRuntimeState.Loading -> Unit
                            is CreatureRuntimeState.LoadFailed -> Log.e(LOG_TAG, runtimeState.message)
                            is CreatureRuntimeState.Ready -> showCreature(runtimeState.creature)
                        }
                    }
                },
                serviceScope.launch {
                    creatureRuntime.persistenceErrors.collect { error ->
                        Log.e(LOG_TAG, error.message)
                    }
                },
            )
    }

    private fun showCreature(state: CreatureState) {
        val previousState = displayedCreature
        if (
            previousState == null ||
            previousState.currentForm != state.currentForm ||
            previousState.selectedMove != state.selectedMove
        ) {
            idleBrightness.wake(SystemClock.uptimeMillis())
        }
        val phenotype = SlimePhenotype.from(state)
        if (previousState == null) {
            engine.snapToPhenotype(phenotype)
        } else {
            engine.setPhenotype(
                newPhenotype = phenotype,
                animateFormChange =
                    previousState.currentForm != state.currentForm &&
                        ValueAnimator.areAnimatorsEnabled(),
            )
        }
        displayedCreature = state
        if (previousState == null) {
            engine.triggerSemanticAnimation(SlimeAnimationCue.INVITATION)
        }
    }

    private fun applyMotion(
        tiltX: Float,
        tiltY: Float,
        tiltZ: Float,
        accelerationX: Float,
        accelerationY: Float,
        accelerationZ: Float,
    ) {
        idleBrightness.onMotion(
            SystemClock.uptimeMillis(),
            tiltX,
            tiltY,
            tiltZ,
            accelerationX,
            accelerationY,
            accelerationZ,
        )
        engine.setTilt(tiltX, tiltY, tiltZ)
        engine.setPhoneAcceleration(accelerationX, accelerationY, accelerationZ)
    }

    private fun handleShake() {
        idleBrightness.wake(SystemClock.uptimeMillis())
        val snapshot = engine.snapshot()
        if (snapshot.activeButtonAction != null || snapshot.isFormTransitioning) return
        creatureRuntime.cycleFormFromShake(CreatureInputSource.GLYPH)
    }

    private fun sendNextFrame() {
        if (!sessionActive || !matrixReady) return
        val manager = glyphMatrixManager ?: return

        val now = SystemClock.uptimeMillis()
        val deltaSeconds =
            if (lastFrameTime == 0L) {
                FRAME_INTERVAL_MILLIS / 1_000f
            } else {
                ((now - lastFrameTime) / 1_000f).coerceAtMost(MAX_FRAME_DELTA_SECONDS)
            }
        lastFrameTime = now
        engine.step(deltaSeconds)
        val snapshot = engine.snapshot()
        val brightnessScale =
            idleBrightness.level(
                now,
                interactionActive =
                    snapshot.isPressed || snapshot.activeButtonAction != null || snapshot.isFormTransitioning,
            )

        try {
            // Raw frames need 12-bit intensity; SDK object brightness is a separate 8-bit multiplier.
            // See docs/glyph-brightness.md and GlyphFrameEncodingTest before changing this boundary.
            frameDelivery.sendIfNeeded(GlyphFrameEncoder.encode(engine.renderFrame(), brightnessScale), now) { frame ->
                manager.setMatrixFrame(frame)
            }
        } catch (error: GlyphException) {
            handleMatrixFailure("Unable to send slime frame", error)
            return
        }
        mainHandler.postDelayed(frameLoop, FRAME_INTERVAL_MILLIS)
    }

    private fun handleMatrixFailure(
        message: String,
        error: GlyphException,
    ) {
        Log.e(LOG_TAG, message, error)
        stopRendering()
    }

    private fun stopRendering() {
        creatureRuntime.resetMoveRecognition()
        matrixReady = false
        frameDelivery.reset()
        mainHandler.removeCallbacks(frameLoop)
        motionSensor.stop()
        engine.cancelPressed()
        engine.setTilt(0f, 0f, 0f)
        engine.setPhoneAcceleration(0f, 0f, 0f)
        lastFrameTime = 0L
    }

    private fun releaseSession() {
        if (!sessionActive && glyphMatrixManager == null) return
        sessionActive = false
        stopRendering()

        glyphMatrixManager?.let { manager ->
            callbackBridge?.detach()
            try {
                manager.turnOff()
            } catch (error: GlyphException) {
                Log.w(LOG_TAG, "Unable to clear Glyph Matrix", error)
            }
            manager.unInit()
        }
        glyphMatrixManager = null
        callbackBridge = null
        displayedCreature = null
        launcherIconSync().setGlyphActive(false)
    }

    private companion object {
        const val LOG_TAG = "SlimeToyService"
        const val FRAME_INTERVAL_MILLIS = 33L
        const val MAX_FRAME_DELTA_SECONDS = 0.1f
    }
}
