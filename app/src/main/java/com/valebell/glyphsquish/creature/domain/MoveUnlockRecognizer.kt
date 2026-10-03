package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.min

class MoveUnlockRecognizer {
    private val recentPrimitives = ArrayDeque<TimedPrimitive>()
    private val circleTracker = CircleTracker()
    private var previousPosition: CreatureInteraction.CirclePosition? = null
    private var pendingCardinal: ShellSector? = null
    private var pendingDiagonal: DiagonalEndpoint? = null
    private var pendingImpactAngle: Double? = null
    private var pendingImpactTimeMillis: Long? = null
    private var consumedAngle: Double? = null
    private var lastInputMillis: Long? = null
    private var resetReason: RecognitionReset? = null
    private var completedRecipes = emptySet<SlimeButtonAction>()

    val progress: RecognitionProgress
        get() =
            RecognitionProgress(
                recipes =
                    SlimeButtonAction.entries.associateWith { move ->
                        RecipeProgress(MoveRecipes.steps(move), matchedSteps(move))
                    },
                resetReason = resetReason,
            )

    val expiryMillis: Long?
        get() {
            val progressTime =
                circleTracker.lastProgressTimeMillis
                    ?: recentPrimitives.lastOrNull()?.timeMillis
            return listOfNotNull(lastInputMillis, progressTime, pendingImpactTimeMillis)
                .minOrNull()
                ?.plus(INPUT_TIMEOUT_MILLIS + 1)
        }

    private fun matchedSteps(move: SlimeButtonAction): Int {
        if (move in completedRecipes) return MoveRecipes.steps(move).size
        val values = recentPrimitives.map(TimedPrimitive::value)
        val steps = MoveRecipes.steps(move)
        for (count in min(values.size, steps.size) downTo 1) {
            val suffix = values.takeLast(count)
            if (suffix.map { it.step } != steps.take(count)) continue
            if (count == 2 && move == SlimeButtonAction.ECHO && !suffix.isMatchingBounceRecipe()) continue
            if (count == 2 && move == SlimeButtonAction.RICOCHET && !suffix.isMatchingDiagonalRecipe()) continue
            return count
        }
        return 0
    }

    fun accept(
        interaction: CreatureInteraction,
        nowMillis: Long,
    ): SlimeButtonAction? {
        completedRecipes = emptySet()
        expireOldInput(nowMillis)
        lastInputMillis = nowMillis
        val primitive =
            when (interaction) {
                CreatureInteraction.GlyphButtonPressed -> recognizeGlyphButton()
                is CreatureInteraction.ShellContact -> recognizeContact(interaction, nowMillis)
                is CreatureInteraction.CirclePosition -> recognizePosition(interaction, nowMillis)
            }
        if (primitive != null) {
            recentPrimitives.addLast(TimedPrimitive(primitive, nowMillis))
            while (recentPrimitives.size > MAXIMUM_PRIMITIVES) recentPrimitives.removeFirst()

            resetReason = null

            completedRecipes =
                SlimeButtonAction.entries.filter { matchedSteps(it) == MoveRecipes.steps(it).size }.toSet()
        }
        val recognized = completedRecipes.firstOrNull { it != SlimeButtonAction.BURST }
        if (completedRecipes.any { it != SlimeButtonAction.BURST }) clearInput()
        return recognized
    }

    fun reset(reason: RecognitionReset? = RecognitionReset.INTERRUPTED) {
        if (lastInputMillis != null || completedRecipes.isNotEmpty()) {
            resetReason = reason
        }
        if (reason == null) resetReason = null

        completedRecipes = emptySet()
        clearInput()
    }

    private fun clearInput() {
        recentPrimitives.clear()
        circleTracker.reset()
        previousPosition = null
        consumedAngle = null
        lastInputMillis = null
        clearIncompleteDiscrete()
    }

