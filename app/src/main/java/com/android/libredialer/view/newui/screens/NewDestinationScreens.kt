package com.android.libredialer.view.newui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.collectAsState
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.koinInject
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.combinedClickable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.libredialer.view.newui.components.NewUiPlaceholderCard
import com.android.libredialer.view.newui.components.NewUiScreenShell
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiSettingsDestination
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.modal.data.CallLogEntry
import com.android.libredialer.controller.CallLogViewModel
import com.android.libredialer.controller.util.formatDate
import com.android.libredialer.controller.util.numbersLikelyMatch
import android.provider.CallLog

@Composable
fun NewDialerScreen(
    onCall: (String) -> Unit,
    onSettings: () -> Unit
) {
    var number by remember { mutableStateOf("") }

    val contactsVM: com.android.libredialer.controller.ContactsViewModel =
        koinActivityViewModel()

    val contacts by contactsVM.allContacts.collectAsState()

    fun normalizeNumber(value: String): String =
        value.filter { it.isDigit() }

    fun t9(value: String): String {
        val map = mapOf(
            'a' to '2', 'b' to '2', 'c' to '2',
            'd' to '3', 'e' to '3', 'f' to '3',
            'g' to '4', 'h' to '4', 'i' to '4',
            'j' to '5', 'k' to '5', 'l' to '5',
            'm' to '6', 'n' to '6', 'o' to '6',
            'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
            't' to '8', 'u' to '8', 'v' to '8',
            'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
        )

        return value.lowercase()
            .filter { it.isLetter() }
            .mapNotNull { map[it] }
            .joinToString("")
    }

    val searchResults = remember(number, contacts) {
        val query = normalizeNumber(number)

        if (query.isBlank()) {
            emptyList()
        } else {
            contacts
                .mapNotNull { contact ->
                    val matchingPhone = contact.phoneNumbers.firstOrNull { phone ->
                        normalizeNumber(phone).contains(query)
                    }

                    val nameT9 = t9(contact.name)

                    val nameMatches = nameT9.startsWith(query)

                    if (matchingPhone != null || nameMatches) {
                        contact to (matchingPhone ?: contact.phoneNumbers.firstOrNull())
                    } else {
                        null
                    }
                }
                .filter { it.second != null }
                .take(6)
        }
    }

    NewUiScreenShell(destination = NewUiDestination.Dialer) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (-68).dp)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings"
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Search results area.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = searchResults,
                            key = { it.first.id }
                        ) { (contact, matchedNumber) ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        matchedNumber?.let {
                                            number = it
                                        }
                                    }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    matchedNumber?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Number display immediately above keypad.
            if (number.isNotEmpty()) {
                Text(
                    text = number,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Five-row keypad.
            DialerKeypad(
                onDigit = { number += it },
                onDelete = {
                    if (number.isNotEmpty()) {
                        number = number.dropLast(1)
                    }
                },
                onClear = {
                    number = ""
                },
                onCall = {
                    if (number.isNotBlank()) {
                        onCall(number)
                    }
                }
            )
            }
        }
    }
}

