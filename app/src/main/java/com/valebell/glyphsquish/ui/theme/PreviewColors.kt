package com.valebell.glyphsquish.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The preview card follows the theme; the physical matrix keeps its own monochrome colors. */
@Immutable
internal data class PreviewColors(
    val container: Color = SquishPreviewBackground,
    val content: Color = SquishPreviewContent,
    val muted: Color = SquishPreviewMuted,
    val accent: Color = SquishDarkRed,
    val border: Color = SquishPreviewSurface,
    val button: Color = SquishPreviewContent,
    val onButton: Color = SquishPreviewBackground,
)

internal val LocalPreviewColors = staticCompositionLocalOf { PreviewColors() }
