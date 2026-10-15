package com.android.libredialer.view.newui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomTabTransitionTest {

    @Test
    fun movingForwardThroughTabsSlidesLeft() {
        assertEquals(
            1,
            bottomTabTransitionDirection(
                NewUiDestination.Recents,
                NewUiDestination.Contacts
            )
        )
        assertEquals(
            1,
            bottomTabTransitionDirection(
                NewUiDestination.Contacts,
                NewUiDestination.Dialer
            )
        )
    }

    @Test
    fun movingBackwardThroughTabsSlidesRight() {
        assertEquals(
            -1,
            bottomTabTransitionDirection(
                NewUiDestination.Dialer,
                NewUiDestination.Contacts
            )
        )
        assertEquals(
            -1,
            bottomTabTransitionDirection(
                NewUiDestination.Contacts,
                NewUiDestination.Recents
            )
        )
    }

    @Test
    fun nonTabAndUnchangedDestinationsDoNotSlide() {
        assertNull(
            bottomTabTransitionDirection(
                NewUiDestination.Contacts,
                NewUiDestination.ContactDetails
            )
        )
        assertNull(
            bottomTabTransitionDirection(
                NewUiDestination.Contacts,
                NewUiDestination.Contacts
            )
        )
    }
}
