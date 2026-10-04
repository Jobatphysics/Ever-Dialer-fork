package com.android.libredialer.controller.util

import com.android.libredialer.modal.data.Contact
import com.android.libredialer.modal.data.ContactPhone
import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Assert.assertThrows
import java.io.IOException

class CallingCardStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun callingCardPersistsSeparatelyFromContactPicture() {
        val store = CallingCardStore(temporaryFolder.newFolder("cards"))
        val contact = Contact(
            id = "42",
            name = "Alex",
            phoneNumbers = listOf("6505550123"),
            photoUri = "content://contacts/photo/42"
        )
        val cardBytes = byteArrayOf(1, 2, 3, 4)

        store.save(contact.id, ByteArrayInputStream(cardBytes))

        assertEquals("content://contacts/photo/42", contact.photoUri)
        assertArrayEquals(cardBytes, CallingCardStore(temporaryFolder.root.resolve("cards"))
            .get(contact.id)?.readBytes())
    }

    @Test
    fun removingCallingCardLeavesOtherContactsCardsIntact() {
        val store = CallingCardStore(temporaryFolder.newFolder("cards"))
        store.save("one", ByteArrayInputStream(byteArrayOf(1)))
        store.save("two", ByteArrayInputStream(byteArrayOf(2)))

        assertTrue(store.remove("one"))

        assertNull(store.get("one"))
        assertArrayEquals(byteArrayOf(2), store.get("two")?.readBytes())
    }

    @Test
    fun missingOrDeletedContactHasNoCallingCard() {
        val store = CallingCardStore(temporaryFolder.newFolder("cards"))

        assertNull(store.get("deleted-contact"))
        assertTrue(store.remove("deleted-contact"))
    }

    @Test
    fun emptyImageIsRejectedWithoutReplacingTheStoredCard() {
        val store = CallingCardStore(temporaryFolder.newFolder("cards"))
        val original = byteArrayOf(5, 6)
        store.save("contact", ByteArrayInputStream(original))

        assertThrows(IOException::class.java) {
            store.save("contact", ByteArrayInputStream(byteArrayOf()))
        }

        assertArrayEquals(original, store.get("contact")?.readBytes())
    }

    @Test
    fun callerMatchingChecksAllNumbersAndNormalizedForms() {
        val contact = Contact(
            id = "multi-number",
            name = "Alex",
            phones = listOf(
                ContactPhone("2125550199"),
                ContactPhone("+1 (650) 555-0123")
            )
        )

        assertEquals(contact, findContactForCaller("6505550123", listOf(contact)))
        assertNull(findContactForCaller("private", listOf(contact)))
        assertFalse(findContactForCaller("911", listOf(contact)) != null)
    }
}