    private fun recognizeContact(
        contact: CreatureInteraction.ShellContact,
        nowMillis: Long,
    ): MotionPrimitive? {
        val angle = circleAngle(contact)
        if (angle == null) {
            reset(RecognitionReset.WEAK_OR_DEPTH)
            return null
        }
        if (!contact.canTrackCircle()) {
            // Weak impacts without a reliable position pause the trace. Actual boundary
            // positions remain usable while sliding, regardless of the physics timestep.
            clearIncompleteDiscrete()
            clearHistoryUnlessThunderboltPrefix()
            return null
        }
        if (isConsumedEndpoint(angle)) return null

        recognizeDiscreteJump(contact, angle)?.let { discrete ->
            circleTracker.reset()
            consumedAngle = angle
            clearIncompleteDiscrete()
            return discrete
        }
        val circleUpdate = circleTracker.advance(angle, nowMillis)
        if (circleUpdate == CircleUpdate.COMPLETED) {
            return completedCircle()
        }

        if (contact.strength < MINIMUM_CONTACT_STRENGTH) {
            if (!isSettlingAtPendingImpact(contact, angle)) {
                clearIncompleteDiscrete()
                clearHistoryUnlessThunderboltPrefix()
            }
            if (circleUpdate == CircleUpdate.INVALID) {
                restartContactCircle(contact, angle, nowMillis)
            }
            return null
        }

        return when (circleUpdate) {
            CircleUpdate.STARTED -> {
                rememberDiscreteContact(contact, angle, nowMillis)
                null
            }

            CircleUpdate.TRACKING -> {
                if (circleTracker.isDeveloped) {
                    clearHistoryUnlessThunderboltPrefix()
                    clearIncompleteDiscrete()
                } else {
                    rememberDiscreteContact(contact, angle, nowMillis)
                }
                null
            }

            CircleUpdate.INVALID -> {
                restartContactCircle(contact, angle, nowMillis)
                rememberDiscreteContact(contact, angle, nowMillis)
                null
            }

            CircleUpdate.COMPLETED -> {
                error("Completed circles return before discrete recognition")
            }
        }
    }

    private fun CreatureInteraction.ShellContact.canTrackCircle(): Boolean =
        hasReliablePosition || strength >= MINIMUM_CIRCLE_CONTACT_STRENGTH

    private fun isSettlingAtPendingImpact(
        contact: CreatureInteraction.ShellContact,
        angle: Double,
    ): Boolean {
        val impactAngle = pendingImpactAngle ?: return false
        // Soft rebounds at a known endpoint neither erase nor refresh the firm hit.
        return contact.hasReliablePosition &&
            !circleTracker.isDeveloped &&
            abs(signedAngularDelta(impactAngle, angle)) < DIRECTION_LOCK_RADIANS
    }

    private fun restartContactCircle(
        contact: CreatureInteraction.ShellContact,
        angle: Double,
        nowMillis: Long,
    ) {
        if (contact.hasReliablePosition &&
            recentPrimitives.map(TimedPrimitive::value).takeLast(2).isThunderboltPrefix()
        ) {
            // Boundary positions must allow the same fresh circle as interior positions.
            clearCircularInput(RecognitionReset.DIRECTION_OR_JUMP)
        } else {
            reset(RecognitionReset.DIRECTION_OR_JUMP)
        }
        lastInputMillis = nowMillis
        circleTracker.advance(angle, nowMillis)
    }

    private fun recognizePosition(
        position: CreatureInteraction.CirclePosition,
        nowMillis: Long,
    ): MotionPrimitive? {
        val previous = previousPosition
        previousPosition = position
        val angle = circleAngle(position.x, position.y, position.z)
        if (angle == null ||
            position.planarRadiusSquared() < MINIMUM_CIRCLE_TRACKING_RADIUS * MINIMUM_CIRCLE_TRACKING_RADIUS
        ) {
            clearCircularInput(RecognitionReset.WEAK_OR_DEPTH)
            return null
        }
        if (previous != null && crossesCircleCenter(previous, position)) {
            restartCircle(position, angle, nowMillis)
            return null
        }
        if (!circleTracker.hasTrace &&
            position.planarRadiusSquared() < MINIMUM_CIRCLE_START_RADIUS * MINIMUM_CIRCLE_START_RADIUS
        ) {
            return null
        }
        if (isConsumedEndpoint(angle)) return null
        return when (circleTracker.advance(angle, nowMillis)) {
            CircleUpdate.COMPLETED -> {
                completedCircle()
            }

            CircleUpdate.INVALID -> {
                restartCircle(position, angle, nowMillis)
                null
            }

            CircleUpdate.STARTED, CircleUpdate.TRACKING -> {
                if (circleTracker.isDeveloped) {
                    clearHistoryUnlessThunderboltPrefix()
                    clearIncompleteDiscrete()
                }
                null
            }
        }
    }

    private fun clearCircularInput(reason: RecognitionReset) {
        if (circleTracker.hasTrace) resetReason = reason
        circleTracker.reset()
        previousPosition = null
        recentPrimitives.removeAll { it.value == MotionPrimitive.Circle }
    }

    private fun restartCircle(
        position: CreatureInteraction.CirclePosition,
        angle: Double,
        nowMillis: Long,
    ) {
        clearCircularInput(RecognitionReset.DIRECTION_OR_JUMP)
        previousPosition = position
        if (position.planarRadiusSquared() >= MINIMUM_CIRCLE_START_RADIUS * MINIMUM_CIRCLE_START_RADIUS) {
            circleTracker.advance(angle, nowMillis)
        }
    }

    private fun completedCircle(): MotionPrimitive {
        clearIncompleteDiscrete()
        return MotionPrimitive.Circle
    }

    private fun isConsumedEndpoint(angle: Double): Boolean {
        val consumed = consumedAngle ?: return false
        // A held endpoint is one contact, including position samples between impacts.
        if (abs(signedAngularDelta(consumed, angle)) < DIRECTION_LOCK_RADIANS) return true
        consumedAngle = null
        return false
    }

