package com.valebell.glyphsquish.ui.forms

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

/** Keep the lock readable before dissolving it into the newly discovered form. */
@Composable
internal fun rememberFormRevealProgress(
    isNew: Boolean,
    visible: Boolean,
): State<Float> {
    var revealed by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (isNew && !revealed) 0f else 1f) }
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    LaunchedEffect(isNew, visible, animationsEnabled) {
        when {
            !isNew || revealed || !animationsEnabled -> {
                progress.snapTo(1f)
                if (isNew) revealed = true
            }

            visible -> {
                progress.snapTo(0f)
                delay(400.milliseconds)
                progress.animateTo(1f, tween(1200, easing = LinearEasing))
                revealed = true
            }

            else -> {
                progress.snapTo(0f)
            }
        }
    }
    return progress.asState()
}

/** A soft breathing outline and a pixel burst synchronized with the lock's dissolve. */
@Composable
internal fun Modifier.formDiscoveryEffect(
    isNew: Boolean,
    visible: Boolean,
    progress: State<Float>,
): Modifier {
    val color = MaterialTheme.colorScheme.tertiary
    val shape = CardDefaults.shape
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    val glow =
        if (isNew && visible && animationsEnabled) {
            rememberInfiniteTransition(label = "form discovery glow").animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "form discovery glow opacity",
            )
        } else {
            null
        }
    return drawWithContent {
        drawContent()
        if (isNew && visible) {
            val outline = shape.createOutline(size, layoutDirection, this)
            // Layered low-alpha strokes soften the edge; only their opacity gently breathes.
            val intensity = (glow?.value ?: 1f) * progress.value
            drawOutline(outline, color.copy(alpha = 0.018f * intensity), style = Stroke(6.dp.toPx()))
            drawOutline(outline, color.copy(alpha = 0.035f * intensity), style = Stroke(3.dp.toPx()))
            drawOutline(outline, color.copy(alpha = 0.18f * intensity), style = Stroke(1.dp.toPx()))
            val fraction = ((progress.value - 0.2f) / 0.8f).coerceIn(0f, 1f)
            if (progress.value > 0.2f && fraction < 1f) {
                val pixel = 3.dp.toPx()
                val center = Offset(size.width / 2f, 52.dp.toPx())
                repeat(12) { index ->
                    val angle = index * PI * 2 / 12
                    val distance = (27 + fraction * (17 + index % 3 * 4)).dp.toPx()
                    val point = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * distance
                    drawRect(
                        color = color.copy(alpha = (1f - fraction) * 0.6f),
                        topLeft = Offset(round(point.x / pixel) * pixel, round(point.y / pixel) * pixel),
                        size = Size(pixel, pixel),
                    )
                }
            }
        }
    }
}
