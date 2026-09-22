package dev.libreglass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibreGlassOpticsTest {
    @Test fun normalization_keepsAllShaderInputsInSafeRanges() {
        val normalized = LibreGlassOptics(
            blurRadius = -2f,
            lensStrength = 100f,
            refractionWidth = 0f,
            distortion = -1f,
            chromaticDispersion = 10f,
            saturation = 3f,
            brightness = -2f,
            highlightAlpha = 2f,
            rimAlpha = -1f,
            shadowAlpha = 9f,
        ).normalized()

        assertEquals(0f, normalized.blurRadius, 0f)
        assertEquals(32f, normalized.lensStrength, 0f)
        assertEquals(1f, normalized.refractionWidth, 0f)
        assertEquals(4f, normalized.chromaticDispersion, 0f)
        assertEquals(2f, normalized.saturation, 0f)
        assertTrue(normalized.brightness >= -0.25f)
        assertEquals(1f, normalized.highlightAlpha, 0f)
        assertEquals(0f, normalized.rimAlpha, 0f)
        assertEquals(1f, normalized.shadowAlpha, 0f)
    }
}
