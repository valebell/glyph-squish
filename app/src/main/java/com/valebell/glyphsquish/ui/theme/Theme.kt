package com.valebell.glyphsquish.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
    lightColorScheme(
        primary = SquishLightOnBackground,
        onPrimary = SquishLightBackground,
        primaryContainer = SquishLightSurfaceVariant,
        onPrimaryContainer = SquishLightOnBackground,
        secondary = SquishLightRed,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDAD9),
        onSecondaryContainer = Color(0xFF410005),
        background = SquishLightBackground,
        onBackground = SquishLightOnBackground,
        surface = SquishLightSurface,
        onSurface = SquishLightOnBackground,
        surfaceVariant = SquishLightSurfaceVariant,
        onSurfaceVariant = SquishLightOnSurfaceVariant,
        outline = SquishLightOutline,
        error = SquishLightRed,
        onError = Color.White,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = SquishDarkOnBackground,
        onPrimary = SquishDarkBackground,
        primaryContainer = SquishDarkSurfaceVariant,
        onPrimaryContainer = SquishDarkOnBackground,
        secondary = SquishDarkRed,
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF7D2930),
        onSecondaryContainer = Color(0xFFFFDAD9),
        background = SquishDarkBackground,
        onBackground = SquishDarkOnBackground,
        surface = SquishDarkSurface,
        onSurface = SquishDarkOnBackground,
        surfaceVariant = SquishDarkSurfaceVariant,
        onSurfaceVariant = SquishDarkOnSurfaceVariant,
        outline = SquishDarkOutline,
        error = SquishDarkRed,
        onError = Color.Black,
    )

@Composable
fun SquishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDeviceColors: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        when {
            useDeviceColors && darkTheme -> dynamicDarkColorScheme(context)
            useDeviceColors -> dynamicLightColorScheme(context)
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    val previewColors =
        if (useDeviceColors) {
            PreviewColors(
                container = colorScheme.surfaceContainer,
                content = colorScheme.onSurface,
                muted = colorScheme.onSurfaceVariant,
                accent = colorScheme.primary,
                border = colorScheme.outlineVariant,
                button = colorScheme.primary,
                onButton = colorScheme.onPrimary,
            )
        } else {
            PreviewColors()
        }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyForLanguage(LocalConfiguration.current.locales[0].language),
        shapes = SquishShapes,
    ) {
        CompositionLocalProvider(LocalPreviewColors provides previewColors, content = content)
    }
}
