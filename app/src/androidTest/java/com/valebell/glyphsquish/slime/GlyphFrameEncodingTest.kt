package com.valebell.glyphsquish.slime

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.valebell.glyphsquish.creature.domain.CreatureForm
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GlyphFrameEncodingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rawEncodingMatchesSdkForEveryGrayLevel() {
        // Includes every 8-bit gray value, including black and white, in one frame.
        val frame = IntArray(25 * 25) { it % 256 }
        val sdk = renderWithSdk(frame)
        assertEquals(4095, sdk.max())
        assertArrayEquals(sdk, GlyphFrameEncoder.encode(frame))
    }

    @Test
    fun everyPhysicalFormMatchesTheSdkWhiteBitmapPath() {
        for (form in CreatureForm.entries) {
            val frame = SlimeEngine(SlimePhenotype(form), shadeBody = false).renderFrame()
            val encoded = GlyphFrameEncoder.encode(frame)
            assertEquals(setOf(0, 4095), encoded.toSet())
            assertArrayEquals(renderWithSdk(frame), encoded)
        }
    }

    private fun renderWithSdk(frame: IntArray): IntArray {
        val colors = IntArray(frame.size) { Color.rgb(frame[it], frame[it], frame[it]) }
        val bitmap = Bitmap.createBitmap(colors, 25, 25, Bitmap.Config.ARGB_8888)
        try {
            val image =
                GlyphMatrixObject
                    .Builder()
                    .setImageSource(bitmap)
                    .setBrightness(255)
                    .setScale(100)
                    .build()
            return GlyphMatrixFrame
                .Builder()
                .addTop(image)
                .build(context)
                .render()
        } finally {
            bitmap.recycle()
        }
    }
}
