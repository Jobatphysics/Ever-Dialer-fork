package com.android.libredialer.view.components

import com.android.libredialer.view.newui.navigation.NewUiSettingsDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSearchSupportTest {
    @Test
    fun settingsSearchKeysTargetLibreUiDestinations() {
        assertEquals(
            NewUiSettingsDestination.ColorsAndTheme,
            settingsDestinationForKey("interface")
        )
        assertEquals(
            NewUiSettingsDestination.BiometricAndAppLock,
            settingsDestinationForKey("lock_call_actions")
        )
        assertEquals(
            NewUiSettingsDestination.NetworkSwitcher,
            settingsDestinationForKey("network_switcher_app")
        )
        assertEquals(
            NewUiSettingsDestination.SimAndCallPlacement,
            settingsDestinationForKey("default_sim")
        )
        assertEquals(
            NewUiSettingsDestination.CallRecording,
            settingsDestinationForKey("recording_storage")
        )
    }
}
