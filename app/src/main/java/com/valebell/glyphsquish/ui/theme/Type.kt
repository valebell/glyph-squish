package com.valebell.glyphsquish.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SquishTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                letterSpacing = (-1.1).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                lineHeight = 32.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

// Android's system family supplies the script-specific fallback fonts. Avoid monospace
// and tracking for scripts with joined letters or combining marks.
internal fun typographyForLanguage(language: String): Typography {
    if (language !in setOf("ar", "ur", "hi", "bn", "zh")) return SquishTypography
    return SquishTypography.copy(
        displaySmall = SquishTypography.displaySmall.copy(letterSpacing = 0.sp),
        labelLarge =
            SquishTypography.labelLarge.copy(
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.sp,
                lineHeight = 24.sp,
            ),
        labelMedium =
            SquishTypography.labelMedium.copy(
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.sp,
                lineHeight = 20.sp,
            ),
        labelSmall =
            SquishTypography.labelSmall.copy(
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.sp,
                lineHeight = 20.sp,
            ),
    )
}
