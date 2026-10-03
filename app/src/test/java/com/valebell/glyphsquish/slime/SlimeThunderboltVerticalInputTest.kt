package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureInteraction
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.domain.CreatureSession
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SlimeThunderboltVerticalInputTest {
    @Test
    fun downThenUpAndSelectedMoveAllowsClockwiseOrCounterclockwiseCircle() {
        val failures = mutableListOf<String>()
        for (form in listOf(CreatureForm.BLOB, CreatureForm.SHARK, CreatureForm.CAT, CreatureForm.BUTTERFLY)) {
            failures += verifyForm(form)
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun verifyForm(form: CreatureForm): List<String> {
        val failures = mutableListOf<String>()
        for (frameRate in listOf(20, 30, 120)) {
            for (direction in listOf(-1, 1)) {
                for (selectedMove in SlimeButtonAction.entries - SlimeButtonAction.THUNDERBOLT) {
                    for (firstEdgeSeconds in listOf(0.7, 1.5)) {
                        val glyph = VerticalGlyph(frameRate, form, selectedMove)
                        glyph.attempt(firstEdgeSeconds, direction)?.let {
                            failures +=
                                "$form, $selectedMove, $frameRate fps, direction $direction, $firstEdgeSeconds: $it"
                        }
                    }
                }
            }
        }
        return failures
    }

    private class VerticalGlyph(
        private val frameRate: Int,
        form: CreatureForm,
        selectedMove: SlimeButtonAction,
    ) {
        private val session =
            CreatureSession(
                CreatureProgression.initialState().copy(
                    currentForm = form,
                    selectedMove = selectedMove,
                    unlockedMoves = SlimeButtonAction.entries.toSet() - SlimeButtonAction.THUNDERBOLT,
                ),
            )
        private var nowSeconds = 0.0
        private val progressTrace = mutableListOf<String>()
        private var previousSteps = 0
        private val engine =
            SlimeEngine(
                initialPhenotype = SlimePhenotype.DEFAULT.copy(form = form),
                interactionHandler = ::handle,
                onMovePlaybackChanged = { active ->
                    session.setRecognitionPaused(active, (nowSeconds * 1_000).toLong())
                },
            )
        private val completedSteps: Int
            get() =
                session.recognition.recipes
                    .getValue(SlimeButtonAction.THUNDERBOLT)
                    .completedSteps

        fun attempt(
            firstEdgeSeconds: Double,
            direction: Int,
        ): String? {
            engine.setTilt(0f, 0.8f, 0.6f)
            advance(firstEdgeSeconds)
            engine.setTilt(0f, -0.8f, 0.6f)
            repeat(frameRate * 3) {
                if (completedSteps != 1) advance(1.0 / frameRate)
            }
            if (completedSteps != 1) return "Opposite impacts did not register a bounce: $progressTrace"
            advance(0.3)
            engine.setPressed(true)
            advance(1.0)
            engine.handleGlyphButtonPress {
                handle(CreatureInteraction.GlyphButtonPressed)
                session.state.selectedMove
            }
            engine.setPressed(false)
            if (completedSteps != 2) return "Holding the button lost the bounce: $progressTrace"
            if (engine.snapshot().activeButtonAction != session.state.selectedMove) return "Selected move did not play"
            repeat(frameRate * 3) {
                if (engine.isMovePlaying) advance(1.0 / frameRate)
            }
            advance(0.3)
            advance(7.5) { time ->
                val angle = -PI / 2 + direction * time * 2 * PI / 6.0
                engine.setTilt(0.8f * cos(angle).toFloat(), 0.8f * sin(angle).toFloat(), 0.6f)
            }
            return if (SlimeButtonAction.THUNDERBOLT in session.state.unlockedMoves) {
                null
            } else {
                "Physical circle did not unlock Thunderbolt: $progressTrace"
            }
        }

        private fun handle(interaction: CreatureInteraction): SlimeButtonAction? {
            val outcome = session.handle(interaction, (nowSeconds * 1_000).toLong())
            if (completedSteps != previousSteps) {
                progressTrace += "${(nowSeconds * 1_000).toLong()}: $previousSteps->$completedSteps $interaction"
                previousSteps = completedSteps
            }
            return outcome.unlockedMove
        }

        private fun advance(
            seconds: Double,
            updateTilt: (Double) -> Unit = {},
        ) {
            repeat((seconds * frameRate).toInt()) { frame ->
                updateTilt(frame.toDouble() / frameRate)
                nowSeconds += 1.0 / frameRate
                engine.step(1f / frameRate)
            }
        }
    }
}
