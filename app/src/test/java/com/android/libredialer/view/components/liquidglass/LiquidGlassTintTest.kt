package com.android.libredialer.view.components.liquidglass

import com.android.libredialer.controller.util.PreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassTintTest {

    @Test
    fun neutralGreyTintHasEqualRgbComponents() {
        val red = (LIQUID_GLASS_NEUTRAL_GREY shr 16) and 0xFF
        val green = (LIQUID_GLASS_NEUTRAL_GREY shr 8) and 0xFF
        val blue = LIQUID_GLASS_NEUTRAL_GREY and 0xFF
        val alpha = (LIQUID_GLASS_NEUTRAL_GREY ushr 24) and 0xFF

        assertEquals("Red and green components must be equal", red, green)
        assertEquals("Green and blue components must be equal", green, blue)
        assertEquals("Red component must be 128", 128, red)
        assertEquals("Green component must be 128", 128, green)
        assertEquals("Blue component must be 128", 128, blue)
        assertEquals("Alpha component must be 255", 255, alpha)
    }

    @Test
    fun defaultLiquidGlassTintIntensityIsTenPercent() {
        assertEquals(0.10f, DEFAULT_LIQUID_GLASS_TINT_ALPHA, 0.0001f)
        assertEquals(0.10f, PreferenceManager.DEFAULT_LIQUID_GLASS_TINT, 0.0001f)
    }

    @Test
    fun liquidGlassTintKeyIsCorrect() {
        assertEquals("liquid_glass_tint", PreferenceManager.KEY_LIQUID_GLASS_TINT)
    }

    @Test
    fun liquidGlassTintSliderRangeAndCoercion() {
        val minRange = 0f
        val maxRange = 0.25f

        assertTrue(DEFAULT_LIQUID_GLASS_TINT_ALPHA in minRange..maxRange)

        val clampedNegative = (-0.05f).coerceIn(minRange, maxRange)
        assertEquals(0f, clampedNegative, 0.0001f)

        val clampedHigh = 0.50f.coerceIn(minRange, maxRange)
        assertEquals(0.25f, clampedHigh, 0.0001f)

        val clampedDefault = 0.10f.coerceIn(minRange, maxRange)
        assertEquals(0.10f, clampedDefault, 0.0001f)
    }
}
