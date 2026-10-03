package com.valebell.glyphsquish.ui.compatibility

import android.animation.ValueAnimator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.slime.SlimeButtonAction
import com.valebell.glyphsquish.slime.SlimeEngine
import com.valebell.glyphsquish.ui.slime.MatrixCanvas
import com.valebell.glyphsquish.ui.theme.SquishSpacing
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Keep the companion, its sensors, and its feedback resources outside unsupported devices. */
@Composable
internal fun DeviceCompatibilityGate(
    isSupported: Boolean,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (isSupported) {
        content()
    } else {
        UnsupportedDeviceScreen(onClose = onClose)
    }
}

@Composable
internal fun UnsupportedDeviceScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = ValueAnimator.areAnimatorsEnabled(),
) {
    val animation = rememberGreetingAnimation(animationsEnabled)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.safeDrawingPadding(), contentAlignment = Alignment.Center) {
            val landscape = maxWidth > maxHeight
            val contentModifier =
                Modifier
                    .widthIn(max = if (landscape) 880.dp else 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(SquishSpacing.Section)
            if (landscape) {
                val previewSize = (maxHeight * 0.5f).coerceIn(120.dp, 240.dp)
                Row(
                    modifier = contentModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SquishSpacing.Section),
                ) {
                    WelcomeCharacter(animation, previewSize, Modifier.weight(1f))
                    CompatibilityMessage(onClose, Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = contentModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SquishSpacing.Section),
                ) {
                    WelcomeCharacter(animation, 240.dp)
                    CompatibilityMessage(onClose)
                }
            }
        }
    }
}

@Composable
private fun WelcomeCharacter(
    animation: GreetingAnimation,
    previewSize: Dp,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.compatibility_squish_description)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SquishSpacing.Item),
    ) {
        MatrixCanvas(
            frame = { animation.frame.value },
            modifier =
                Modifier
                    .size(previewSize)
                    .clip(CircleShape)
                    .clickable(
                        enabled = animation.replayEnabled,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.compatibility_replay),
                        onClick = animation.onReplay,
                    ).semantics { contentDescription = description },
            drawOffPixels = false,
            showStroke = false,
        )
        Text(
            stringResource(R.string.compatibility_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompatibilityMessage(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SquishSpacing.Item),
    ) {
        Text(
            stringResource(R.string.compatibility_requirement),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.compatibility_explanation),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onClose,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.padding(top = SquishSpacing.Item),
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
            Text(
                stringResource(R.string.compatibility_close),
                modifier = Modifier.padding(start = SquishSpacing.Compact),
            )
        }
    }
}

@Composable
private fun rememberGreetingAnimation(animationsEnabled: Boolean): GreetingAnimation {
    var hasPlayed by rememberSaveable { mutableStateOf(false) }
    val engine = remember { SlimeEngine() }
    val idleFrame = remember { engine.renderFrame() }
    val frame = remember { mutableStateOf(idleFrame) }
    var isIdle by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, animationsEnabled) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (animationsEnabled) {
                isRunning = true
                try {
                    if (!hasPlayed) {
                        delay(600)
                        hasPlayed = true
                        engine.triggerButtonAction(SlimeButtonAction.BURST)
                    }
                    engine.animateGreeting {
                        frame.value = it
                        isIdle = engine.snapshot().activeButtonAction == null
                    }
                } finally {
                    isRunning = false
                }
            } else {
                frame.value = idleFrame
            }
        }
    }
    return GreetingAnimation(
        frame = frame,
        replayEnabled = animationsEnabled && isRunning && isIdle,
        onReplay = {
            if (animationsEnabled && isRunning && isIdle) {
                // The engine also guards active moves, including multiple taps in one frame.
                engine.handleGlyphButtonPress { SlimeButtonAction.BURST }
                isIdle = false
                frame.value = engine.renderFrame()
            }
        },
    )
}

private data class GreetingAnimation(
    val frame: State<IntArray>,
    val replayEnabled: Boolean,
    val onReplay: () -> Unit,
)

/** Continue into the engine's natural floating and blinking after Burst, at about 30 fps. */
private suspend fun SlimeEngine.animateGreeting(onFrame: (IntArray) -> Unit) {
    var previousFrame = withFrameNanos { it }
    while (currentCoroutineContext().isActive) {
        val now = withFrameNanos { it }
        if (now - previousFrame < 33_000_000L) continue
        val deltaSeconds = ((now - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
        step(deltaSeconds)
        onFrame(renderFrame())
        previousFrame = now
    }
}
