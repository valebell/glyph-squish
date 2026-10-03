package com.valebell.glyphsquish.slime

import kotlin.math.PI

internal const val MATRIX_SIZE = 25
internal const val PIXEL_COUNT = MATRIX_SIZE * MATRIX_SIZE
internal const val SHAPE_SCALE = 1f
internal const val EYE_SCALE = 1f
internal const val FORM_TRANSITION_MILLIS = 1_280
internal const val BURST_ACTION_MILLIS = 1_630
internal const val HELIX_ACTION_MILLIS = 1_330
internal const val ECHO_ACTION_MILLIS = 1_270
internal const val RICOCHET_ACTION_MILLIS = 1_300
internal const val THUNDERBOLT_ACTION_MILLIS = 2_170

internal const val MATRIX_CENTER = MATRIX_SIZE / 2
internal const val MATRIX_RADIUS_SQUARED = 156
internal const val PIXEL_OFF = 0
internal const val ORB_DIAMETER = SlimeFormGeometry.CANONICAL_SIZE
internal const val FORM_TRANSITION_SECONDS = FORM_TRANSITION_MILLIS / 1_000f
internal const val FORM_PARTICLE_COUNT = 80
internal const val MOVEMENT_RADIUS = 7f
internal const val MAX_SPEED = 17f
internal const val BURST_ANTICIPATION_END = 0.13f
internal const val BURST_EXPLOSION_START = 0.4f
internal const val BURST_LOCK_END = 1.4f
internal const val BURST_FAR_LIGHT = 0.65f
internal const val BURST_NEAR_LIGHT = 1f
internal const val HELIX_ACTION_SECONDS = HELIX_ACTION_MILLIS / 1_000f
internal const val ECHO_ACTION_SECONDS = ECHO_ACTION_MILLIS / 1_000f
internal const val ECHO_CROUCH_END = 0.13f
internal const val ECHO_REAR_END = 0.37f
internal const val ECHO_REAR_HOLD_END = 0.47f
internal const val ECHO_RETURN_END = 0.77f
internal const val ECHO_CROUCH_DEPTH = -0.8f
internal const val ECHO_REAR_DEPTH = -4.8f
internal const val ECHO_FRONT_DEPTH = 3f
internal const val ECHO_SURF_DEPTH = 3.8f
internal const val RICOCHET_ACTION_SECONDS = RICOCHET_ACTION_MILLIS / 1_000f
internal const val THUNDERBOLT_PARTICLE_BREAK_START = 0.74f
internal const val THUNDERBOLT_FORM_REFORM_END = 1.86f
internal const val TRAIL_EMISSION_INTERVAL_SECONDS = 0.11f
internal const val GOLDEN_ANGLE = 2.3999631f
internal const val TWO_PI = 2f * PI.toFloat()
