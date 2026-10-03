package com.valebell.glyphsquish.ui.home

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.ui.theme.LocalPreviewColors
import com.valebell.glyphsquish.ui.theme.SquishSpacing
import kotlin.math.abs

@Composable
internal fun ReplayMoveButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = ValueAnimator.areAnimatorsEnabled(),
) {
    val previewColors = LocalPreviewColors.current
    val replayLabel = stringResource(R.string.move_gallery_replay)
    val playingLabel = stringResource(R.string.move_gallery_playing)
    val playbackStatus =
        if (isPlaying) playingLabel else stringResource(R.string.move_gallery_replay_ready)
    val containerAlpha =
        animateFloatAsState(
            targetValue = if (isPlaying) 0f else 1f,
            animationSpec = tween(durationMillis = if (animationsEnabled) 220 else 0),
            label = "replay container fade",
        )
    val containerColor = previewColors.button.copy(alpha = previewColors.button.alpha * containerAlpha.value)
    Button(
        onClick = onClick,
        enabled = !isPlaying,
        shapes = ButtonDefaults.shapes(),
        modifier =
            modifier
                .replayShimmer(isPlaying, animationsEnabled)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    stateDescription = playbackStatus
                },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = previewColors.onButton,
                disabledContainerColor = containerColor,
                disabledContentColor = previewColors.onButton,
            ),
    ) {
        // Keep the label measured and accessible while its visual content is hidden.
        Row(
            modifier = Modifier.alpha(if (isPlaying) 0f else 1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(text = replayLabel, modifier = Modifier.padding(start = SquishSpacing.Compact))
        }
    }
}

@Composable
private fun Modifier.replayShimmer(
    isPlaying: Boolean,
    animationsEnabled: Boolean,
): Modifier {
    if (!isPlaying) return this
    val shimmerPosition =
        if (animationsEnabled) {
            rememberInfiniteTransition(label = "replay shimmer").animateFloat(
                initialValue = -0.5f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                label = "replay shimmer position",
            )
        } else {
            null
        }
    val shape = ButtonDefaults.shape
    val shimmerColor = LocalPreviewColors.current.button
    return drawWithCache {
        val clip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val spacing = 7.dp.toPx()
        val columns = (size.width / spacing).toInt().coerceAtLeast(1)
        val rows = (size.height / spacing).toInt().coerceAtLeast(1)
        val left = (size.width - (columns - 1) * spacing) / 2f
        val top = (size.height - (rows - 1) * spacing) / 2f
        onDrawWithContent {
            drawContent()
            val center = size.width * (shimmerPosition?.value ?: 0.5f)
            val halfWidth = (size.width * 0.35f).coerceAtLeast(1f)
            clipPath(clip) {
                repeat(rows) { row ->
                    repeat(columns) { column ->
                        val dot = Offset(left + column * spacing, top + row * spacing)
                        val sweep = (1f - abs(dot.x - center) / halfWidth).coerceIn(0f, 1f)
                        drawCircle(
                            color = shimmerColor.copy(alpha = 0.08f + 0.67f * sweep),
                            radius = spacing * 0.26f,
                            center = dot,
                        )
                    }
                }
            }
        }
    }
}
