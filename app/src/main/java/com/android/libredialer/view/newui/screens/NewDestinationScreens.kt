package com.android.libredialer.view.newui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.produceState
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.libredialer.controller.util.normalizeNumberDigits
import com.android.libredialer.view.newui.components.rememberPhysicalListState
import com.android.libredialer.view.newui.components.physicalListItem
import com.android.libredialer.view.newui.components.physicalItemInput
import com.android.libredialer.view.newui.components.PhysicalListState
import com.android.libredialer.view.newui.components.lensSurface
import com.android.libredialer.view.newui.motion.rememberLensInteractionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.TimePicker
import android.app.TimePickerDialog
import java.util.Calendar

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.libredialer.view.newui.components.NewUiPlaceholderCard
import com.android.libredialer.view.newui.components.NewUiScreenShell
import com.android.libredialer.view.newui.components.NewContextualAction
import com.android.libredialer.view.newui.components.NewContextualActionsDialog
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiSettingsDestination
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.modal.data.CallLogEntry
import com.android.libredialer.controller.CallLogViewModel
import com.android.libredialer.controller.util.formatDate
import com.android.libredialer.controller.util.numbersLikelyMatch
import com.android.libredialer.controller.util.BlockedNumbersManager
import com.android.libredialer.controller.util.CallReminderManager
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.controller.util.makeCall
import com.android.libredialer.modal.`interface`.ICallLogRepository
import com.android.libredialer.view.screen.settings.AboutAppScreen
import com.android.libredialer.view.screen.settings.AppSettingsScreen
import com.android.libredialer.view.screen.settings.BiometricScreen
import com.android.libredialer.view.screen.settings.CallAccountsScreen
import com.android.libredialer.view.screen.settings.ContactsHiderScreen
import com.android.libredialer.view.screen.settings.RainModeScreen
import com.android.libredialer.view.screen.settings.RaiseToAnswerScreen
import com.android.libredialer.view.screen.settings.SimAndCallPlacementScreen
import com.android.libredialer.view.screen.settings.SoundVibrationScreen
import com.android.libredialer.view.screen.settings.UpdatesScreen
import com.android.libredialer.view.screen.settings.VolumeDndScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.SettingsScreen as RecorderSettingsScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.SettingsViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavOptionsBuilder
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.RouteOrDirection
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.lifecycle.viewmodel.compose.viewModel
import android.provider.CallLog

@Composable
fun NewDialerScreen(
    onCall: (String) -> Unit,
    onSettings: () -> Unit,
    onAddContact: (String?) -> Unit = {}
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
    NewUiScreenShell(
        destination = NewUiDestination.Dialer,
        showTitle = number.isEmpty(),
        headerAction = if (number.isEmpty()) ({
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
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
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
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
                onDeleteRepeat = {
                    if (number.isNotEmpty()) {
                        number = number.dropLast(1)
                        true
                    } else false
                },
                onCall = {
                    if (number.isNotBlank()) {
                        onCall(number)
                    }
                },
                onAddContact = { onAddContact(number.takeIf { it.isNotEmpty() }) }
            )
            }
        }
    }
}

@Immutable
data class NewUiCallLogItem(
    val key: String,
    val log: CallLogEntry,
    val contact: Contact?,
    val displayName: String,
    val subtitle: String?,
    val photoUri: String?,
    val initialLetter: String,
    val typeLabel: String,
    val formattedDate: String,
    val isMissed: Boolean,
    val typeIcon: ImageVector
)

