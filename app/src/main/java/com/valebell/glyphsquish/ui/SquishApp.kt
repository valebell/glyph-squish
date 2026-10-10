package com.valebell.glyphsquish.ui

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valebell.glyphsquish.BuildConfig
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.ui.audio.GlyphSoundEffect
import com.valebell.glyphsquish.ui.audio.rememberGlyphHaptic
import com.valebell.glyphsquish.ui.audio.rememberGlyphSound
import com.valebell.glyphsquish.ui.home.HomeRoute
import com.valebell.glyphsquish.ui.home.SquishSection
import com.valebell.glyphsquish.ui.settings.AppearanceState
import com.valebell.glyphsquish.ui.settings.SettingsScreen
import com.valebell.glyphsquish.ui.settings.rememberAppearanceState
import com.valebell.glyphsquish.ui.settings.rememberFeedbackState
import com.valebell.glyphsquish.ui.theme.SquishSpacing

@Composable
internal fun SquishApp(
    viewModel: SquishViewModel,
    launchGlyphToys: () -> Boolean,
    modifier: Modifier = Modifier,
    shortcutSection: SquishSection? = null,
    onShortcutHandled: () -> Unit = {},
    appearance: AppearanceState = rememberAppearanceState(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playHaptic = rememberGlyphHaptic()
    val feedback = rememberFeedbackState()
    val isSoundEnabled = feedback.settings?.isSoundEnabled == true
    val isVibrationEnabled = feedback.settings?.isVibrationEnabled == true
    val playSound = rememberGlyphSound(enabled = isSoundEnabled)
    val playFeedback: (GlyphSoundEffect) -> Unit =
        remember(playSound, playHaptic, isSoundEnabled, isVibrationEnabled) {
            { effect ->
                if (isSoundEnabled) playSound(effect)
                if (isVibrationEnabled) {
                    playHaptic(effect, isSoundEnabled)
                }
            }
        }
    var screen by rememberSaveable { mutableStateOf(SquishScreen.HOME) }
    var selectedSection by rememberSaveable { mutableStateOf(shortcutSection ?: SquishSection.FORMS) }
    LaunchedEffect(shortcutSection) {
        shortcutSection?.let {
            selectedSection = it
            screen = SquishScreen.HOME
            onShortcutHandled()
        }
    }
    // The runtime retains the failure until a save succeeds, so the card also
    // appears when the save failed before this screen started collecting.
    val pendingSaveFailure by viewModel.pendingSaveFailure.collectAsStateWithLifecycle()
    var dismissedSaveFailure by rememberSaveable { mutableStateOf<Long?>(null) }
    val hasSaveError = pendingSaveFailure != null && pendingSaveFailure != dismissedSaveFailure
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    val layoutDirection = LocalLayoutDirection.current
    BackHandler(enabled = screen == SquishScreen.SETTINGS) {
        screen = SquishScreen.HOME
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (hasSaveError) {
            Card(Modifier.fillMaxWidth().statusBarsPadding().padding(SquishSpacing.Screen)) {
                Column(Modifier.padding(SquishSpacing.Card)) {
                    Text(
                        stringResource(R.string.save_error),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Row {
                        TextButton(onClick = {
                            playFeedback(GlyphSoundEffect.SELECT)
                            dismissedSaveFailure = pendingSaveFailure
                            viewModel.retrySave()
                        }) { Text(stringResource(R.string.retry)) }
                        TextButton(onClick = {
                            playFeedback(GlyphSoundEffect.SELECT)
                            dismissedSaveFailure = pendingSaveFailure
                        }) { Text(stringResource(R.string.dismiss)) }
                    }
                }
            }
        }
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = screen,
            transitionSpec =
                if (animationsEnabled) {
                    screenTransitionSpec(layoutDirection)
                } else {
                    ReducedMotionScreenTransitionSpec
                },
            label = "settings screen transition",
        ) { targetScreen ->
            when (targetScreen) {
                SquishScreen.HOME -> {
                    HomeRoute(
                        uiState = uiState,
                        selectedSection = selectedSection,
                        onSectionChange = { selectedSection = it },
                        viewModel = viewModel,
                        playSound = playFeedback,
                        onOpenSettings = {
                            playFeedback(GlyphSoundEffect.SLIDE_FORWARD)
                            screen = SquishScreen.SETTINGS
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                SquishScreen.SETTINGS -> {
                    SettingsScreen(
                        versionName = BuildConfig.VERSION_NAME,
                        feedback = feedback,
                        appearance = appearance,
                        onOpenGlyphToys = {
                            playFeedback(GlyphSoundEffect.SLIDE_FORWARD)
                            launchGlyphToys()
                        },
                        onBack = {
                            playFeedback(GlyphSoundEffect.SLIDE_BACKWARD)
                            screen = SquishScreen.HOME
                        },
                        playFeedback = playFeedback,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private const val SCREEN_TRANSITION_MILLIS = 260

private fun screenTransitionSpec(
    layoutDirection: LayoutDirection,
): AnimatedContentTransitionScope<SquishScreen>.() -> ContentTransform =
    {
        val forward = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        val direction = if (targetState == SquishScreen.SETTINGS) forward else -forward
        slideInHorizontally(
            animationSpec =
                tween(
                    durationMillis = SCREEN_TRANSITION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
        ) { width -> direction * width }
            .togetherWith(
                slideOutHorizontally(
                    animationSpec =
                        tween(
                            durationMillis = SCREEN_TRANSITION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                ) { width -> -direction * width },
            )
    }

private val ReducedMotionScreenTransitionSpec:
    AnimatedContentTransitionScope<SquishScreen>.() -> ContentTransform = {
        EnterTransition.None togetherWith ExitTransition.None
    }

private enum class SquishScreen {
    HOME,
    SETTINGS,
}
