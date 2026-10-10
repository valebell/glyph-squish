package com.valebell.glyphsquish.ui.slime

import android.animation.ValueAnimator
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.slime.SlimeEngine
import com.valebell.glyphsquish.slime.SlimePhenotype
import com.valebell.glyphsquish.slime.SlimeSnapshot
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

@Stable
class SlimeMatrixState internal constructor(
    initialPhenotype: SlimePhenotype,
    interactionHandler: (CreatureInteraction) -> SlimeButtonAction?,
) {
    private val engine =
        SlimeEngine(
            initialPhenotype = initialPhenotype,
            interactionHandler = interactionHandler,
        )
    private var phenotype = initialPhenotype

    var frame by mutableStateOf(engine.renderFrame())
        private set

    var isMovePlaying by mutableStateOf(engine.isMovePlaying)
        private set

    fun setPhenotype(
        newPhenotype: SlimePhenotype,
        animateFormChange: Boolean,
    ) {
        if (newPhenotype == phenotype) return
        val formChanged = newPhenotype.form != phenotype.form
        engine.setPhenotype(
            newPhenotype = newPhenotype,
            animateFormChange = animateFormChange && formChanged,
        )
        phenotype = newPhenotype
        render()
    }

    fun triggerButtonAction(action: SlimeButtonAction) {
        engine.triggerButtonAction(action)
        render()
    }

    fun setPressed(pressed: Boolean) {
        engine.setPressed(pressed)
        render()
    }

    fun performSemanticClick() {
        engine.setPressed(true)
        engine.setPressed(false)
        render()
    }

    fun setMotion(
        tiltX: Float,
        tiltY: Float,
        tiltZ: Float,
        accelerationX: Float,
        accelerationY: Float,
        accelerationZ: Float,
    ) {
        engine.setTilt(tiltX, tiltY, tiltZ)
        engine.setPhoneAcceleration(accelerationX, accelerationY, accelerationZ)
    }

    fun step(deltaSeconds: Float) {
        engine.step(deltaSeconds)
        render()
    }

    fun snapshot(): SlimeSnapshot = engine.snapshot()

    fun cancelInteraction() {
        engine.cancelPressed()
        engine.setTilt(0f, 0f, 0f)
        engine.setPhoneAcceleration(0f, 0f, 0f)
        render()
    }

    private fun render() {
        isMovePlaying = engine.isMovePlaying
        frame = engine.renderFrame()
    }
}

@Composable
fun rememberSlimeMatrixState(
    initialPhenotype: SlimePhenotype = SlimePhenotype.DEFAULT,
    interactionHandler: (CreatureInteraction) -> SlimeButtonAction? = { null },
): SlimeMatrixState {
    val currentInteractionHandler = rememberUpdatedState(interactionHandler)
    return remember {
        SlimeMatrixState(initialPhenotype) { interaction ->
            currentInteractionHandler.value(interaction)
        }
    }
}

@Composable
fun SlimeMatrix(
    state: SlimeMatrixState,
    phenotype: SlimePhenotype,
    contentDescription: String,
    modifier: Modifier = Modifier,
    animation: MatrixAnimation = MatrixAnimation(),
    interactive: Boolean = false,
    enabled: Boolean = true,
) {
    val lifecycleState by
        LocalLifecycleOwner.current.lifecycle.currentStateFlow
            .collectAsStateWithLifecycle()
    val inspectionMode = LocalInspectionMode.current
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()

    LaunchedEffect(state, phenotype, animation.formChanges, animationsEnabled) {
        state.setPhenotype(
            newPhenotype = phenotype,
            animateFormChange = animation.formChanges && animationsEnabled,
        )
    }

    AnimateMatrix(state, animation.running, lifecycleState, inspectionMode)

    DisposableEffect(state) {
        onDispose(state::cancelInteraction)
    }

    val squishLabel = stringResource(R.string.controls_squish)
    val interactionModifier = Modifier.matrixInteraction(state, interactive, enabled)

    MatrixCanvas(
        frame = { state.frame },
        modifier =
            modifier
                .then(interactionModifier)
                .semantics {
                    this.contentDescription = contentDescription
                    if (interactive) {
                        role = Role.Button
                        if (!enabled) disabled()
                        onClick(label = squishLabel) {
                            if (!enabled) return@onClick false
                            state.performSemanticClick()
                            true
                        }
                    }
                },
        drawOffPixels = true,
        showStroke = true,
    )
}

@Composable
private fun AnimateMatrix(
    state: SlimeMatrixState,
    animationRunning: Boolean,
    lifecycleState: Lifecycle.State,
    inspectionMode: Boolean,
) {
    LaunchedEffect(state, animationRunning, lifecycleState, inspectionMode) {
        if (
            !animationRunning ||
            inspectionMode ||
            !lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            return@LaunchedEffect
        }

        state.animateFrames()
    }
}

private fun Modifier.matrixInteraction(
    state: SlimeMatrixState,
    interactive: Boolean,
    enabled: Boolean,
): Modifier =
    if (interactive) {
        this
            .pointerInput(state, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        state.setPressed(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            state.setPressed(false)
                        }
                    },
                )
            }.focusable(enabled)
    } else {
        this
    }

private const val FRAME_INTERVAL_NANOS = 33_000_000L
private const val FRAME_INTERVAL_SECONDS = 0.033f
private const val NANOS_PER_SECOND = 1_000_000_000f

internal suspend fun SlimeMatrixState.animateFrames() {
    var lastStepNanos = 0L
    while (currentCoroutineContext().isActive) {
        withFrameNanos { frameNanos ->
            val elapsedNanos = frameNanos - lastStepNanos
            if (lastStepNanos == 0L || elapsedNanos >= FRAME_INTERVAL_NANOS) {
                val deltaSeconds =
                    if (lastStepNanos == 0L) {
                        FRAME_INTERVAL_SECONDS
                    } else {
                        elapsedNanos / NANOS_PER_SECOND
                    }
                step(deltaSeconds)
                lastStepNanos = frameNanos
            }
        }
    }
}

data class MatrixAnimation(
    val running: Boolean = true,
    val formChanges: Boolean = true,
)