@Composable
fun NewRecentsScreen(
    onCallLogClick: (CallLogEntry) -> Unit,
    onSettings: () -> Unit,
    onCall: (String) -> Unit = {},
    onAddContact: (String) -> Unit = {}
) {
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val logs by callLogViewModel.allCallLogs.collectAsState()
    val contacts by contactsViewModel.allContacts.collectAsState()
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    var contextualItem by remember { mutableStateOf<NewUiCallLogItem?>(null) }
    var showReminderPicker by remember { mutableStateOf<NewUiCallLogItem?>(null) }

    val physicalListState = rememberPhysicalListState()

    // High-performance asynchronous mapping:
    // Builds O(1) indices and pre-formats all display text off the UI main thread
    val uiLogs by produceState<List<NewUiCallLogItem>>(initialValue = emptyList(), logs, contacts) {
        if (logs.isEmpty()) {
            value = emptyList()
            return@produceState
        }
        withContext(Dispatchers.Default) {
            val contactById = HashMap<String, Contact>(contacts.size)
            val contactByNumber = HashMap<String, Contact>(contacts.size * 2)
            for (c in contacts) {
                contactById[c.id] = c
                for (phone in c.phoneNumbers) {
                    val digits = normalizeNumberDigits(phone).filter { it.isDigit() }
                    if (digits.isNotEmpty()) {
                        contactByNumber[digits] = c
                        if (digits.length >= 7) {
                            contactByNumber[digits.takeLast(7)] = c
                        }
                    }
                }
            }

            val items = ArrayList<NewUiCallLogItem>(logs.size)
            for (log in logs) {
                val contact = if (!log.contactId.isNullOrBlank() && log.contactId != "null") {
                    contactById[log.contactId]
                } else {
                    val digits = normalizeNumberDigits(log.number).filter { it.isDigit() }
                    contactByNumber[digits] ?: if (digits.length >= 7) contactByNumber[digits.takeLast(7)] else null
                }

                val displayName = contact?.name?.takeIf { it.isNotBlank() }
                    ?: log.name?.takeIf { it.isNotBlank() && it != log.number && !log.isCallerIdName }
                    ?: log.number

                val subtitle = if (contact != null || displayName == log.number) log.number else null
                val photoUri = contact?.photoUri ?: log.photoUri
                val initialLetter = displayName.trim().firstOrNull()?.uppercase() ?: "?"

                val isMissed = log.type == CallLog.Calls.MISSED_TYPE
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

                val formattedDate = "$typeLabel · ${formatDate(log.date, false)}"
                val key = "${log.callIds.firstOrNull() ?: log.date}_${log.number}"

                items.add(
                    NewUiCallLogItem(
                        key = key,
                        log = log,
                        contact = contact,
                        displayName = displayName,
                        subtitle = subtitle,
                        photoUri = photoUri,
                        initialLetter = initialLetter,
                        typeLabel = typeLabel,
                        formattedDate = formattedDate,
                        isMissed = isMissed,
                        typeIcon = typeIcon
                    )
                )
            }
            value = items
        }
    }

    NewUiScreenShell(
        destination = NewUiDestination.Recents,
        headerAction = {
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    ) {
        if (uiLogs.isEmpty()) {
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
                contentPadding = PaddingValues(top = 72.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = uiLogs,
                    key = { _, item -> item.key }
                ) { index, item ->
                    NewCallLogRow(
                        item = item,
                        index = index,
                        physicalListState = physicalListState,
                        onClick = { onCallLogClick(item.log) },
                        onLongClick = { contextualItem = item }
                    )
                }
            }
            contextualItem?.let { item ->
                val number = item.log.number
                val blocked = BlockedNumbersManager.isBlocked(context, prefs, number)
                val actions = buildList {
                    add(NewContextualAction("Call") { onCall(number) })
                    add(NewContextualAction("Message") {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")))
                    })
                    add(NewContextualAction("Delete this call") {
                        callLogViewModel.deleteCallLog(item.log)
                    })
                    add(NewContextualAction("Delete call history for this number") {
                        callLogViewModel.deleteCallLogs(logs.filter { it.number == number })
                    })
                    add(NewContextualAction(if (blocked) "Unblock" else "Block") {
                        if (blocked) BlockedNumbersManager.unblock(context, prefs, number)
                        else BlockedNumbersManager.block(context, prefs, number)
                    })
                    if (item.contact == null) add(NewContextualAction("Add to Contacts") { onAddContact(number) })
                    add(NewContextualAction("Copy number") {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Phone number", number))
                    })
                    add(NewContextualAction("Remind me") { showReminderPicker = item })
                }
                NewContextualActionsDialog(
                    title = item.displayName,
                    actions = actions,
                    onDismiss = { contextualItem = null }
                )
            }
            showReminderPicker?.let { item ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val trigger = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                        }.timeInMillis
                        CallReminderManager.schedule(context, item.log.number, trigger)
                        showReminderPicker = null
                    },
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    Calendar.getInstance().get(Calendar.MINUTE),
                    true
                ).apply { setOnDismissListener { showReminderPicker = null } }.show()
            }
        }
    }
}

