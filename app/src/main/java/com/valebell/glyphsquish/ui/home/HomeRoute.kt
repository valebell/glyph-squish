package com.valebell.glyphsquish.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeState
import com.valebell.glyphsquish.slime.SlimePhenotype
import com.valebell.glyphsquish.slime.SpatialMotionSensor
import com.valebell.glyphsquish.ui.SquishViewModel
import com.valebell.glyphsquish.ui.audio.GlyphSoundEffect
import com.valebell.glyphsquish.ui.slime.SlimeMatrixState
import com.valebell.glyphsquish.ui.slime.animateFrames
import com.valebell.glyphsquish.ui.slime.rememberSlimeMatrixState

@Composable
fun HomeRoute(
    uiState: CreatureRuntimeState,
    selectedSection: SquishSection,
    onSectionChange: (SquishSection) -> Unit,
    viewModel: SquishViewModel,
    playSound: (GlyphSoundEffect) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val creature = (uiState as? CreatureRuntimeState.Ready)?.creature
    val previewState =
        rememberSlimeMatrixState(
            initialPhenotype = creature?.let(SlimePhenotype::from) ?: SlimePhenotype.DEFAULT,
            interactionHandler = viewModel::handlePreviewInteraction,
        )

    ObservePreviewMotion(previewState, viewModel, playSound, ready = creature != null)
    AnimatePreviewPlayback(previewState, ready = creature != null)

    HomeScreen(
        creature = creature,
        loadFailure = (uiState as? CreatureRuntimeState.LoadFailed)?.message,
        selectedSection = selectedSection,
        previewState = previewState,
        modifier = modifier,
        actions =
            HomeActions(
                onSelectSection = { section ->
                    if (section != selectedSection) {
                        playSound(
                            if (section == SquishSection.MOVES) {
                                GlyphSoundEffect.SLIDE_FORWARD
                            } else {
                                GlyphSoundEffect.SLIDE_BACKWARD
                            },
                        )
                        onSectionChange(section)
                    }
                },
                onReplayMove = {
                    creature?.selectedMove?.let { move ->
                        playSound(GlyphSoundEffect.forMove(move))
                        previewState.triggerButtonAction(move)
                    }
                },
                onOpenSettings = onOpenSettings,
                onRetry = viewModel::retryLoad,
                gallery = galleryActions(creature, viewModel, previewState, playSound),
            ),
    )
}

@Composable
private fun ObservePreviewMotion(
    previewState: SlimeMatrixState,
    viewModel: SquishViewModel,
    playSound: (GlyphSoundEffect) -> Unit,
    ready: Boolean,
) {
    val context = LocalContext.current.applicationContext
    val onShake by
        rememberUpdatedState {
            val snapshot = previewState.snapshot()
            if (snapshot.activeButtonAction == null && !snapshot.isFormTransitioning) {
                val nextForm = viewModel.cycleFormFromShake()
                if (nextForm != null) {
                    playSound(GlyphSoundEffect.forForm(nextForm))
                }
            }
        }
    val motionSensor =
        remember(context, previewState) {
            SpatialMotionSensor(
                context = context,
                viewedFromBack = false,
                followsDisplayRotation = true,
                onMotionChanged = previewState::setMotion,
                onShake = { onShake() },
            )
        }
    PreviewMotionEffect(
        ready = ready,
        sensorKey = motionSensor,
        start = { motionSensor.start() },
        stop = {
            motionSensor.stop()
            previewState.cancelInteraction()
        },
    )
}

@Composable
internal fun PreviewMotionEffect(
    ready: Boolean,
    sensorKey: Any,
    start: () -> Unit,
    stop: () -> Unit,
) {
    if (ready) {
        LifecycleResumeEffect(sensorKey) {
            start()
            onPauseOrDispose { stop() }
        }
    }
}

/**
 * Steps the preview engine from this route, including while the preview composable is
 * scrolled out of the lazy gallery, so in-flight playback finishes and the shake
 * guard is not left blocked by frozen busy state.
 */
@Composable
internal fun AnimatePreviewPlayback(
    previewState: SlimeMatrixState,
    ready: Boolean,
) {
    if (ready) {
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(previewState, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                previewState.animateFrames()
            }
        }
    }
}

private fun galleryActions(
    creature: CreatureState?,
    viewModel: SquishViewModel,
    previewState: SlimeMatrixState,
    playSound: (GlyphSoundEffect) -> Unit,
): GalleryActions =
    GalleryActions(
        onFormsVisible = viewModel::markFormsSeen,
        onSelectForm = { form ->
            if (form != creature?.currentForm) {
                val effect = if (viewModel.selectForm(form)) GlyphSoundEffect.forForm(form) else GlyphSoundEffect.SELECT
                playSound(effect)
            }
        },
        onSelectMove = { move ->
            if (viewModel.selectMove(move)) {
                playSound(GlyphSoundEffect.forMove(move))
                previewState.triggerButtonAction(move)
            } else {
                playSound(GlyphSoundEffect.SELECT)
            }
        },
    )
