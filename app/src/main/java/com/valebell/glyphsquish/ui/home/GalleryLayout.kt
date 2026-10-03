package com.valebell.glyphsquish.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

internal data class GalleryLayout(
    val animateSectionChange: Boolean = false,
    val scrollToPreviewOnSelection: Boolean = false,
    val contentPadding: PaddingValues = PaddingValues(0.dp),
    val leadingContent: (@Composable () -> Unit)? = null,
)
