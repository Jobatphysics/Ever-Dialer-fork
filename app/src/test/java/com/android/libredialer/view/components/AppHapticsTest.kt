package com.android.libredialer.view.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppHapticsTest {
    @Test
    fun feedbackRequiresEnabledPreferenceAndMeaningfulChange() {
        assertTrue(shouldPerformAppHaptic(enabled = true, changed = true))
        assertFalse(shouldPerformAppHaptic(enabled = false, changed = true))
        assertFalse(shouldPerformAppHaptic(enabled = true, changed = false))
    }

    @Test
    fun preferenceStrengthMapsToSystemFeedbackEvents() {
        assertEquals(AppHapticEvent.CLICK, hapticEventForStrength("light", 0.5f))
        assertEquals(AppHapticEvent.LONG_PRESS, hapticEventForStrength("strong", 0.5f))
        assertEquals(AppHapticEvent.SELECTION, hapticEventForStrength("custom", 0.5f))
        assertEquals(AppHapticEvent.CLICK, hapticEventForStrength("custom", 0.9f))
    }
}