@Composable
fun NewRecentsScreen(onCallLogClick: (CallLogEntry) -> Unit) {
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val logs by callLogViewModel.allCallLogs.collectAsState()
    val contacts by contactsViewModel.allContacts.collectAsState()

    NewUiScreenShell(destination = NewUiDestination.Recents) {
        if (logs.isEmpty()) {
            Text(
                text = "No recent calls",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = logs,
                    key = { "${it.number}|${it.date}" }
                ) { log ->
                    val contact = remember(log, contacts) {
                        contacts.firstOrNull { candidate ->
                            (!log.contactId.isNullOrBlank() && candidate.id == log.contactId) ||
                                candidate.phoneNumbers.any { number ->
                                    numbersLikelyMatch(number, log.number)
                                }
                        }
                    }
                    NewCallLogRow(
                        log = log,
                        contact = contact,
                        onClick = { onCallLogClick(log) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NewCallLogRow(
    log: CallLogEntry,
    contact: Contact?,
    onClick: () -> Unit
) {
    val displayName = contact?.name?.takeIf { it.isNotBlank() }
        ?: log.name?.takeIf { it.isNotBlank() && it != log.number && !log.isCallerIdName }
        ?: log.number
    val photoUri = contact?.photoUri ?: log.photoUri
    val typeLabel = when (log.type) {
        CallLog.Calls.INCOMING_TYPE -> "Incoming"
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE -> "Missed"
        CallLog.Calls.REJECTED_TYPE -> "Rejected"
        CallLog.Calls.BLOCKED_TYPE -> "Blocked"
        else -> "Call"
    }
    val typeIcon = when (log.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
        else -> Icons.Filled.Call
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (log.type == CallLog.Calls.MISSED_TYPE) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayName.trim().firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(displayName, style = MaterialTheme.typography.titleMedium)
                if (contact != null || displayName == log.number) {
                    Text(
                        text = log.number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$typeLabel · ${formatDate(log.date, false)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (log.type == CallLog.Calls.MISSED_TYPE) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Icon(
                imageVector = typeIcon,
                contentDescription = typeLabel,
                tint = if (log.type == CallLog.Calls.MISSED_TYPE) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
fun NewContactsScreen(onContactClick: (Contact) -> Unit) {
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val contacts by contactsViewModel.displayedContacts.collectAsState()
    var query by remember { mutableStateOf("") }
    val filteredContacts = remember(contacts, query) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(normalizedQuery, ignoreCase = true) ||
                    contact.phoneNumbers.any { number ->
                        number.contains(normalizedQuery, ignoreCase = true)
                    }
            }
        }
    }

    NewUiScreenShell(destination = NewUiDestination.Contacts) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search contacts") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredContacts,
                    key = { it.id }
                ) { contact ->
                    NewContactRow(
                        contact = contact,
                        onClick = { onContactClick(contact) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NewContactRow(
    contact: Contact,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!contact.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = contact.photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contact.name.trim().firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = contact.name.ifBlank {
                        contact.phoneNumbers.firstOrNull() ?: "Unknown contact"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                val phoneSummary = contact.phoneNumbers
                    .filter(String::isNotBlank)
                    .distinct()
                    .joinToString(" · ")
                if (phoneSummary.isNotBlank()) {
                    Text(
                        text = phoneSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NewFavoritesScreen(onContactClick: (Contact) -> Unit) {
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsViewModel.allContacts.collectAsState()
    val favorites = remember(allContacts) {
        allContacts.filter { it.isFavorite }
    }

    NewUiScreenShell(destination = NewUiDestination.Favorites) {
        if (favorites.isEmpty()) {
            Text(
                text = "No favorite contacts",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = favorites,
                    key = { it.id }
                ) { contact ->
                    NewFavoriteRow(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onRemove = { contactsViewModel.toggleFavorite(contact) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NewFavoriteRow(
    contact: Contact,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!contact.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = contact.photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contact.name.trim().firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = contact.name.ifBlank {
                        contact.phoneNumbers.firstOrNull() ?: "Unknown contact"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                val phoneSummary = contact.phoneNumbers
                    .filter(String::isNotBlank)
                    .distinct()
                    .joinToString(" · ")
                if (phoneSummary.isNotBlank()) {
                    Text(
                        text = phoneSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class NewSettingsRow(
    val title: String,
    val summary: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val destination: NewUiSettingsDestination
)

@Composable
fun NewSettingsScreen(
    selectedDestination: NewUiSettingsDestination?,
    onNavigate: (NewUiSettingsDestination) -> Unit
) {
    if (selectedDestination != null) {
        NewSettingsPlaceholderScreen(selectedDestination)
        return
    }

    val rows = remember {
        listOf(
            "App & Calls" to listOf(
                NewSettingsRow(
                    "App & Call Behavior",
                    "Call behavior, notes, services, and integrations",
                    Icons.Filled.Tune,
                    NewUiSettingsDestination.AppAndCallBehavior
                ),
                NewSettingsRow(
                    "SIM & Call Placement",
                    "Default SIM, call confirmation, and SIM controls",
                    Icons.Filled.SimCard,
                    NewUiSettingsDestination.SimAndCallPlacement
                ),
                NewSettingsRow(
                    "Call Accounts",
                    "Manage Android Telecom phone accounts",
                    Icons.Filled.Phone,
                    NewUiSettingsDestination.CallAccounts
                )
            ),
            "Appearance" to listOf(
                NewSettingsRow(
                    "Colors & Theme",
                    "Material 3 colors, theme mode, and Material You options",
                    Icons.Filled.Palette,
                    NewUiSettingsDestination.ColorsAndTheme
                )
            ),
            "Sound & Vibration" to listOf(
                NewSettingsRow(
                    "Sound & Vibration",
                    "Dialpad tones and notification-policy options",
                    Icons.Filled.Vibration,
                    NewUiSettingsDestination.SoundAndVibration
                )
            ),
            "Privacy & Security" to listOf(
                NewSettingsRow(
                    "Biometric & App Lock",
                    "Protect calls and private app content",
                    Icons.Filled.Fingerprint,
                    NewUiSettingsDestination.BiometricAndAppLock
                ),
                NewSettingsRow(
                    "Contacts Hider",
                    "Hide contacts behind the existing access controls",
                    Icons.Filled.Security,
                    NewUiSettingsDestination.ContactsHider
                )
            ),
            "Call Recording" to listOf(
                NewSettingsRow(
                    "Call Recording",
                    "Open the existing recorder settings and controls",
                    Icons.Filled.Phone,
                    NewUiSettingsDestination.CallRecording
                )
            ),
            "Advanced / Services" to listOf(
                NewSettingsRow(
                    "Raise to Answer",
                    "Answer calls using the existing sensor service",
                    Icons.Filled.Phone,
                    NewUiSettingsDestination.RaiseToAnswer
                ),
                NewSettingsRow(
                    "Rain Mode",
                    "Configure motion-based call actions",
                    Icons.Filled.Tune,
                    NewUiSettingsDestination.RainMode
                ),
                NewSettingsRow(
                    "Volume DND",
                    "Configure the existing accessibility service",
                    Icons.Filled.Tune,
                    NewUiSettingsDestination.VolumeDnd
                ),
                NewSettingsRow(
                    "Network Switcher",
                    "Open the existing network and Shizuku controls",
                    Icons.Filled.Tune,
                    NewUiSettingsDestination.NetworkSwitcher
                )
            ),
            "Other" to listOf(
                NewSettingsRow(
                    "Updates",
                    "Check for app updates",
                    Icons.Filled.Update,
                    NewUiSettingsDestination.Updates
                ),
                NewSettingsRow(
                    "About",
                    "App information, links, and support",
                    Icons.Filled.Info,
                    NewUiSettingsDestination.About
                )
            )
        )
    }

    NewUiScreenShell(destination = NewUiDestination.Settings) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            rows.forEach { (section, sectionRows) ->
                item(key = section) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = section,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 1.dp
                        ) {
                            Column {
                                sectionRows.forEachIndexed { index, row ->
                                    NewSettingsRowItem(row = row, onClick = { onNavigate(row.destination) })
                                    if (index < sectionRows.lastIndex) {
                                        androidx.compose.material3.HorizontalDivider(
                                            modifier = Modifier.padding(start = 68.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewSettingsPlaceholderScreen(destination: NewUiSettingsDestination) {
    if (destination == NewUiSettingsDestination.ColorsAndTheme) {
        NewColorsAndThemeScreen()
        return
    }
    NewUiScreenShell(destination = NewUiDestination.Settings) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = "${destination.title} foundation",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun NewColorsAndThemeScreen() {
        val prefs: com.android.libredialer.controller.util.PreferenceManager = koinInject()
        val settingsVersion by prefs.settingsChanged.collectAsState()
        val selectedMode = prefs.getString(
            com.android.libredialer.controller.util.PreferenceManager.KEY_THEME_MODE,
            "auto"
        ) ?: "auto"
        val modes = listOf(
            "auto" to "System",
            "light" to "Light",
            "dark" to "Dark",
            "black" to "Pure AMOLED"
        )

        NewUiScreenShell(destination = NewUiDestination.Settings) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp
                ) {
                    Column {
                        modes.forEachIndexed { index, (mode, label) ->
                            Surface(
                                onClick = {
                                    prefs.setString(
                                        com.android.libredialer.controller.util.PreferenceManager.KEY_THEME_MODE,
                                        mode
                                    )
                                },
                                color = androidx.compose.ui.graphics.Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedMode == mode, onClick = null)
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text(label, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = when (mode) {
                                                "auto" -> "Follow the system and use Material You colors"
                                                "light" -> "Force light mode with Material You colors"
                                                "dark" -> "Force dark mode with Material You colors"
                                                else -> "True black surfaces with Material You accents"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (index < modes.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
}

@Composable
private fun NewSettingsRowItem(
    row: NewSettingsRow,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(row.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    row.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewDestinationShell(destination: NewUiDestination) {
    NewUiScreenShell(destination = destination) {
        NewUiPlaceholderCard(destination)
    }
}

private val DialerKeys = listOf(
        "1" to "", "2" to "ABC", "3" to "DEF",
        "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
        "*" to "", "0" to "+", "#" to ""
    )

@Composable
private fun DialerKeypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onCall: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1 2 3
        // 4 5 6
        // 7 8 9
        // * 0 #
        DialerKeys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    KeypadButton(
                        digit = digit,
                        letters = letters,
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }

        // CALL under 0, DELETE under #
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty space under *
            Box(
                modifier = Modifier.size(108.dp)
            )

            // CALL — same 108dp circle as keypad
            Surface(
                onClick = onCall,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(108.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            // DELETE — same 108dp circle as keypad
            Surface(
                onClick = onDelete,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(108.dp)
                    .combinedClickable(
                        onClick = onDelete,
                        onLongClick = onClear
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Backspace,
                        contentDescription = "Delete",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    letters: String,
    onClick: () -> Unit
) {
    val interactionSource =
        remember {
            androidx.compose.foundation.interaction.MutableInteractionSource()
        }

    val pressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(108.dp)
            .graphicsLayer {
                val scale = if (pressed) 0.92f else 1f
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (letters.isEmpty()) {
            // * 1 # are perfectly centered.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 40.sp
                    )
                )
            }
        } else {
            // 2–9 and 0: digit above small secondary label.
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = digit,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 40.sp
                    )
                )

                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
