package com.android.libredialer.view.newui.screens

import com.android.libredialer.modal.data.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialerContactSearchTest {
    @Test
    fun matchesPhoneDigitsAtBeginningMiddleAndEnd() {
        val contacts = listOf(
            Contact("beginning", "Beginning", listOf("72481234")),
            Contact("middle", "Middle", listOf("9872481234")),
            Contact("end", "End", listOf("12347248"))
        )

        assertEquals(
            listOf("beginning", "middle", "end"),
            searchDialerContacts(contacts, "7248").map { it.first.id }
        )
    }

    @Test
    fun doesNotHideMatchingContactsAfterTheFirstSixResults() {
        val contacts = (1..6).map { index ->
            Contact("match-$index", "Match $index", listOf("9872481234"))
        } + Contact("sahu", "Sahu", listOf("9872481234"))

        assertEquals(
            "sahu",
            searchDialerContacts(contacts, "7248").last().first.id
        )
    }

    @Test
    fun matchesFormattedNumberAndSearchesAllNumbers() {
        val contact = Contact(
            "multiple",
            "Sahu",
            phoneNumbers = listOf("+1 (555) 000-0000", "+91 987-248-1234")
        )

        val result = searchDialerContacts(listOf(contact), "7-248")

        assertEquals(1, result.size)
        assertEquals("+91 987-248-1234", result.single().second)
    }

    @Test
    fun matchesNamesWithoutCaseSensitivityAndT9NameDigits() {
        val contact = Contact("sahu", "Sahu", listOf("9872481234"))

        assertEquals("sahu", searchDialerContacts(listOf(contact), "sAhU").single().first.id)
        assertEquals("sahu", searchDialerContacts(listOf(contact), "7248").single().first.id)
    }

    @Test
    fun emptyQueryReturnsNoSuggestions() {
        assertTrue(searchDialerContacts(listOf(Contact("empty", "Empty", listOf("7248"))), "").isEmpty())
        assertTrue(searchDialerContacts(listOf(Contact("empty", "Empty", listOf("7248"))), " +()- ").isEmpty())
    }
}
