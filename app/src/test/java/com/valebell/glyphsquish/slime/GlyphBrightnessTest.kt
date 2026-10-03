package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphBrightnessTest {
    @Test
    fun everyPhysicalFormUsesFullWhiteAndPreservesSilhouetteAndEyes() {
        for (form in CreatureForm.entries) {
            val phenotype = SlimePhenotype(form)
            val preview = SlimeEngine(initialPhenotype = phenotype).renderFrame()
            val physical = SlimeEngine(initialPhenotype = phenotype, shadeBody = false).renderFrame()
            assertEquals(setOf(0, 255), physical.toSet())
            assertEquals(preview.map { it > 0 }, physical.map { it > 0 })
            assertTrue(preview.any { it in 1..254 })
        }
    }

    @Test
    fun depthDoesNotDimThePhysicalBodyAndTrailsStillFade() {
        for (depth in listOf(-1f, 1f)) {
            val preview = SlimeEngine()
            val physical = SlimeEngine(shadeBody = false)
            for (engine in listOf(preview, physical)) {
                engine.setTilt(0.6f, 0f, depth)
                repeat(12) { engine.step(1f / 12f) }
            }
            val previewFrame = preview.renderFrame()
            val physicalFrame = physical.renderFrame()
            assertEquals(previewFrame.map { it > 0 }, physicalFrame.map { it > 0 })
            for (index in previewFrame.indices) {
                if (previewFrame[index] >= 170) assertEquals(255, physicalFrame[index])
                if (previewFrame[index] in 1..132) assertEquals(previewFrame[index], physicalFrame[index])
            }
            assertTrue(physicalFrame.any { it == 255 })
            assertTrue(physicalFrame.any { it in 1..132 })
        }
    }

    @Test
    fun formMorphStillFadesThenSettlesAtFullBrightness() {
        val engine = SlimeEngine(shadeBody = false)
        engine.setPhenotype(SlimePhenotype(CreatureForm.CAT), animateFormChange = true)
        var sawFade = false
        repeat(120) {
            engine.step(1f / 60f)
            if (engine.renderFrame().any { it in 1..254 }) sawFade = true
        }
        assertTrue(sawFade)
        assertEquals(setOf(0, 255), engine.renderFrame().toSet())
    }
}
