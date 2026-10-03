
package com.android.libredialer.view.newui.screens

import com.android.libredialer.view.newui.components.LensSurfaceBox
import com.android.libredialer.view.components.SettingsSearchHeaderAction
import com.android.libredialer.view.components.providedCallIcon
import com.android.libredialer.view.components.providedMessageIcon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.runtime.produceState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.libredialer.controller.util.normalizeNumberDigits
import com.android.libredialer.view.newui.components.rememberPhysicalListState
import com.android.libredialer.view.newui.components.physicalListItem
import com.android.libredialer.view.newui.components.physicalItemInput
import com.android.libredialer.view.newui.components.PhysicalListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.collectAsState
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.koinInject
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import android.widget.TimePicker
import android.app.TimePickerDialog
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.libredialer.view.newui.components.NewUiPlaceholderCard
import com.android.libredialer.view.newui.components.NewUiScreenShell
import com.android.libredialer.view.newui.components.NewUiDialerButton
import com.android.libredialer.view.newui.components.newUiScrollContentPadding
import com.android.libredialer.view.newui.components.newUiSettingsHeaderContentTopPadding
import com.android.libredialer.view.newui.components.newUiClickable
import com.android.libredialer.view.newui.motion.NewUiMotion
import com.android.libredialer.view.newui.components.LocalNewUiSettingsStyle
import com.android.libredialer.view.newui.components.LocalNewUiScrollClearance
import com.android.libredialer.view.newui.components.NewContextualAction
import com.android.libredialer.view.newui.components.NewContextualActionsDialog
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiSettingsDestination
import com.android.libredialer.view.newui.navigation.NewUiNavigationTiming
import com.android.libredialer.view.newui.theme.NewUiDimensions
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
import com.android.libredialer.view.screen.settings.ContactsHiderScreen
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
import androidx.lifecycle.ViewModel
import android.provider.CallLog

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun NewDialerScreen(
    onCall: (String) -> Unit,
    onSettings: () -> Unit,
    onAddContact: (String?) -> Unit = {}
) {
    val numberField = rememberTextFieldState()
    val number = numberField.text.toString()
    val context = LocalContext.current

    fun insertAtSelection(value: String) {
        numberField.edit {
            val start = selection.min
            val end = selection.max
            replace(start, end, value)
            selection = TextRange(start + value.length)
        }
    }

    fun deleteAtSelection() {
        val currentNumber = numberField.text
        val start = numberField.selection.min
        val end = numberField.selection.max
        if (start != end) {
            numberField.edit {
                replace(start, end, "")
                selection = TextRange(start)
            }
        } else if (start > 0) {
            numberField.edit {
                replace(start - 1, start, "")
                selection = TextRange(start - 1)
            }
        }
    }

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
    val searchResultsTopInset = (
        with(LocalDensity.current) {
            WindowInsets.statusBars.union(WindowInsets.displayCutout).getTop(this).toDp()
        } - NewUiDimensions.PagePadding
    ).coerceAtLeast(0.dp)
    NewUiScreenShell(
        destination = NewUiDestination.Dialer,
        showTitle = number.isEmpty(),
        contentUnderStatusBar = true,
        headerAction = if (number.isEmpty()) ({
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = searchResultsTopInset),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = searchResults,
                            key = { it.first.id }
                        ) { (contact, matchedNumber) ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .newUiClickable(androidx.compose.ui.graphics.RectangleShape) {
                                        matchedNumber?.let {
                                            numberField.edit {
                                                replace(0, length, it)
                                                selection = TextRange(it.length)
                                            }
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
            InterceptPlatformTextInput(
                interceptor = { _, _ -> awaitCancellation() }
            ) {
                BasicTextField(
                    state = numberField,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(vertical = 4.dp),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 45.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        showKeyboardOnFocus = false
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
            // Five-row keypad.
            DialerKeypad(
                onDigit = { insertAtSelection(it) },
                onDelete = ::deleteAtSelection,
                onDeleteRepeat = {
                    val oldNumber = numberField.text
                    deleteAtSelection()
                    oldNumber != numberField.text
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
    val photoUri: String?,
    val typeLabel: String,
    val formattedDate: String,
    val isMissed: Boolean,
    val typeIcon: ImageVector
)

internal data class NewUiCallLogDateSection(
    val key: Int,
    val title: String,
    val items: List<IndexedValue<NewUiCallLogItem>>
)

internal class NewRecentsProcessingCache : ViewModel() {
    private var sourceLogs: List<CallLogEntry>? = null
    private var sourceContacts: List<Contact>? = null
    private var cachedItems: List<NewUiCallLogItem>? = null
    private var sourceSectionItems: List<NewUiCallLogItem>? = null
    private var sectionDateRefresh: Int? = null
    private var cachedSections: List<NewUiCallLogDateSection>? = null

    fun getItems(logs: List<CallLogEntry>, contacts: List<Contact>): List<NewUiCallLogItem>? =
        cachedItems?.takeIf {
            (sourceLogs === logs || sourceLogs == logs) &&
                (sourceContacts === contacts || sourceContacts == contacts)
        }

    fun storeItems(
        logs: List<CallLogEntry>,
        contacts: List<Contact>,
        items: List<NewUiCallLogItem>
    ) {
        sourceLogs = logs
        sourceContacts = contacts
        cachedItems = items
        sourceSectionItems = null
        sectionDateRefresh = null
        cachedSections = null
    }

    fun getDateSections(items: List<NewUiCallLogItem>, dateRefresh: Int): List<NewUiCallLogDateSection>? =
        cachedSections?.takeIf { sourceSectionItems === items && sectionDateRefresh == dateRefresh }

    fun storeDateSections(
        items: List<NewUiCallLogItem>,
        dateRefresh: Int,
        sections: List<NewUiCallLogDateSection>
    ) {
        sourceSectionItems = items
        sectionDateRefresh = dateRefresh
        cachedSections = sections
    }
}

@Composable
fun NewRecentsScreen(
    onCallLogClick: (CallLogEntry) -> Unit,
    onSettings: () -> Unit,
    onCall: (String) -> Unit = {},
    onAddContact: (String) -> Unit = {}
) {
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val processingCache: NewRecentsProcessingCache = viewModel()
    val logs by callLogViewModel.allCallLogs.collectAsState()
    val contacts by contactsViewModel.allContacts.collectAsState()
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val themeSettingsVersion by prefs.settingsChanged.collectAsState()
    val themeMode = remember(themeSettingsVersion) {
        prefs.getString(PreferenceManager.KEY_THEME_MODE, "auto") ?: "auto"
    }
    val systemDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light", "white" -> false
        "dark", "black" -> true
        else -> systemDarkTheme
    }
    var contextualItem by remember { mutableStateOf<NewUiCallLogItem?>(null) }
    var showReminderPicker by remember { mutableStateOf<NewUiCallLogItem?>(null) }

    val physicalListState = rememberPhysicalListState()
    var recentsContentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        recentsContentReady = true
    }
    SideEffect {
        NewUiNavigationTiming.recentsFirstCompositionCommitted()
    }
    var dateRefresh by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            delay((nextMidnight.timeInMillis - System.currentTimeMillis()).coerceAtLeast(1L))
            dateRefresh++
        }
    }

    // High-performance asynchronous mapping:
    // Builds O(1) indices and pre-formats all display text off the UI main thread
    var callLogsProcessed by remember { mutableStateOf(false) }

    val uiLogs by produceState(
        initialValue = processingCache.getItems(logs, contacts) ?: emptyList(),
        logs,
        contacts
    ) {
        callLogsProcessed = false
        processingCache.getItems(logs, contacts)?.let { cachedItems ->
            value = cachedItems
            callLogsProcessed = true
            Log.d("RecentsColdStart", "Mapped display-model cache hit: ${cachedItems.size} calls")
            return@produceState
        }
        Log.d("RecentsColdStart", "Mapped display-model cache miss: ${logs.size} calls, ${contacts.size} contacts")
        if (logs.isEmpty()) {
            value = emptyList()
            callLogsProcessed = true
            return@produceState
        }
        val processingStartedAt = SystemClock.elapsedRealtimeNanos()
        val mappedItems = withContext(Dispatchers.Default) {
            val contactIndexStartedAt = SystemClock.elapsedRealtime()
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
            Log.d(
                "RecentsColdStart",
                "UI contact index build: ${contacts.size} contacts, ${contactById.size} IDs, ${contactByNumber.size} number keys, ${SystemClock.elapsedRealtime() - contactIndexStartedAt}ms"
            )

            val sortStartedAt = SystemClock.elapsedRealtime()
            val sortedLogs = logs.sortedByDescending { it.date }
            Log.d(
                "RecentsColdStart",
                "UI call sort: ${sortedLogs.size} calls, ${SystemClock.elapsedRealtime() - sortStartedAt}ms"
            )
            val itemMappingStartedAt = SystemClock.elapsedRealtime()
            val items = ArrayList<NewUiCallLogItem>(sortedLogs.size)
            for (log in sortedLogs) {
                val contact = if (!log.contactId.isNullOrBlank() && log.contactId != "null") {
                    contactById[log.contactId]
                } else {
                    val digits = normalizeNumberDigits(log.number).filter { it.isDigit() }
                    contactByNumber[digits] ?: if (digits.length >= 7) contactByNumber[digits.takeLast(7)] else null
                }

                val displayName = contact?.name?.takeIf { it.isNotBlank() } ?: log.number
                val photoUri = contact?.photoUri ?: log.photoUri

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

                val formattedDate = formatDate(log.date, false)
                val key = "${log.callIds.firstOrNull() ?: log.date}_${log.number}"

                items.add(
                    NewUiCallLogItem(
                        key = key,
                        log = log,
                        contact = contact,
                        displayName = displayName,
                        photoUri = photoUri,
                        typeLabel = typeLabel,
                        formattedDate = formattedDate,
                        isMissed = isMissed,
                        typeIcon = typeIcon
                    )
                )
            }
            Log.d(
                "RecentsColdStart",
                "UI contact matching and display formatting: ${items.size} calls, ${SystemClock.elapsedRealtime() - itemMappingStartedAt}ms"
            )
            items
        }
        processingCache.storeItems(logs, contacts, mappedItems)
        value = mappedItems
        Log.d(
            "RecentsColdStart",
            "Mapped ${mappedItems.size} calls in ${(SystemClock.elapsedRealtimeNanos() - processingStartedAt) / 1_000_000}ms"
        )
        callLogsProcessed = true
    }

    val dateSections by produceState<List<NewUiCallLogDateSection>>(
        initialValue = processingCache.getDateSections(uiLogs, dateRefresh) ?: emptyList(),
        uiLogs,
        dateRefresh
    ) {
        processingCache.getDateSections(uiLogs, dateRefresh)?.let { cachedSections ->
            value = cachedSections
            Log.d("RecentsColdStart", "Date-section cache hit: ${cachedSections.size} sections")
            return@produceState
        }
        if (uiLogs.isEmpty()) {
            value = emptyList()
            return@produceState
        }
        val groupingStartedAt = SystemClock.elapsedRealtimeNanos()
        val groupedSections = withContext(Dispatchers.Default) {
            val today = Calendar.getInstance()
            val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val grouped = LinkedHashMap<Int, MutableList<IndexedValue<NewUiCallLogItem>>>()
            uiLogs.forEachIndexed { index, item ->
                val callDate = Calendar.getInstance().apply { timeInMillis = item.log.date }
                val dateKey = callDate.get(Calendar.YEAR) * 1000 + callDate.get(Calendar.DAY_OF_YEAR)
                grouped.getOrPut(dateKey) { mutableListOf() }.add(IndexedValue(index, item))
            }
            grouped.map { (key, items) ->
                NewUiCallLogDateSection(
                    key = key,
                    title = recentsDateHeader(items.first().value.log.date, today, yesterday),
                    items = items
                )
            }
        }
        processingCache.storeDateSections(uiLogs, dateRefresh, groupedSections)
        value = groupedSections
        Log.d(
            "RecentsColdStart",
            "Grouped ${uiLogs.size} calls into ${groupedSections.size} dates in ${(SystemClock.elapsedRealtimeNanos() - groupingStartedAt) / 1_000_000}ms"
        )
    }

    NewUiScreenShell(
        destination = NewUiDestination.Recents,
        contentUnderStatusBar = true,
        headerAction = {
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) {
        if (uiLogs.isEmpty()) {
            if (callLogsProcessed) {
                Text(
                    text = "No recent calls",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 88.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (recentsContentReady) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = newUiScrollContentPadding(PaddingValues(top = 112.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val listBuildStartedAt = SystemClock.elapsedRealtimeNanos()
                Trace.beginSection("Recents.buildLazyListItems")
                try {
                    dateSections.forEach { section ->
                        item(
                            key = "date_${section.key}",
                            contentType = "date-header"
                        ) {
                            com.android.libredialer.view.components.RivoSectionHeader(section.title)
                        }
                        items(
                            items = section.items,
                            key = { indexedItem -> indexedItem.value.key },
                            contentType = { "call-row" }
                        ) { indexedItem ->
                            val item = indexedItem.value
                            NewCallLogRow(
                                item = item,
                                index = indexedItem.index,
                                physicalListState = physicalListState,
                                isDarkTheme = isDarkTheme,
                                onClick = { onCallLogClick(item.log) },
                                onLongClick = { contextualItem = item },
                                onCall = { onCall(item.log.number) }
                            )
                        }
                    }
                } finally {
                    Trace.endSection()
                    Log.d(
                        "RecentsColdStart",
                        "Declared ${uiLogs.size} list items across ${dateSections.size} dates in ${(SystemClock.elapsedRealtimeNanos() - listBuildStartedAt) / 1_000_000}ms"
                    )
                }
            }
            contextualItem?.let { item ->
                val number = item.log.number
                val blocked = BlockedNumbersManager.isBlocked(context, prefs, number)
                val actions = buildList {
                    add(NewContextualAction("Call", providedCallIcon()) { onCall(number) })
                    add(NewContextualAction("Message", providedMessageIcon()) {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")))
                    })
                    add(NewContextualAction("Delete", Icons.Filled.Delete) {
                        callLogViewModel.deleteCallLog(item.log)
                    })
                    add(NewContextualAction(
                        if (blocked) "Unblock" else "Block",
                        Icons.Filled.Block
                    ) {
                        if (blocked) BlockedNumbersManager.unblock(context, prefs, number)
                        else BlockedNumbersManager.block(context, prefs, number)
                    })
                    if (item.contact == null) add(
                        NewContextualAction("Add to Contacts", Icons.Filled.PersonAdd) {
                            onAddContact(number)
                        }
                    )
                    add(NewContextualAction("Copy number", Icons.Filled.ContentCopy) {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Phone number", number))
                    })
                    add(NewContextualAction("Remind me", Icons.Filled.Alarm) {
                        showReminderPicker = item
                    })
                }
                NewContextualActionsDialog(
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
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCall: () -> Unit
) {
    val callTypeTint = when (item.log.type) {
        CallLog.Calls.MISSED_TYPE -> MaterialTheme.colorScheme.error
        CallLog.Calls.OUTGOING_TYPE -> MaterialTheme.colorScheme.secondary
        CallLog.Calls.INCOMING_TYPE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val rowBackground = when (item.log.type) {
        CallLog.Calls.OUTGOING_TYPE ->
            if (isDarkTheme) Color(0xFF3D4B36) else Color(0xFFD8E7CC)
        CallLog.Calls.INCOMING_TYPE ->
            if (isDarkTheme) Color(0xFF3D4758) else Color(0xFFD8E2F7)
        CallLog.Calls.MISSED_TYPE ->
            if (isDarkTheme) Color(0xFF5E4041) else Color(0xFFFFDADA)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .physicalListItem(physicalListState, index),
        shape = MaterialTheme.shapes.medium,
        color = rowBackground,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .physicalItemInput(physicalListState, index, onClick, onLongClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NewContactAvatar(
                    photoUri = item.photoUri,
                    displayName = item.displayName,
                    size = 56.dp
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = item.typeIcon,
                            contentDescription = item.typeLabel,
                            tint = callTypeTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = item.formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.isMissed) {
                                callTypeTint
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                            },
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(
                onClick = onCall,
                enabled = item.log.number.isNotBlank()
            ) {
                    Icon(
                        imageVector = providedCallIcon(),
                        contentDescription = "Call ${item.displayName}",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                    )
            }
        }
    }
}

private fun recentsDateHeader(timestamp: Long, today: Calendar, yesterday: Calendar): String {
    val callDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    fun sameCalendarDate(first: Calendar, second: Calendar) =
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)

    return when {
        sameCalendarDate(callDate, today) -> "Today"
        sameCalendarDate(callDate, yesterday) -> "Yesterday"
        else -> {
            val pattern = if (callDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                "EEEE, d MMMM"
            } else {
                "EEEE, d MMMM yyyy"
            }
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
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
        val matchingContacts = if (normalizedQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(normalizedQuery, ignoreCase = true) ||
                    contact.phoneNumbers.any { number ->
                        number.contains(normalizedQuery, ignoreCase = true)
                    }
            }
        }
        val firstContactByNumber = mutableMapOf<String, Contact>()
        val additionalCountByContact = mutableMapOf<String, Int>()
        val visibleContacts = mutableListOf<Contact>()
        matchingContacts.forEach { contact ->
            val duplicateOwner = contact.phoneNumbers
                .asSequence()
                .map { normalizeNumberDigits(it).filter(Char::isDigit) }
                .firstOrNull { it.isNotBlank() && firstContactByNumber.containsKey(it) }
                ?.let(firstContactByNumber::get)

            if (duplicateOwner != null) {
                additionalCountByContact[duplicateOwner.id] =
                    (additionalCountByContact[duplicateOwner.id] ?: 0) + 1
            } else {
                visibleContacts += contact
                contact.phoneNumbers
                    .asSequence()
                    .map { normalizeNumberDigits(it).filter(Char::isDigit) }
                    .filter(String::isNotBlank)
                    .forEach { number -> firstContactByNumber.putIfAbsent(number, contact) }
            }
        }
        visibleContacts.map { contact -> contact to (additionalCountByContact[contact.id] ?: 0) }
    }
    val physicalListState = rememberPhysicalListState()
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val contactScrollClearance = LocalNewUiScrollClearance.current
    var contextualContact by remember { mutableStateOf<Contact?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }

    NewUiScreenShell(
        destination = NewUiDestination.Contacts,
        contentUnderStatusBar = true,
        sectionSpacing = 8.dp,
        headerAction = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LensSurfaceBox(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search contacts",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                LensSurfaceBox(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onCreateContact) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = "Add contact",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                LensSurfaceBox(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = if (searchExpanded) 196.dp else 136.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = filteredContacts,
                    key = { _, item -> item.first.id }
                ) { index, (contact, additionalContactCount) ->
                    NewContactRow(
                        contact = contact,
                        additionalContactCount = additionalContactCount,
                        index = index,
                        physicalListState = physicalListState,
                        onClick = { onContactClick(contact) },
                        onLongClick = { contextualContact = contact }
                    )
                }
                if (filteredContacts.isNotEmpty()) {
                    item(key = "contacts_scroll_clearance") {
                        Spacer(Modifier.height(contactScrollClearance))
                    }
                }
            }
            if (searchExpanded) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 104.dp)
                        .zIndex(1f),
                    singleLine = true,
                    label = { Text("Search contacts") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            query = ""
                            searchExpanded = false
                        }) {
                            Icon(Icons.Filled.Backspace, contentDescription = "Close search")
                        }
                    }
                )
            }
            contextualContact?.let { contact ->
                val number = contact.phoneNumbers.firstOrNull().orEmpty()
                val blocked = BlockedNumbersManager.isBlocked(context, prefs, number)
                NewContextualActionsDialog(
                    actions = listOf(
                        NewContextualAction(
                            if (contact.isFavorite) "Remove from Favorites" else "Add to Favorites",
                            Icons.Filled.Favorite
                        ) {
                            contactsViewModel.toggleFavorite(contact)
                        },
                        NewContextualAction(
                            if (blocked) "Unblock" else "Block",
                            Icons.Filled.Block
                        ) {
                            if (blocked) BlockedNumbersManager.unblock(context, prefs, number)
                            else BlockedNumbersManager.block(context, prefs, number)
                        },
                        NewContextualAction("Delete Contact", Icons.Filled.Delete) {
                            contactsViewModel.deleteContact(contact.id)
                        },
                        NewContextualAction("Copy number", Icons.Filled.ContentCopy) {
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
    additionalContactCount: Int,
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
            NewContactAvatar(
                photoUri = contact.photoUri,
                displayName = contact.name,
                size = 56.dp
            )
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
                if (additionalContactCount > 0) {
                    Text(
                        text = "… more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NewContactAvatar(
    photoUri: String?,
    displayName: String,
    size: androidx.compose.ui.unit.Dp
) {
    if (!photoUri.isNullOrBlank()) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val avatarPx = remember(density, size) { with(density) { size.roundToPx() } }
        val imageRequest = remember(photoUri, avatarPx) {
            coil.request.ImageRequest.Builder(context)
                .data(photoUri)
                .size(avatarPx)
                .crossfade(true)
                .memoryCacheKey(photoUri)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.size(size),
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
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
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
                contentPadding = newUiScrollContentPadding(PaddingValues(top = 112.dp)),
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
                    actions = listOf(
                        NewContextualAction(
                            "Remove from Favorites",
                            Icons.Filled.Favorite
                        ) { contactsViewModel.toggleFavorite(contact) },
                        NewContextualAction(
                            if (blocked) "Unblock" else "Block",
                            Icons.Filled.Block
                        ) {
                            if (blocked) BlockedNumbersManager.unblock(context, prefs, number)
                            else BlockedNumbersManager.block(context, prefs, number)
                        },
                        NewContextualAction("Delete Contact", Icons.Filled.Delete) {
                            contactsViewModel.deleteContact(contact.id)
                        },
                        NewContextualAction("Copy number", Icons.Filled.ContentCopy) {
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
    val scale by animateFloatAsState(
        targetValue = if (pressed) NewUiMotion.PressedScale else 1f,
        animationSpec = NewUiMotion.Interaction.animationSpec(),
        label = "newFavoriteRowScale"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
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
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
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
            )
        )
    }

    val settingsNavigator = remember(onBack, onNavigate) {
        NewSettingsDestinationsNavigator({ onBack() }, onNavigate)
    }
    NewUiScreenShell(
        destination = NewUiDestination.Settings,
        headerAction = { SettingsSearchHeaderAction(settingsNavigator) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = 112.dp)),
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
    CompositionLocalProvider(LocalNewUiSettingsStyle provides true) {
        when (destination) {
            NewUiSettingsDestination.AppAndCallBehavior ->
                NewUiAppSettingsScreen(navigator = navigator)
            NewUiSettingsDestination.SimAndCallPlacement ->
                NewUiSimAndCallPlacementScreen(navigator = navigator)
            NewUiSettingsDestination.CallAccounts ->
                NewUiCallAccountsScreen()
            NewUiSettingsDestination.SoundAndVibration ->
                NewUiSoundVibrationScreen(navigator = navigator)
            NewUiSettingsDestination.BiometricAndAppLock ->
                NewUiBiometricScreen(navigator = navigator)
            NewUiSettingsDestination.ContactsHider ->
                NewUiContactsHiderScreen()
            NewUiSettingsDestination.CallRecording -> {
                val recorderViewModel: SettingsViewModel = viewModel()
                NewUiScreenShell(
                    destination = NewUiDestination.Settings,
                    titleOverride = "Call Recording",
                    headerAction = { SettingsSearchHeaderAction(navigator) }
                ) {
                    RecorderSettingsScreen(
                        viewModel = recorderViewModel,
                        onBack = onBack,
                        bottomScrollClearance = LocalNewUiScrollClearance.current,
                        embeddedInNewUi = true
                    )
                }
            }
            NewUiSettingsDestination.RaiseToAnswer ->
                NewUiRaiseToAnswerScreen(navigator = navigator)
            NewUiSettingsDestination.RainMode ->
                NewUiRainModeScreen(navigator = navigator)
            NewUiSettingsDestination.VolumeDnd ->
                NewUiVolumeDndScreen(navigator = navigator)
            NewUiSettingsDestination.NetworkSwitcher ->
                NewUiAppSettingsScreen(navigator = navigator, highlightKey = "network_switcher")
            NewUiSettingsDestination.ColorsAndTheme -> Unit
        }
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
            direction.route.startsWith("sim_and_call_placement_screen") ->
                onNavigate(NewUiSettingsDestination.SimAndCallPlacement)
            direction.route.startsWith("biometric_screen") ->
                onNavigate(NewUiSettingsDestination.BiometricAndAppLock)
            direction.route.startsWith("contacts_hider_screen") ->
                onNavigate(NewUiSettingsDestination.ContactsHider)
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
                contentPadding = newUiScrollContentPadding(
                    PaddingValues(top = newUiSettingsHeaderContentTopPadding())
                )
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
    val scale by animateFloatAsState(
        targetValue = if (pressed) NewUiMotion.PressedScale else 1f,
        animationSpec = NewUiMotion.Interaction.animationSpec(),
        label = "newSettingsRowScale"
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.graphicsLayer {
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
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
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
                        onClick = { onDigit(digit) },
                        onLongClick = if (digit == "0") ({ onDigit("+") }) else null
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
            NewUiDialerButton(
                onClick = onAddContact,
                interactionSource = addContactInteractionSource
            ) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = "Add contact",
                        modifier = Modifier.size(40.dp)
                    )
            }

            // CALL — same 98dp circle as keypad
            NewUiDialerButton(
                onClick = onCall,
                interactionSource = callInteractionSource,
                containerColor = Color(0xFF239E43),
                contentColor = Color.White
            ) {
                    Icon(
                        providedCallIcon(),
                        contentDescription = "Call",
                        modifier = Modifier.size(52.dp)
                    )
            }

            // DELETE — same 98dp circle as keypad
            NewUiDialerButton(
                onClick = onDelete,
                onLongClick = {},
                interactionSource = deleteInteractionSource,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
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

@Composable
private fun KeypadButton(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    NewUiDialerButton(
        onClick = onClick,
        onLongClick = onLongClick,
        pressedScale = 0.80f
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
