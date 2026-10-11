package com.android.libredialer.view.newui.screens

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.libredialer.controller.CallLogViewModel
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.controller.util.NoteEntry
import com.android.libredialer.controller.util.NoteManager
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.controller.util.SearchHistoryManager
import com.android.libredialer.controller.util.matchesFuzzySearch
import com.android.libredialer.controller.util.normalizeNumberDigits
import com.android.libredialer.modal.data.CallLogEntry
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.view.components.RivoListItem
import com.android.libredialer.view.components.RivoSectionHeader
import com.android.libredialer.view.components.PermissionDeniedView
import com.android.libredialer.view.components.SearchClearButton
import com.android.libredialer.view.components.SearchFilterButton
import com.android.libredialer.view.components.SearchHistorySection
import com.android.libredialer.view.components.SearchPillInput
import com.android.libredialer.view.components.buildGlobalSettingsSearchEntries
import com.android.libredialer.view.components.getSearchFilterState
import com.android.libredialer.view.newui.navigation.NewUiSettingsDestination
import com.android.libredialer.view.newui.navigation.NewUiSettingsNavigation
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NewUniversalSearchScreen(
    onBack: () -> Unit,
    onContactClick: (Contact, String?) -> Unit,
    onNumberClick: (String) -> Unit,
    onUnknownNumberClick: (String) -> Unit,
    onCallLogClick: (CallLogEntry) -> Unit,
    onRecordingClick: (String) -> Unit,
    onSettings: NewUiSettingsNavigation
) {
    val contactsPermission = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    if (contactsPermission.status != PermissionStatus.Granted) {
        PermissionDeniedView(
            icon = Icons.Default.Person,
            title = "Contacts Permission Required",
            description = "Allow access to search contacts and related notes.",
            onGrantClick = { contactsPermission.launchPermissionRequest() }
        )
        return
    }
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    val recordingsViewModel: HomeViewModel = viewModel()
    val contacts by contactsViewModel.displayedContacts.collectAsState()
    val callLogs by callLogViewModel.allCallLogs.collectAsState()
    val groups by contactsViewModel.contactGroups.collectAsState()
    val recordings by recordingsViewModel.allRecordings.collectAsState()
    val enabledAccountKeys by contactsViewModel.enabledAccountKeys.collectAsState()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val hiddenGroupIds = remember(settingsVersion) { prefs.getHiddenContactGroupIds() }
    val filterState = remember(settingsVersion) { prefs.getSearchFilterState() }
    val settings = remember { buildGlobalSettingsSearchEntries() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var queryField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val query = queryField.text
    val history = remember(settingsVersion) {
        SearchHistoryManager.getHistory(prefs, SearchHistoryManager.Type.UNIVERSAL)
    }
    var notes by remember { mutableStateOf<List<NoteEntry>>(emptyList()) }
    var selectedNote by remember { mutableStateOf<NoteEntry?>(null) }
    var showDeleteNoteConfirm by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
        contactsViewModel.fetchContactGroups()
        notes = withContext(Dispatchers.IO) {
            runCatching { NoteManager.getAllNotes(context) }.getOrDefault(emptyList())
        }
    }
    val visibleGroups = remember(groups, hiddenGroupIds, enabledAccountKeys) {
        groups.filter { it.id !in hiddenGroupIds }
            .filter { com.android.libredialer.view.components.isGroupMatchingAccountFilter(it, enabledAccountKeys) }
    }

    val matchingContacts = remember(contacts, query, filterState.contacts) {
        if (query.isBlank() || !filterState.contacts) emptyList()
        else contacts.filter { contact ->
            val normalizedQuery = query.replace(" ", "")
            matchesFuzzySearch(contact.name, query) ||
                contact.phoneNumbers.any { it.replace(" ", "").contains(normalizedQuery) }
        }
    }
    val nonContacts = remember(callLogs, query, filterState.nonContacts) {
        if (query.isBlank() || !filterState.nonContacts) emptyList()
        else {
            val seen = LinkedHashMap<String, CallLogEntry>()
            callLogs.asSequence()
                .filter { it.contactId.isNullOrBlank() }
                .forEach { entry ->
                    val key = normalizeNumberDigits(entry.number).filter(Char::isDigit).takeLast(9)
                        .ifBlank { entry.number }
                    seen.putIfAbsent(key, entry)
            }
            val queryDigits = query.replace(" ", "")
            seen.values.filter { entry ->
                entry.number.replace(" ", "").contains(queryDigits) ||
                    (entry.isCallerIdName && !entry.name.isNullOrBlank() &&
                        matchesFuzzySearch(entry.name, query))
            }
        }
    }
    val matchingNotes = remember(notes, query, filterState.contactNotes) {
        if (!filterState.contactNotes) emptyList()
        else if (query.isBlank()) notes
        else notes.filter { note ->
            matchesFuzzySearch(note.contactName, query) ||
                note.phoneNumber.contains(
                    query.filter { it.isDigit() || it == '+' }.ifEmpty { query },
                    ignoreCase = true
                ) ||
                matchesNoteQuery(note.content, query)
        }
    }
    val matchingRecordings = remember(recordings, query, filterState.recordings, filterState.recordingNotes) {
        recordings.filter { item ->
            val queryDigits = query.replace(" ", "")
            (filterState.recordings &&
                !(filterState.recordingNotes && matchesNoteQuery(item.noteText, query)) && (
                query.isBlank() ||
                    item.contactName?.let { matchesFuzzySearch(it, query) } == true ||
                    item.phoneNumber.replace(" ", "").contains(queryDigits)
                )) || (filterState.recordingNotes && matchesNoteQuery(item.noteText, query))
        }
    }
    val matchingGroups = remember(visibleGroups, query, filterState.groups) {
        if (!filterState.groups) emptyList()
        else if (query.isBlank()) visibleGroups
        else visibleGroups.filter { matchesFuzzySearch(it.name, query) }
    }
    val matchingSettings = remember(settings, query, filterState.settings) {
        if (query.isBlank() || !filterState.settings) emptyList()
        else settings.filter {
            matchesFuzzySearch(it.title, query) || matchesFuzzySearch(it.subtitle, query)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val searchTop = (maxHeight * 0.25f - 28.dp).coerceAtLeast(0.dp)
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = searchTop)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            SearchPillInput(
                value = queryField,
                onValueChange = { queryField = it },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp)
                    .height(56.dp)
                    .focusRequester(focusRequester),
                placeholder = "Search",
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                placeholderTextAlign = TextAlign.Center,
                placeholderTextOffsetX = (-28).dp,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) {
                        SearchHistoryManager.addHistory(prefs, SearchHistoryManager.Type.UNIVERSAL, query)
                    }
                    keyboardController?.hide()
                })
            )
            SearchClearButton(
                onClick = {
                    if (query.isNotEmpty()) queryField = TextFieldValue("")
                    else onBack()
                },
                contentDescription = if (query.isNotEmpty()) "Clear search" else "Close search",
                buttonSize = 56.dp,
                iconSize = 34.dp
            )
            SearchFilterButton()
        }

        if (query.isNotBlank() && query.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) {
            RivoListItem(
                headline = "Call $query",
                supporting = "Open dialer with this number",
                leadingIcon = Icons.Default.Call,
                onClick = {
                    SearchHistoryManager.addHistory(prefs, SearchHistoryManager.Type.UNIVERSAL, query)
                    onNumberClick(query)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        val resultCount = matchingContacts.size + nonContacts.size + matchingNotes.size +
            matchingRecordings.size + matchingGroups.size + matchingSettings.size
        when {
            query.isBlank() && resultCount == 0 && history.isNotEmpty() -> SearchHistorySection(
                history = history,
                onItemClick = { item ->
                    queryField = TextFieldValue(item)
                    SearchHistoryManager.addHistory(prefs, SearchHistoryManager.Type.UNIVERSAL, item)
                },
                onRemoveItem = { SearchHistoryManager.removeHistoryItem(prefs, SearchHistoryManager.Type.UNIVERSAL, it) },
                onClearAll = { SearchHistoryManager.clearHistory(prefs, SearchHistoryManager.Type.UNIVERSAL) }
            )
            query.isBlank() && resultCount == 0 -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Universal search", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            resultCount == 0 -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Text("$resultCount results", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                filterState.order.forEach { section ->
                    when (section) {
                        "contacts" -> if (matchingContacts.isNotEmpty()) {
                            item { RivoSectionHeader("Contacts") }
                            items(matchingContacts, key = { "contact:${it.id}" }) { contact ->
                                SearchResultRow(
                                    contact.name,
                                    contact.phoneNumbers.firstOrNull(),
                                    Icons.Default.Person,
                                    onClick = { onContactClick(contact, null) }
                                )
                            }
                        }
                        "non_contacts" -> if (nonContacts.isNotEmpty()) {
                            item { RivoSectionHeader("Non contacts") }
                            items(nonContacts, key = { "call:${it.number}:${it.date}" }) { entry ->
                                SearchResultRow(entry.name?.ifBlank { entry.number } ?: entry.number, entry.number, Icons.Default.Call,
                                    onClick = { onCallLogClick(entry) },
                                    onCall = { onNumberClick(entry.number) })
                            }
                        }
                        "groups" -> if (matchingGroups.isNotEmpty()) {
                            item { RivoSectionHeader("Groups") }
                            items(matchingGroups, key = { "group:${it.id}" }) { group ->
                                SearchResultRow(
                                    group.name,
                                    "${group.contactIds.size} contacts",
                                    Icons.Default.Group,
                                    onClick = { com.android.libredialer.view.newui.navigation.NewUiNavigationRequests.openContacts(groupId = group.id) }
                                )
                            }
                        }
                        "contact_notes" -> if (matchingNotes.isNotEmpty()) {
                            item { RivoSectionHeader("Notes") }
                            items(matchingNotes, key = { "note:${it.file.absolutePath}" }) { note ->
                                SearchResultRow(
                                    note.contactName.ifBlank { note.phoneNumber },
                                    note.content,
                                    Icons.Default.StickyNote2,
                                    onClick = { selectedNote = note }
                                )
                            }
                        }
                        "recordings", "recording_notes" -> if (filterState.recordings || filterState.recordingNotes) {
                            val sectionItems = matchingRecordings.filter { recording ->
                                if (section == "recording_notes") filterState.recordingNotes && matchesNoteQuery(recording.noteText, query)
                                else filterState.recordings &&
                                    !(filterState.recordingNotes && matchesNoteQuery(recording.noteText, query)) && (
                                    recording.contactName?.let { matchesFuzzySearch(it, query) } == true ||
                                        (query.filter(Char::isDigit).isNotEmpty() &&
                                            normalizeNumberDigits(recording.phoneNumber).filter(Char::isDigit)
                                                .contains(query.filter(Char::isDigit)))
                                    )
                            }
                            if (sectionItems.isNotEmpty()) {
                                item { RivoSectionHeader(if (section == "recording_notes") "Recording notes" else "Recordings") }
                                items(sectionItems, key = { "$section:${it.uri}" }) { recording ->
                                    SearchResultRow(
                                        recording.contactName?.ifBlank { recording.phoneNumber } ?: recording.phoneNumber,
                                        if (section == "recording_notes") recording.noteText else recording.phoneNumber,
                                        Icons.Default.Mic,
                                        onClick = { onRecordingClick(recording.uri.toString()) },
                                        onAvatarClick = {
                                            val contact = contacts.firstOrNull { candidate ->
                                                candidate.phoneNumbers.any {
                                                    normalizeNumberDigits(it) == normalizeNumberDigits(recording.phoneNumber)
                                                }
                                            }
                                            if (contact != null) onContactClick(contact, recording.phoneNumber)
                                            else onUnknownNumberClick(recording.phoneNumber)
                                        }
                                    )
                                }
                            }
                        }
                        "settings" -> if (matchingSettings.isNotEmpty()) {
                            item { RivoSectionHeader("Settings") }
                            items(matchingSettings, key = { "setting:${it.key}" }) { entry ->
                                SearchResultRow(entry.title, entry.subtitle, Icons.Default.Settings, onClick = {
                                    SearchHistoryManager.addHistory(prefs, SearchHistoryManager.Type.UNIVERSAL, query)
                                    onSettings(settingsDestinationFor(entry.key), entry.key)
                                })
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        }
    }
        selectedNote?.let { note ->
            var noteText by remember(note) { mutableStateOf(note.content) }
            ModalBottomSheet(
                onDismissRequest = { selectedNote = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(note.contactName.ifBlank { note.phoneNumber }, style = MaterialTheme.typography.titleLarge)
                    if (note.phoneNumber.isNotBlank()) {
                        Text(note.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        minLines = 8,
                        label = { Text("Note") }
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showDeleteNoteConfirm = true }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        Button(onClick = {
                            NoteManager.writeNote(context, note.contactName, note.phoneNumber, noteText)
                            notes = NoteManager.getAllNotes(context)
                            selectedNote = null
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
            if (showDeleteNoteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteNoteConfirm = false },
                    title = { Text("Delete note?") },
                    text = { Text("This note will be permanently deleted.") },
                    confirmButton = {
                        TextButton(onClick = {
                            NoteManager.deleteNoteFile(note.file)
                            notes = NoteManager.getAllNotes(context)
                            showDeleteNoteConfirm = false
                            selectedNote = null
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteNoteConfirm = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onCall: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null
) {
    RivoListItem(
        headline = title,
        supporting = subtitle,
        leadingIcon = icon,
        trailingStartContent = if (onCall != null) {
            {
                IconButton(onClick = onCall) {
                    Icon(Icons.Default.Call, contentDescription = "Call")
                }
            }
        } else null,
        onClick = onClick,
        onAvatarClick = onAvatarClick,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun settingsDestinationFor(key: String): NewUiSettingsDestination = when {
    key == "interface" || key == "theme_mode" -> NewUiSettingsDestination.ColorsAndTheme
    key.contains("auth") || key.contains("lock") || key.startsWith("biometric") -> NewUiSettingsDestination.BiometricAndAppLock
    key == "integrate_notes" || key == "delete_notes_with_recording" -> NewUiSettingsDestination.AppAndCallBehavior
    key.contains("record") || key.contains("note") && key.contains("recording") -> NewUiSettingsDestination.CallRecording
    key.contains("network") || key.contains("preferred_network") -> NewUiSettingsDestination.NetworkSwitcher
    key.contains("sim") || key.contains("call_placement") || key.contains("contacts_to_display") -> NewUiSettingsDestination.SimAndCallPlacement
    key.contains("sound") || key.contains("tone") || key.contains("ringtone") || key.contains("dnd_settings") -> NewUiSettingsDestination.SoundAndVibration
    key.contains("rain") -> NewUiSettingsDestination.RainMode
    key.contains("raise") || key.contains("decline_by_flipping") -> NewUiSettingsDestination.RaiseToAnswer
    key.contains("volume") -> NewUiSettingsDestination.VolumeDnd
    key.contains("contacts_hider") -> NewUiSettingsDestination.ContactsHider
    key == "about" -> NewUiSettingsDestination.About
    else -> NewUiSettingsDestination.AppAndCallBehavior
}

private fun matchesNoteQuery(text: String, query: String): Boolean {
    if (query.isBlank()) return false
    fun normalize(value: String) =
        value.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
    val normalizedText = normalize(text)
    val tokens = normalize(query).split(" ").filter(String::isNotBlank)
    return if (tokens.isEmpty()) text.contains(query, ignoreCase = true)
    else tokens.all(normalizedText::contains)
}
