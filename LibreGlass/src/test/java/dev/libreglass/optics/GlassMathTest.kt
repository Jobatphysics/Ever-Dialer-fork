package dev.libreglass.optics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassMathTest {
    @Test fun roundedRectSdf_isNegativeInside_zeroOnEdge_positiveOutside() {
        val half = GlassMath.Vec2(10f, 8f)
        assertTrue(GlassMath.roundedRectSdf(GlassMath.Vec2(0f, 0f), half, 3f) < 0f)
        assertEquals(0f, GlassMath.roundedRectSdf(GlassMath.Vec2(10f, 0f), half, 3f), 0.001f)
        assertTrue(GlassMath.roundedRectSdf(GlassMath.Vec2(13f, 0f), half, 3f) > 0f)
    }

    @Test fun lensProfile_isMonotonic_andBounded() {
        val values = listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map(GlassMath::lensProfile)
        assertEquals(0f, values.first(), 0.0001f)
        assertEquals(1f, values.last(), 0.0001f)
        assertTrue(values.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test fun dispersionOffsets_areSymmetricAroundGreen() {
        val (red, green, blue) = GlassMath.dispersionOffsets(GlassMath.Vec2(3f, 4f), 2f)
        assertEquals(0f, green.length(), 0.0001f)
        assertEquals(0f, (red + blue).length(), 0.0001f)
        assertEquals(2f, red.length(), 0.0001f)
    }

    @Test fun rimProgress_clampsToUnitInterval() {
        assertEquals(0f, GlassMath.rimProgress(-50f, 10f), 0.0001f)
        assertEquals(1f, GlassMath.rimProgress(2f, 10f), 0.0001f)
    }
}
