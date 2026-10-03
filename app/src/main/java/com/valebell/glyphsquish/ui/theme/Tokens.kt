package com.valebell.glyphsquish.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SquishShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )

object SquishSpacing {
    val Screen = 20.dp
    val ScreenWide = 48.dp
    val Section = 28.dp
    val Card = 18.dp
    val Item = 12.dp
    val Compact = 8.dp
    val Tiny = 4.dp
    val ContentMaxWidth = 1_200.dp
    val PhoneContentMaxWidth = 680.dp
}
