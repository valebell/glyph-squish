package com.valebell.glyphsquish.ui.slime

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.slime.SlimePhenotype
import com.valebell.glyphsquish.ui.theme.SquishTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SlimeMatrixTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun engineUpdatesRedrawTheCanvasWithoutChangingItsParameters() {
        lateinit var state: SlimeMatrixState
        composeRule.setContent {
            state = rememberSlimeMatrixState()
            SquishTheme {
                SlimeMatrix(
                    state = state,
                    phenotype = SlimePhenotype.DEFAULT,
                    contentDescription = "Test matrix",
                    animation = MatrixAnimation(running = false),
                    modifier = Modifier.size(240.dp),
                )
            }
        }
        val matrix = composeRule.onNodeWithContentDescription("Test matrix")
        val before = matrix.captureToImage().asAndroidBitmap()

        composeRule.runOnIdle {
            state.setPressed(true)
            repeat(10) { state.step(0.033f) }
        }

        val after = matrix.captureToImage().asAndroidBitmap()
        assertFalse("Frame changes must invalidate drawing", before.sameAs(after))
    }
}
