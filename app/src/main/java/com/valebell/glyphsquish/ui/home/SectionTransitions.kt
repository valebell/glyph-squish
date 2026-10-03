package com.valebell.glyphsquish.ui.home

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.LayoutDirection

internal fun sectionTransitionSpec(
    layoutDirection: LayoutDirection,
): AnimatedContentTransitionScope<SquishSection>.() -> ContentTransform =
    {
        val forward = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        val direction =
            if (targetState == SquishSection.MOVES) {
                forward
            } else {
                -forward
            }
        (
            fadeIn(
                animationSpec =
                    tween(
                        durationMillis = SECTION_FADE_IN_MILLIS,
                        delayMillis = SECTION_FADE_IN_DELAY_MILLIS,
                    ),
            ) +
                slideInHorizontally(
                    animationSpec =
                        tween(
                            durationMillis = SECTION_TRANSITION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                ) { width -> direction * width / SECTION_SLIDE_DISTANCE_DIVISOR }
        ).togetherWith(
            fadeOut(
                animationSpec = tween(durationMillis = SECTION_FADE_OUT_MILLIS),
            ) +
                slideOutHorizontally(
                    animationSpec =
                        tween(
                            durationMillis = SECTION_TRANSITION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                ) { width -> -direction * width / SECTION_SLIDE_DISTANCE_DIVISOR },
        ).using(
            SizeTransform(clip = false) { _, _ ->
                tween(
                    durationMillis = SECTION_TRANSITION_MILLIS,
                    easing = FastOutSlowInEasing,
                )
            },
        )
    }

internal val ReducedMotionSectionTransitionSpec:
    AnimatedContentTransitionScope<SquishSection>.() -> ContentTransform = {
        EnterTransition.None togetherWith ExitTransition.None
    }

internal const val SECTION_TRANSITION_MILLIS = 320
internal const val SECTION_FADE_IN_MILLIS = 220
private const val SECTION_FADE_IN_DELAY_MILLIS = 60
internal const val SECTION_FADE_OUT_MILLIS = 140
private const val SECTION_SLIDE_DISTANCE_DIVISOR = 10
