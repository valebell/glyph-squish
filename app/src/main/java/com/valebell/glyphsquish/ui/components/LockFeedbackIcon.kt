package com.valebell.glyphsquish.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun LockFeedbackIcon(
    feedbackTrigger: Int,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(feedbackTrigger) {
        // A new tap cancels the previous wiggle instead of queuing more motion.
        rotation.snapTo(0f)
        if (feedbackTrigger != 0 && ValueAnimator.areAnimatorsEnabled()) {
            rotation.animateTo(
                targetValue = 0f,
                animationSpec =
                    keyframes {
                        durationMillis = 320
                        0f at 0 using FastOutSlowInEasing
                        -12f at 60 using FastOutSlowInEasing
                        10f at 125 using FastOutSlowInEasing
                        -7f at 190 using FastOutSlowInEasing
                        4f at 255 using FastOutSlowInEasing
                        0f at 320
                    },
            )
        }
    }
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        modifier = modifier.graphicsLayer { rotationZ = rotation.value },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
