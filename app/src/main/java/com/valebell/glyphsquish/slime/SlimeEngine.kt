package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.ShellSector
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Coordinates input, simulation and rendering. Call from a single thread. */
class SlimeEngine(
    initialPhenotype: SlimePhenotype = SlimePhenotype.DEFAULT,
    shadeBody: Boolean = true,
    interactionHandler: (CreatureInteraction) -> SlimeButtonAction? = { null },
    private val onMovePlaybackChanged: (Boolean) -> Unit = {},
) {
    private val scene = SlimeScene(initialPhenotype, shadeBody, interactionHandler)
    private val motion = SlimeMotion(scene)
    private val trail = SlimeTrail(scene)
    private val burst = SlimeBurst(scene)
    private val thunderbolt = SlimeThunderbolt(scene)
    private val orbitMoves = SlimeOrbitMoves(scene)
    private val renderer = SlimeRenderer(scene)
    private var reportedMovePlaying = false

    /** Includes moves waiting for a form transition, so playback UI stays busy until completion. */
    val isMovePlaying: Boolean
        get() = scene.activeButtonAction != null || scene.pendingButtonAction != null

    fun setPhenotype(
        newPhenotype: SlimePhenotype,
        animateFormChange: Boolean = false,
        forceReform: Boolean = false,
    ) = updateMovePlayback { scene.setPhenotype(newPhenotype, animateFormChange, forceReform) }

    fun snapToPhenotype(newPhenotype: SlimePhenotype) = updateMovePlayback { scene.snapToPhenotype(newPhenotype) }

    fun triggerSemanticAnimation(
        cue: SlimeAnimationCue,
        sector: ShellSector = ShellSector.RIGHT,
    ) = scene.triggerSemanticAnimation(cue, sector)

    fun simulateInteraction(interaction: CreatureInteraction) =
        updateMovePlayback { scene.simulateInteraction(interaction) }

    fun setTilt(
        x: Float,
        y: Float,
        z: Float = 0f,
    ) = scene.setTilt(x, y, z)

    fun setPhoneAcceleration(
        x: Float,
        y: Float,
        z: Float,
    ) = scene.setPhoneAcceleration(x, y, z)

    fun setPressed(pressed: Boolean) = scene.setPressed(pressed)

    fun cancelPressed() = scene.cancelPressed()

    fun handleGlyphButtonPress(resolveAction: () -> SlimeButtonAction?) =
        updateMovePlayback { scene.handleGlyphButtonPress(resolveAction) }

    fun triggerButtonAction(action: SlimeButtonAction) = updateMovePlayback { scene.triggerButtonAction(action) }

    /** Cap pause catch-up and integrate at most 1/120 s per step to stabilize contacts. */
    fun step(deltaSeconds: Float) {
        if (!deltaSeconds.isFinite() || deltaSeconds <= 0f) return
        val elapsed = deltaSeconds.coerceAtMost(MAX_STEP_SECONDS)
        val count = ceil(elapsed / MAX_PHYSICS_STEP_SECONDS).toInt()
        val delta = elapsed / count
        // Resume recognition before the next substep can emit physical motion.
        repeat(count) { updateMovePlayback { scene.step(delta) } }
    }

    fun snapshot(): SlimeSnapshot = scene.snapshot()

    fun renderFrame(): IntArray = renderer.render()

    private inline fun updateMovePlayback(update: () -> Unit) {
        update()
        val playing = isMovePlaying
        if (playing != reportedMovePlaying) {
            reportedMovePlaying = playing
            onMovePlaybackChanged(playing)
        }
    }

    private fun SlimeScene.setPhenotype(
        newPhenotype: SlimePhenotype,
        animateFormChange: Boolean = false,
        forceReform: Boolean = false,
    ) {
        val runningTransition = formTransition
        if (
            runningTransition != null &&
            !animateFormChange &&
            runningTransition.target.form == newPhenotype.form
        ) {
            runningTransition.target = newPhenotype
            phenotype = newPhenotype
            return
        }

        val formChanged = phenotype.form != newPhenotype.form
        val shouldAnimate =
            animateFormChange &&
                (forceReform || formChanged)
        if (!shouldAnimate) {
            phenotype = newPhenotype
            if (formChanged || runningTransition != null) {
                cancelTransientBodyAnimation()
                formTransition = null
                setExpression(SlimeExpression.CURIOUS, 0f)
                startPendingButtonAction()
            }
            return
        }

        cancelTransientBodyAnimation()
        formTransition =
            ActiveFormTransition(
                source = phenotype,
                target = newPhenotype,
                originX = positionX,
                originY = positionY,
                originZ = positionZ,
            )
        phenotype = newPhenotype
        setExpression(SlimeExpression.FOCUSED, FORM_TRANSITION_SECONDS)
    }

    private fun SlimeScene.snapToPhenotype(newPhenotype: SlimePhenotype) {
        phenotype = newPhenotype
        formTransition = null
        pendingButtonAction = null
        cancelTransientBodyAnimation()
        positionX = 0f
        positionY = 0f
        positionZ = 0f
        isPressed = false
        pressAmount = 0f
        setExpression(SlimeExpression.CURIOUS, 0f)
    }

    private fun SlimeScene.triggerSemanticAnimation(
        cue: SlimeAnimationCue,
        sector: ShellSector = ShellSector.RIGHT,
    ) {
        animationCoordinator.trigger(cue, sector)
    }

    private fun SlimeScene.simulateInteraction(interaction: CreatureInteraction) {
        emitInteraction(interaction)
    }

    private fun SlimeScene.setTilt(
        x: Float,
        y: Float,
        z: Float = 0f,
    ) {
        tiltX = applyDeadZone(x, TILT_DEAD_ZONE)
        tiltY = applyDeadZone(y, TILT_DEAD_ZONE)
        tiltZ = applyDeadZone(z, TILT_DEAD_ZONE)
    }

    private fun SlimeScene.setPhoneAcceleration(
        x: Float,
        y: Float,
        z: Float,
    ) {
        phoneAccelerationX =
            applyDeadZone(x, PHONE_ACCELERATION_DEAD_ZONE)
        phoneAccelerationY =
            applyDeadZone(y, PHONE_ACCELERATION_DEAD_ZONE)
        phoneAccelerationZ =
            applyDeadZone(z, PHONE_ACCELERATION_DEAD_ZONE)
    }

    private fun SlimeScene.setPressed(pressed: Boolean) {
        if (isPressed == pressed) return

        isPressed = pressed
        if (!pressed) {
            velocityY = max(velocityY - RELEASE_HOP_SPEED, -MAX_SPEED)
            velocityZ = min(velocityZ + RELEASE_DEPTH_SPEED, MAX_SPEED)
            impactAmount = max(impactAmount, 0.45f)
            impactNormalX = 0f
            impactNormalY = 1f
            impactNormalZ = 0f
        }
    }

    private fun SlimeScene.cancelPressed() {
        isPressed = false
        pressAmount = 0f
    }

    private fun SlimeScene.handleGlyphButtonPress(resolveAction: () -> SlimeButtonAction?) {
        if (activeButtonAction != null || pendingButtonAction != null) return
        cancelPressed()
        // A form change can finish before playing this press; don't discard it mid-morph.
        resolveAction()?.let { triggerButtonAction(it) }
    }

    private fun SlimeScene.triggerButtonAction(action: SlimeButtonAction) {
        if (formTransition != null) {
            pendingButtonAction = action
            return
        }
        startButtonAction(action)
    }

    private fun SlimeScene.startButtonAction(action: SlimeButtonAction) {
        if (isPressed) {
            isPressed = false
        }
        activeButtonAction = action
        buttonActionElapsed = 0f
        buttonActionRemaining = actionDuration(action)
        buttonActionPhase = -1
        actionOriginX = positionX
        actionOriginY = positionY
        actionOriginZ = positionZ
        buttonActionDirection = if (tiltX + tiltY < 0f) -1f else 1f
        velocityX = 0f
        velocityY = 0f
        velocityZ = 0f
        burstPixels.clear()
        thunderboltParticles.clear()

        when (action) {
            SlimeButtonAction.BURST -> {
                trailParticles.clear()
                burst.prepare()
            }

            SlimeButtonAction.THUNDERBOLT -> {
                thunderbolt.prepare()
            }

            SlimeButtonAction.HELIX,
            SlimeButtonAction.ECHO,
            SlimeButtonAction.RICOCHET,
            -> {
                // Orbit moves use the shared state above and need no particle preparation.
            }
        }
        setActionPhase(0, SlimeExpression.FOCUSED)
    }

    private fun SlimeScene.step(delta: Float) {
        elapsedSeconds += delta
        animationCoordinator.step(delta)
        if (updateFormTransition(delta)) return
        if (activeButtonAction == null && pendingButtonAction != null) {
            startPendingButtonAction()
        }
        pressAmount =
            approach(
                current = pressAmount,
                target = if (isPressed) 1f else 0f,
                amount = delta * if (isPressed) PRESS_IN_SPEED else PRESS_OUT_SPEED,
            )
        updateExpression(delta)
        impactAmount = max(0f, impactAmount - delta * IMPACT_DECAY)
        val runningAction = activeButtonAction
        if (runningAction != null) {
            updateButtonAction(delta)
            if (runningAction != SlimeButtonAction.BURST) {
                trail.step(delta)
            }
            return
        }

        motion.step(delta)
        trail.step(delta)
    }

    private fun SlimeScene.snapshot(): SlimeSnapshot =
        SlimeSnapshot(
            motion = SlimeMotionSnapshot(positionX, positionY, positionZ, velocityX, velocityY, velocityZ),
            appearance = SlimeAppearanceSnapshot(expression, phenotype.form, formTransition != null),
            activeButtonAction = activeButtonAction,
            activeSemanticAnimation = animationCoordinator.snapshot().activeCue,
            isPressed = isPressed,
            trailCount = trailParticles.size,
        )

    private fun SlimeScene.updateFormTransition(delta: Float): Boolean {
        val transition = formTransition ?: return false
        transition.elapsedSeconds =
            min(transition.elapsedSeconds + delta, FORM_TRANSITION_SECONDS)
        val centerProgress =
            smoothSegment(transition.elapsedSeconds, 0f, FORM_CENTERING_END_SECONDS)
        positionX = lerp(transition.originX, 0f, centerProgress)
        positionY = lerp(transition.originY, 0f, centerProgress)
        positionZ = lerp(transition.originZ, 0f, centerProgress)
        updateExpression(delta)

        if (transition.elapsedSeconds < FORM_TRANSITION_SECONDS) return true

        formTransition = null
        positionX = 0f
        positionY = 0f
        positionZ = 0f
        velocityX = 0f
        velocityY = 0f
        velocityZ = 0f
        setExpression(SlimeExpression.HAPPY, FORM_SETTLE_EXPRESSION_SECONDS)
        startPendingButtonAction()
        return true
    }

    private fun SlimeScene.startPendingButtonAction() {
        pendingButtonAction?.let { action ->
            pendingButtonAction = null
            startButtonAction(action)
        }
    }

    private fun SlimeScene.updateExpression(delta: Float) {
        if (expressionRemaining <= 0f) return

        expressionRemaining = max(0f, expressionRemaining - delta)
        if (expressionRemaining == 0f) {
            expression = SlimeExpression.CURIOUS
        }
    }

    private fun SlimeScene.updateButtonAction(delta: Float) {
        val action = activeButtonAction ?: return
        val previousX = positionX
        val previousY = positionY
        val previousZ = positionZ
        val duration = actionDuration(action)
        buttonActionElapsed = min(buttonActionElapsed + delta, duration)
        buttonActionRemaining = max(0f, duration - buttonActionElapsed)

        when (action) {
            SlimeButtonAction.BURST -> burst.step()
            SlimeButtonAction.HELIX -> orbitMoves.helix()
            SlimeButtonAction.ECHO -> orbitMoves.echo()
            SlimeButtonAction.RICOCHET -> orbitMoves.ricochet()
            SlimeButtonAction.THUNDERBOLT -> thunderbolt.step()
        }

        velocityX = (positionX - previousX) / delta
        velocityY = (positionY - previousY) / delta
        velocityZ = (positionZ - previousZ) / delta
        limitVelocity()

        if (buttonActionRemaining == 0f) {
            activeButtonAction = null
            buttonActionPhase = -1
            burstPixels.clear()
            thunderboltParticles.clear()
            velocityX *= ACTION_EXIT_VELOCITY
            velocityY *= ACTION_EXIT_VELOCITY
            velocityZ *= ACTION_EXIT_VELOCITY
            setExpression(SlimeExpression.HAPPY, ACTION_SETTLE_EXPRESSION_SECONDS)
        }
    }

    companion object {
        const val MATRIX_SIZE = com.valebell.glyphsquish.slime.MATRIX_SIZE
        const val PIXEL_COUNT = com.valebell.glyphsquish.slime.PIXEL_COUNT
        const val SHAPE_SCALE = com.valebell.glyphsquish.slime.SHAPE_SCALE
        const val EYE_SCALE = com.valebell.glyphsquish.slime.EYE_SCALE
        internal const val FORM_TRANSITION_MILLIS = com.valebell.glyphsquish.slime.FORM_TRANSITION_MILLIS
        internal const val BURST_ACTION_MILLIS = com.valebell.glyphsquish.slime.BURST_ACTION_MILLIS
        internal const val HELIX_ACTION_MILLIS = com.valebell.glyphsquish.slime.HELIX_ACTION_MILLIS
        internal const val ECHO_ACTION_MILLIS = com.valebell.glyphsquish.slime.ECHO_ACTION_MILLIS
        internal const val RICOCHET_ACTION_MILLIS = com.valebell.glyphsquish.slime.RICOCHET_ACTION_MILLIS
        internal const val THUNDERBOLT_ACTION_MILLIS = com.valebell.glyphsquish.slime.THUNDERBOLT_ACTION_MILLIS
    }
}

private const val FORM_CENTERING_END_SECONDS = 0.22f
private const val FORM_SETTLE_EXPRESSION_SECONDS = 0.45f
private const val PHONE_ACCELERATION_DEAD_ZONE = 0.025f
private const val MAX_STEP_SECONDS = 0.1f
private const val RELEASE_HOP_SPEED = 5f
private const val RELEASE_DEPTH_SPEED = 5.5f
private const val ACTION_EXIT_VELOCITY = 0.18f
private const val ACTION_SETTLE_EXPRESSION_SECONDS = 0.35f
private const val PRESS_IN_SPEED = 9f
private const val PRESS_OUT_SPEED = 6f
private const val IMPACT_DECAY = 2.7f
private const val TILT_DEAD_ZONE = 0.07f
private const val MAX_PHYSICS_STEP_SECONDS = 1f / 120f
