package com.valebell.glyphsquish.slime

import com.valebell.glyphsquish.creature.domain.CreatureForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SlimeFormGeometryTest {
    @Test
    fun catalogContainsExactlyTheApprovedForms() {
        assertEquals(
            listOf(
                CreatureForm.BLOB,
                CreatureForm.SHARK,
                CreatureForm.CAT,
                CreatureForm.STAR,
                CreatureForm.BUTTERFLY,
            ),
            CreatureForm.entries,
        )
    }

    @Test
    fun canonicalMasksMatchApprovedPixelArt() {
        APPROVED_MASKS.forEach { (form, expectedRows) ->
            assertEquals(form.name, expectedRows, rows(mask(form, CANONICAL_SIZE), CANONICAL_SIZE))
        }
    }

    @Test
    fun everyFormHasFinitePositiveRadialGeometry() {
        CreatureForm.entries.forEach { form ->
            repeat(360) { degree ->
                val angle = degree / 180f * PI.toFloat()
                val radius = SlimeFormGeometry.radiusAt(form, angle)
                assertTrue("$form has an invalid radius at $degree degrees", radius.isFinite())
                assertTrue("$form collapses at $degree degrees", radius > 0.45f)
                assertTrue("$form exceeds its mask bounds", radius <= MAX_MASK_RADIUS)
            }
        }
    }

    @Test
    fun renderedDepthSizesStayDistinctConnectedAndDense() {
        listOf(7, CANONICAL_SIZE, 15).forEach { size ->
            val masks = CreatureForm.entries.associateWith { form -> mask(form, size) }

            assertEquals(
                CreatureForm.entries.size,
                masks.values
                    .map(BooleanArray::contentHashCode)
                    .distinct()
                    .size,
            )
            masks.forEach { (form, body) ->
                assertTrue("$form is too sparse at ${size}px", body.count { it } >= size * size / 3)
                assertTrue("$form is disconnected at ${size}px", isConnected(body, size))
            }
        }
    }

    @Test
    fun nonBlobFormsKeepMostOfTheBlobMass() {
        val masks = CreatureForm.entries.associateWith { form -> mask(form, CANONICAL_SIZE) }
        val blobPixelCount = masks.getValue(CreatureForm.BLOB).count { it }

        CreatureForm.entries
            .filterNot { it == CreatureForm.BLOB }
            .forEach { form ->
                assertTrue(
                    "$form lost too much rounded body mass",
                    masks.getValue(form).count { it } >= blobPixelCount * MIN_BLOB_MASS_RATIO,
                )
            }
    }

    @Test
    fun sharkUsesADorsalRidgeAndForkedSideTail() {
        assertTrue(cell(CreatureForm.SHARK, 4, 0))
        assertTrue(cell(CreatureForm.SHARK, 5, 0))
        assertFalse(cell(CreatureForm.SHARK, 6, 0))
        assertTrue(cell(CreatureForm.SHARK, 10, 4))
        assertFalse(cell(CreatureForm.SHARK, 10, 5))
        assertTrue(cell(CreatureForm.SHARK, 10, 6))
        assertFalse(isBilaterallySymmetric(mask(CreatureForm.SHARK, CANONICAL_SIZE), CANONICAL_SIZE))
    }

    @Test
    fun catHasTwoChunkyEarsAboveARoundedFace() {
        assertTrue(cell(CreatureForm.CAT, 2, 0))
        assertTrue(cell(CreatureForm.CAT, 3, 0))
        assertFalse(cell(CreatureForm.CAT, 5, 0))
        assertTrue(cell(CreatureForm.CAT, 7, 0))
        assertTrue(cell(CreatureForm.CAT, 8, 0))
        assertTrue(cell(CreatureForm.CAT, 0, 5))
        assertTrue(cell(CreatureForm.CAT, 10, 5))
    }

    @Test
    fun starHasFiveBroadRaysAroundADenseFace() {
        assertTrue(cell(CreatureForm.STAR, 4, 0))
        assertTrue(cell(CreatureForm.STAR, 5, 0))
        assertTrue(cell(CreatureForm.STAR, 6, 0))
        assertTrue(cell(CreatureForm.STAR, 0, 4))
        assertTrue(cell(CreatureForm.STAR, 10, 4))
        assertTrue(cell(CreatureForm.STAR, 2, 10))
        assertTrue(cell(CreatureForm.STAR, 8, 10))
        assertFalse(cell(CreatureForm.STAR, 5, 10))
        assertTrue(cell(CreatureForm.STAR, 5, 5))
    }

    @Test
    fun butterflyHasFourPairedWingsAroundAWideFace() {
        assertTrue(cell(CreatureForm.BUTTERFLY, 2, 0))
        assertTrue(cell(CreatureForm.BUTTERFLY, 8, 0))
        assertFalse(cell(CreatureForm.BUTTERFLY, 5, 0))
        assertTrue(cell(CreatureForm.BUTTERFLY, 0, 2))
        assertTrue(cell(CreatureForm.BUTTERFLY, 10, 2))
        assertTrue(cell(CreatureForm.BUTTERFLY, 0, 8))
        assertTrue(cell(CreatureForm.BUTTERFLY, 10, 8))
        assertTrue(cell(CreatureForm.BUTTERFLY, 2, 10))
        assertTrue(cell(CreatureForm.BUTTERFLY, 8, 10))
        assertFalse(cell(CreatureForm.BUTTERFLY, 5, 10))
    }

    @Test
    fun everyFormFramesItsRestingEyesWithBodyPixels() {
        CreatureForm.entries.forEach { form ->
            val radius = CANONICAL_SIZE / 2f
            val center = CANONICAL_SIZE / 2
            val eyeY =
                center +
                    (radius * SlimeFormGeometry.eyeOffset(form)).roundToInt()
            val spacing =
                (radius * SlimeFormGeometry.eyeSpacing(form))
                    .roundToInt()

            listOf(center - spacing, center + spacing).forEachIndexed { index, eyeX ->
                assertTrue("$form clips eye $index", cell(form, eyeX, eyeY))
                assertTrue("$form clips eye $index", cell(form, eyeX, eyeY + 1))
                assertTrue("$form has no forehead above eye $index", cell(form, eyeX, eyeY - 1))
                assertTrue("$form has no lower face below eye $index", cell(form, eyeX, eyeY + 2))
                val outerCheekX = eyeX + if (index == 0) -1 else 1
                assertTrue("$form has no outer cheek beside eye $index", cell(form, outerCheekX, eyeY))
            }
        }
    }

    @Test
    fun canonicalParticlesStayWithinEveryTargetForm() {
        CreatureForm.entries.forEach { form ->
            repeat(PARTICLE_COUNT) { index ->
                val point = SlimeFormGeometry.particle(form, index, PARTICLE_COUNT)
                assertTrue(
                    "$form particle $index left the silhouette",
                    SlimeFormGeometry.contains(form, point.x, point.y),
                )
                assertTrue(
                    "$form particle $index left the mask bounds",
                    sqrt(point.x * point.x + point.y * point.y) <= MAX_MASK_RADIUS,
                )
            }
        }
    }

    private fun cell(
        form: CreatureForm,
        x: Int,
        y: Int,
    ): Boolean = mask(form, CANONICAL_SIZE)[y * CANONICAL_SIZE + x]

    private fun mask(
        form: CreatureForm,
        size: Int,
    ): BooleanArray {
        val radius = size / 2f
        val center = (size - 1) / 2f
        return BooleanArray(size * size) { index ->
            val x = (index % size - center) / radius
            val y = (index / size - center) / radius
            SlimeFormGeometry.contains(form, x, y)
        }
    }

    private fun rows(
        mask: BooleanArray,
        size: Int,
    ): List<String> =
        List(size) { y ->
            buildString(size) {
                repeat(size) { x ->
                    append(if (mask[y * size + x]) '#' else '.')
                }
            }
        }

    private fun isConnected(
        mask: BooleanArray,
        size: Int,
    ): Boolean {
        val start = mask.indexOfFirst { it }
        if (start < 0) return false
        val visited = BooleanArray(mask.size)
        val queue = ArrayDeque<Int>()
        queue += start
        visited[start] = true
        var count = 0
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            count += 1
            val x = index % size
            val y = index / size
            listOf(
                x - 1 to y,
                x + 1 to y,
                x to y - 1,
                x to y + 1,
            ).forEach { (neighborX, neighborY) ->
                if (neighborX !in 0 until size || neighborY !in 0 until size) return@forEach
                val neighbor = neighborY * size + neighborX
                if (mask[neighbor] && !visited[neighbor]) {
                    visited[neighbor] = true
                    queue += neighbor
                }
            }
        }
        return count == mask.count { it }
    }

    private fun isBilaterallySymmetric(
        mask: BooleanArray,
        size: Int,
    ): Boolean =
        mask.indices.all { index ->
            val x = index % size
            val y = index / size
            mask[index] == mask[y * size + size - 1 - x]
        }

    private companion object {
        const val CANONICAL_SIZE = SlimeFormGeometry.CANONICAL_SIZE
        const val PARTICLE_COUNT = 80
        const val MAX_MASK_RADIUS = 1.42f
        const val MIN_BLOB_MASS_RATIO = 0.75f

        val APPROVED_MASKS =
            mapOf(
                CreatureForm.BLOB to
                    listOf(
                        "...####....",
                        "..#######..",
                        ".#########.",
                        ".#########.",
                        "###########",
                        "###########",
                        "###########",
                        "###########",
                        ".#########.",
                        "..#######..",
                        "....###....",
                    ),
                CreatureForm.SHARK to
                    listOf(
                        "....##.....",
                        "...###.....",
                        "..#######..",
                        ".########..",
                        "###########",
                        "#########..",
                        "###########",
                        ".########..",
                        "..#######..",
                        "...#####...",
                        "...........",
                    ),
                CreatureForm.CAT to
                    listOf(
                        "..##...##..",
                        "..###.###..",
                        ".#########.",
                        ".#########.",
                        "###########",
                        "###########",
                        "###########",
                        ".#########.",
                        ".#########.",
                        "..#######..",
                        "...#####...",
                    ),
                CreatureForm.STAR to
                    listOf(
                        "....###....",
                        "....###....",
                        "...#####...",
                        ".#########.",
                        "###########",
                        "###########",
                        ".#########.",
                        "..#######..",
                        ".#########.",
                        ".###...###.",
                        "..##...##..",
                    ),
                CreatureForm.BUTTERFLY to
                    listOf(
                        "..##...##..",
                        ".####.####.",
                        "###########",
                        ".#########.",
                        "..#######..",
                        "..#######..",
                        "..#######..",
                        ".#########.",
                        "###########",
                        ".####.####.",
                        "..##...##..",
                    ),
            )
    }
}
