package com.android.libredialer.view.newui.screens

import com.android.libredialer.modal.data.Contact

internal fun searchDialerContacts(
    contacts: List<Contact>,
    query: String
): List<Pair<Contact, String>> {
    val normalizedQuery = normalizeDialerDigits(query)
    val nameQuery = query.trim()
    if (normalizedQuery.isEmpty() && nameQuery.isEmpty()) return emptyList()

    return contacts.mapNotNull { contact ->
        val matchingPhone = normalizedQuery.takeIf(String::isNotEmpty)?.let { digits ->
            contact.phoneNumbers.firstOrNull { phone ->
                normalizeDialerDigits(phone).contains(digits)
            }
        }
        val nameMatches = nameQuery.isNotEmpty() &&
            contact.name.startsWith(nameQuery, ignoreCase = true)
        val t9Matches = normalizedQuery.isNotEmpty() &&
            dialerNameDigits(contact.name).startsWith(normalizedQuery)
        val selectedNumber = matchingPhone ?: contact.phoneNumbers.firstOrNull()

        if ((matchingPhone != null || nameMatches || t9Matches) && selectedNumber != null) {
            contact to selectedNumber
        } else {
            null
        }
    }
}

private fun normalizeDialerDigits(value: String): String =
    buildString(value.length) {
        value.forEach { char ->
            char.digitToIntOrNull()?.let { append(it) }
        }
    }

private fun dialerNameDigits(value: String): String = buildString(value.length) {
    value.forEach { char ->
        when (char.lowercaseChar()) {
            in 'a'..'c' -> append('2')
            in 'd'..'f' -> append('3')
            in 'g'..'i' -> append('4')
            in 'j'..'l' -> append('5')
            in 'm'..'o' -> append('6')
            in 'p'..'s' -> append('7')
            in 't'..'v' -> append('8')
            in 'w'..'z' -> append('9')
        }
    }
}