    private fun recognizeGlyphButton(): MotionPrimitive {
        if (recentPrimitives.map(TimedPrimitive::value).takeLast(2).isThunderboltPrefix()) {
            // Another deliberate press replays the move and starts a fresh final circle.
            recentPrimitives.removeLast()
        } else if (circleTracker.isDeveloped || pendingCardinal != null || pendingDiagonal != null) {
            recentPrimitives.clear()
        }
        circleTracker.reset()
        previousPosition = null
        consumedAngle = null
        clearIncompleteDiscrete()
        return MotionPrimitive.GlyphButton
    }

    private fun recognizeDiscreteJump(
        contact: CreatureInteraction.ShellContact,
        angle: Double,
    ): MotionPrimitive? {
        val previousAngle = pendingImpactAngle ?: return null
        if (contact.strength < MINIMUM_CONTACT_STRENGTH ||
            abs(signedAngularDelta(previousAngle, angle)) <= MAXIMUM_ANGULAR_STEP_RADIANS
        ) {
            return null
        }
        val diagonal = diagonalEndpoint(contact)
        if (diagonal != null) {
            val previous = pendingDiagonal
            if (previous != null && previous.isOpposite(diagonal)) {
                return MotionPrimitive.Diagonal(diagonal.axis)
            }
            return null
        }

        val previous = pendingCardinal
        return if (
            contact.sector in CARDINAL_SECTORS &&
            previous != null &&
            previous.isOpposite(contact.sector)
        ) {
            MotionPrimitive.Bounce(from = previous, to = contact.sector)
        } else {
            null
        }
    }

    private fun rememberDiscreteContact(
        contact: CreatureInteraction.ShellContact,
        angle: Double,
        nowMillis: Long,
    ) {
        pendingImpactAngle = angle
        pendingImpactTimeMillis = nowMillis
        val diagonal = diagonalEndpoint(contact)
        when {
            diagonal != null -> {
                pendingCardinal = null
                pendingDiagonal = diagonal
            }

            contact.sector in CARDINAL_SECTORS -> {
                pendingCardinal = contact.sector
                pendingDiagonal = null
            }

            else -> {
                clearIncompleteDiscrete()
            }
        }
    }

    private fun clearIncompleteDiscrete() {
        pendingCardinal = null
        pendingDiagonal = null
        pendingImpactAngle = null
        pendingImpactTimeMillis = null
    }

    private fun clearHistoryUnlessThunderboltPrefix() {
        val values = recentPrimitives.map(TimedPrimitive::value)
        if (values.any { it != MotionPrimitive.Circle } && !values.takeLast(2).isThunderboltPrefix()) {
            recentPrimitives.clear()
        }
    }

    fun expireOldInput(nowMillis: Long) {
        val lastInput = lastInputMillis ?: return
        val pendingImpactTime = pendingImpactTimeMillis
        if (pendingImpactTime != null && nowMillis - pendingImpactTime > INPUT_TIMEOUT_MILLIS) {
            clearIncompleteDiscrete()
        }
        val expiry = expiryMillis ?: return
        if (nowMillis < lastInput || nowMillis >= expiry) reset(RecognitionReset.TIMED_OUT)
    }

    private fun List<MotionPrimitive>.isThunderboltPrefix(): Boolean =
        size == 2 &&
            get(0) is MotionPrimitive.Bounce &&
            get(1) == MotionPrimitive.GlyphButton

    private fun List<MotionPrimitive>.isMatchingBounceRecipe(): Boolean {
        if (size != 2) return false
        val first = get(0) as? MotionPrimitive.Bounce ?: return false
        val second = get(1) as? MotionPrimitive.Bounce ?: return false
        return first.from == second.from && first.to == second.to
    }

    private fun List<MotionPrimitive>.isMatchingDiagonalRecipe(): Boolean {
        if (size != 2) return false
        val first = get(0) as? MotionPrimitive.Diagonal ?: return false
        val second = get(1) as? MotionPrimitive.Diagonal ?: return false
        return first.axis == second.axis
    }

    private data class TimedPrimitive(
        val value: MotionPrimitive,
        val timeMillis: Long,
    )

    private companion object {
        val CARDINAL_SECTORS =
            listOf(
                ShellSector.LEFT,
                ShellSector.TOP,
                ShellSector.RIGHT,
                ShellSector.BOTTOM,
            )
        const val MAXIMUM_PRIMITIVES = 3
        const val INPUT_TIMEOUT_MILLIS = 4_000L
        const val MINIMUM_CIRCLE_CONTACT_STRENGTH = 0.05f
        const val MINIMUM_CONTACT_STRENGTH = 0.40f
        const val MINIMUM_CIRCLE_START_RADIUS = 0.70f
    }
}