@Composable
private fun NewCallLogRow(
    item: NewUiCallLogItem,
    index: Int,
    physicalListState: PhysicalListState,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = rememberLensInteractionSource()

    val baseSurfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val callTypeTint = when (item.log.type) {
        CallLog.Calls.MISSED_TYPE -> MaterialTheme.colorScheme.error
        CallLog.Calls.OUTGOING_TYPE -> Color(0xFF2E7D32)
        CallLog.Calls.INCOMING_TYPE -> Color(0xFF1565C0)
        else -> baseSurfaceColor
    }
    val surfaceColor = if (item.log.type == CallLog.Calls.MISSED_TYPE) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        androidx.compose.ui.graphics.lerp(baseSurfaceColor, callTypeTint, 0.20f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .physicalListItem(physicalListState, index)
            .physicalItemInput(physicalListState, index, onClick, onLongClick)
            .lensSurface(
                shape = MaterialTheme.shapes.medium,
                tonalColor = surfaceColor,
                translucentAlpha = 0.88f,
                specularAlpha = 0.28f,
                elevation = 1.dp,
                interactionSource = interactionSource
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.photoUri.isNullOrBlank()) {
                val context = LocalContext.current
                val density = LocalDensity.current
                val avatarPx = remember(density) { with(density) { 52.dp.roundToPx() } }
                val imageRequest = remember(item.photoUri, avatarPx) {
                    coil.request.ImageRequest.Builder(context)
                        .data(item.photoUri)
                        .size(avatarPx)
                        .crossfade(true)
                        .memoryCacheKey(item.photoUri)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
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
                            text = item.initialLetter,
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
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (item.subtitle != null) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = item.formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isMissed) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = item.typeIcon,
                contentDescription = item.typeLabel,
                tint = if (item.isMissed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
fun NewContactsScreen(
    onContactClick: (Contact) -> Unit,
    onCreateContact: () -> Unit = {},
    onSettings: () -> Unit
) {
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
    val physicalListState = rememberPhysicalListState()
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    var contextualContact by remember { mutableStateOf<Contact?>(null) }

    NewUiScreenShell(
        destination = NewUiDestination.Contacts,
        sectionSpacing = 8.dp,
        headerAction = {
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 136.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = filteredContacts,
                    key = { _, contact -> contact.id }
                ) { index, contact ->
                    NewContactRow(
                        contact = contact,
                        index = index,
                        physicalListState = physicalListState,
                        onClick = { onContactClick(contact) },
                        onLongClick = { contextualContact = contact }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp)
                    .zIndex(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    label = { Text("Search contacts") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onCreateContact,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = "Add contact",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            contextualContact?.let { contact ->
                val number = contact.phoneNumbers.firstOrNull().orEmpty()
                val blocked = BlockedNumbersManager.isBlocked(context, prefs, number)
                NewContextualActionsDialog(
                    title = contact.name.ifBlank { number },
                    actions = listOf(
                        NewContextualAction(if (contact.isFavorite) "Remove from Favorites" else "Add to Favorites") {
                            contactsViewModel.toggleFavorite(contact)
                        },
                        NewContextualAction(if (blocked) "Unblock" else "Block") {
                            if (blocked) BlockedNumbersManager.unblock(context, prefs, number)
                            else BlockedNumbersManager.block(context, prefs, number)
                        },
                        NewContextualAction("Delete Contact") {
                            contactsViewModel.deleteContact(contact.id)
                        },
                        NewContextualAction("Copy number") {
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("Phone number", number))
                        }
                    ),
                    onDismiss = { contextualContact = null }
                )
            }
        }
    }
}

@Composable
private fun NewContactRow(
    contact: Contact,
    index: Int,
    physicalListState: PhysicalListState,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .physicalListItem(physicalListState, index)
            .physicalItemInput(physicalListState, index, onClick, onLongClick),
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
fun NewFavoritesScreen(
    onContactClick: (Contact) -> Unit,
    onSettings: () -> Unit
) {
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsViewModel.allContacts.collectAsState()
    val favorites = remember(allContacts) {
        allContacts.filter { it.isFavorite }
    }
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    var contextualContact by remember { mutableStateOf<Contact?>(null) }

    NewUiScreenShell(
        destination = NewUiDestination.Favorites,
        headerAction = {
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    ) {
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
                contentPadding = PaddingValues(top = 72.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = favorites,
                    key = { it.id }
                ) { contact ->
                    NewFavoriteRow(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onRemove = { contactsViewModel.toggleFavorite(contact) },
                        onLongClick = { contextualContact = contact }
                    )
                }
            }
            contextualContact?.let { contact ->
                val number = contact.phoneNumbers.firstOrNull().orEmpty()
                val blocked = BlockedNumbersManager.isBlocked(context, prefs, number)
                NewContextualActionsDialog(
                    title = contact.name.ifBlank { number },
                    actions = listOf(
                        NewContextualAction("Remove from Favorites") { contactsViewModel.toggleFavorite(contact) },
                        NewContextualAction(if (blocked) "Unblock" else "Block") {
                            if (blocked) BlockedNumbersManager.unblock(context, prefs, number)
                            else BlockedNumbersManager.block(context, prefs, number)
                        },
                        NewContextualAction("Delete Contact") { contactsViewModel.deleteContact(contact.id) },
                        NewContextualAction("Copy number") {
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("Phone number", number))
                        }
                    ),
                    onDismiss = { contextualContact = null }
                )
            }
        }
    }
}

@Composable
private fun NewFavoriteRow(
    contact: Contact,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val scale = if (pressed) 0.96f else 1f
                scaleX = scale
                scaleY = scale
            }
            // Keep the existing pressed scale while adding the contextual long-press path.
            // The trailing favorite button remains a separate normal tap target.
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
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
    onNavigate: (NewUiSettingsDestination) -> Unit,
    onBack: () -> Boolean
) {
    if (selectedDestination != null) {
        NewSettingsDestinationScreen(
            destination = selectedDestination,
            onNavigate = onNavigate,
            onBack = { onBack() }
        )
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
                    "Themes",
                    "Choose System, Light, Dark, or Pure AMOLED",
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
            contentPadding = PaddingValues(top = 72.dp),
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
private fun NewSettingsDestinationScreen(
    destination: NewUiSettingsDestination,
    onNavigate: (NewUiSettingsDestination) -> Unit,
    onBack: () -> Unit
) {
    if (destination == NewUiSettingsDestination.ColorsAndTheme) {
        NewColorsAndThemeScreen()
        return
    }

    val navigator = remember(onBack, onNavigate) {
        NewSettingsDestinationsNavigator(onBack, onNavigate)
    }
    when (destination) {
        NewUiSettingsDestination.AppAndCallBehavior ->
            AppSettingsScreen(navigator = navigator)
        NewUiSettingsDestination.SimAndCallPlacement ->
            SimAndCallPlacementScreen(navigator = navigator)
        NewUiSettingsDestination.CallAccounts ->
            CallAccountsScreen(navigator = navigator)
        NewUiSettingsDestination.SoundAndVibration ->
            SoundVibrationScreen(navigator = navigator)
        NewUiSettingsDestination.BiometricAndAppLock ->
            BiometricScreen(navigator = navigator)
        NewUiSettingsDestination.ContactsHider ->
            ContactsHiderScreen(navigator = navigator)
        NewUiSettingsDestination.CallRecording -> {
            val recorderViewModel: SettingsViewModel = viewModel()
            RecorderSettingsScreen(
                viewModel = recorderViewModel,
                onBack = onBack
            )
        }
        NewUiSettingsDestination.RaiseToAnswer ->
            RaiseToAnswerScreen(navigator = navigator)
        NewUiSettingsDestination.RainMode ->
            RainModeScreen(navigator = navigator)
        NewUiSettingsDestination.VolumeDnd ->
            VolumeDndScreen(navigator = navigator)
        NewUiSettingsDestination.NetworkSwitcher ->
            AppSettingsScreen(navigator = navigator, highlightKey = "network_switcher")
        NewUiSettingsDestination.Updates ->
            UpdatesScreen(navigator = navigator)
        NewUiSettingsDestination.About ->
            AboutAppScreen(navigator = navigator)
        NewUiSettingsDestination.ColorsAndTheme -> Unit
    }
}

private class NewSettingsDestinationsNavigator(
    private val onBack: () -> Unit,
    private val onNavigate: (NewUiSettingsDestination) -> Unit
) : DestinationsNavigator {
    override fun navigate(
        direction: Direction,
        builder: DestinationsNavOptionsBuilder.() -> Unit
    ) = navigate(direction, null, null)

    override fun navigate(
        direction: Direction,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        when {
            direction.route.startsWith("sound_vibration_screen") ->
                onNavigate(NewUiSettingsDestination.SoundAndVibration)
            direction.route.startsWith("raise_to_answer_screen") ->
                onNavigate(NewUiSettingsDestination.RaiseToAnswer)
            direction.route.startsWith("rain_mode_screen") ->
                onNavigate(NewUiSettingsDestination.RainMode)
            direction.route.startsWith("volume_dnd_screen") ->
                onNavigate(NewUiSettingsDestination.VolumeDnd)
            direction.route.startsWith("recordings_screen") ->
                onNavigate(NewUiSettingsDestination.CallRecording)
            direction.route.startsWith("app_settings_screen") ->
                onNavigate(NewUiSettingsDestination.AppAndCallBehavior)
            direction.route.startsWith("settings_screen") ->
                onNavigate(NewUiSettingsDestination.AppAndCallBehavior)
            direction.route.startsWith("interface_screen") ->
                onNavigate(NewUiSettingsDestination.ColorsAndTheme)
        }
    }

    override fun navigateUp(): Boolean {
        onBack()
        return true
    }

    override fun popBackStack(): Boolean {
        onBack()
        return true
    }

    override fun popBackStack(
        route: RouteOrDirection,
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean = popBackStack()

    override fun clearBackStack(route: RouteOrDirection): Boolean = false

    override fun getBackStackEntry(route: RouteOrDirection): NavBackStackEntry? = null
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

        NewUiScreenShell(
            destination = NewUiDestination.Settings,
            titleOverride = "Themes"
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 72.dp)
            ) {
                item {
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
        }

@Composable
private fun NewSettingsRowItem(
    row: NewSettingsRow,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.graphicsLayer {
            val scale = if (pressed) 0.96f else 1f
            scaleX = scale
            scaleY = scale
        },
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
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
    onDeleteRepeat: () -> Boolean,
    onCall: () -> Unit,
    onAddContact: () -> Unit
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
            val callInteractionSource = remember { MutableInteractionSource() }
            val deleteInteractionSource = remember { MutableInteractionSource() }
            val callPressed by callInteractionSource.collectIsPressedAsState()
            val deletePressed by deleteInteractionSource.collectIsPressedAsState()
            LaunchedEffect(deletePressed) {
                if (deletePressed) {
                    delay(500L)
                    while (deletePressed && onDeleteRepeat()) {
                        delay(80L)
                    }
                }
            }
            val addContactInteractionSource = remember { MutableInteractionSource() }
            val addContactPressed by addContactInteractionSource.collectIsPressedAsState()
            Surface(
                onClick = onAddContact,
                interactionSource = addContactInteractionSource,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        val scale = if (addContactPressed) 0.92f else 1f
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = "Add contact",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // CALL — same 108dp circle as keypad
            Surface(
                onClick = onCall,
                interactionSource = callInteractionSource,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        val scale = if (callPressed) 0.92f else 1f
                        scaleX = scale
                        scaleY = scale
                    }
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
                interactionSource = deleteInteractionSource,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        val scale = if (deletePressed) 0.92f else 1f
                        scaleX = scale
                        scaleY = scale
                    }
                    .combinedClickable(
                        interactionSource = deleteInteractionSource,
                        indication = null,
                        onClick = onDelete,
                        onLongClick = {}
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
