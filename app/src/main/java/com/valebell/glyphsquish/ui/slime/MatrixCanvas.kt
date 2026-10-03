package com.valebell.glyphsquish.ui.slime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.slime.MATRIX_PIXEL_INDICES
import com.valebell.glyphsquish.slime.SlimeEngine
import com.valebell.glyphsquish.slime.SlimePhenotype
import com.valebell.glyphsquish.ui.theme.SquishPreviewBackground
import com.valebell.glyphsquish.ui.theme.SquishPreviewSurface
import kotlin.math.roundToInt

@Composable
fun FormThumbnail(
    form: CreatureForm,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val frame =
        remember(form) {
            SlimeEngine(SlimePhenotype.DEFAULT.copy(form = form)).renderFrame()
        }
    MatrixCanvas(
        frame = { frame },
        modifier =
            if (contentDescription == null) {
                modifier
            } else {
                modifier.semantics { this.contentDescription = contentDescription }
            },
        drawOffPixels = false,
        showStroke = false,
    )
}

@Composable
internal fun MatrixCanvas(
    frame: () -> IntArray,
    modifier: Modifier,
    drawOffPixels: Boolean,
    showStroke: Boolean,
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val pixels = frame()
        val drawingSize = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val panelRadius = drawingSize * PANEL_RADIUS_FACTOR
        drawCircle(
            color = SquishPreviewBackground,
            radius = panelRadius,
            center = center,
        )
        if (showStroke) {
            drawCircle(
                color = SquishPreviewSurface,
                radius = panelRadius,
                center = center,
                style = Stroke(width = drawingSize * PANEL_STROKE_FACTOR),
            )
        }

        val gridDiameter = panelRadius * 2f * GRID_SCALE
        val cellSize = gridDiameter / SlimeEngine.MATRIX_SIZE
        val gridLeft = center.x - gridDiameter / 2f + cellSize / 2f
        val gridTop = center.y - gridDiameter / 2f + cellSize / 2f
        val pixelRadius = cellSize * PIXEL_RADIUS_FACTOR
        MATRIX_PIXEL_INDICES.forEach { index ->
            val x = index % SlimeEngine.MATRIX_SIZE
            val y = index / SlimeEngine.MATRIX_SIZE
            val brightness = pixels[index]
            if (!drawOffPixels && brightness <= 0) return@forEach
            drawCircle(
                color = pixelColor(brightness),
                radius = pixelRadius,
                center = Offset(gridLeft + x * cellSize, gridTop + y * cellSize),
            )
        }
    }
}

private fun pixelColor(brightness: Int): Color {
    if (brightness <= 0) return OffPixelColor
    val channel =
        (
            OFF_PIXEL_CHANNEL +
                (255 - OFF_PIXEL_CHANNEL) * brightness.coerceIn(0, 255) / 255f
        ).roundToInt().coerceAtLeast(OFF_PIXEL_CHANNEL + 1)
    return Color(channel, channel, channel)
}

private const val PANEL_RADIUS_FACTOR = 0.49f
private const val PANEL_STROKE_FACTOR = 0.006f
private const val GRID_SCALE = 0.92f
private const val PIXEL_RADIUS_FACTOR = 0.31f
private const val OFF_PIXEL_CHANNEL = 0x25
private val OffPixelColor = Color(0xFF252523)
