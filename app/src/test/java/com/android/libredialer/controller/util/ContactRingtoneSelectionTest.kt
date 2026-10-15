package com.android.libredialer.controller.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactRingtoneSelectionTest {

    @Test
    fun silentSelectionIsStoredAsEmptyValue() {
        assertEquals("", encodeContactRingtoneSelection(null, "content://settings/system/ringtone"))
    }

    @Test
    fun defaultSelectionResetsCustomRingtone() {
        assertEquals(
            null,
            encodeContactRingtoneSelection(
                "content://settings/system/ringtone",
                "content://settings/system/ringtone"
            )
        )
    }

    @Test
    fun customSelectionPreservesPickedUri() {
        val customUri = "content://media/internal/audio/media/42"
        assertEquals(
            customUri,
            encodeContactRingtoneSelection(customUri, "content://settings/system/ringtone")
        )
    }
}
