package com.android.libredialer.view.newui.navigation

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import android.os.SystemClock
import android.util.Log
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.modal.data.CallLogEntry
import com.android.libredialer.view.newui.screens.NewContactsScreen
import com.android.libredialer.view.newui.screens.NewContactDetailsScreen
import com.android.libredialer.view.newui.screens.NewContactEditScreen
import com.android.libredialer.view.newui.screens.NewCallDetailsScreen
import com.android.libredialer.view.newui.screens.NewDialerScreen
import com.android.libredialer.view.newui.screens.NewRecentsScreen
import com.android.libredialer.view.newui.screens.NewSettingsScreen
import com.android.libredialer.view.newui.screens.NewUniversalSearchScreen
import com.android.libredialer.view.newui.components.LocalNewUiScrollClearance
import com.android.libredialer.view.newui.components.NewUiNavigationBarScrollClearance
import com.android.libredialer.view.newui.components.newUiScrollHaptics
import com.android.libredialer.controller.CallLogViewModel
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.controller.util.numbersLikelyMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinActivityViewModel

@Immutable
enum class NewUiDestination(val route: String, val title: String) {
    Recents("new/recents", "Calls"),
    Contacts("new/contacts", "Contacts"),
    Dialer("new/dialer", "Dialer"),
    Settings("new/settings", "Settings"),
    ContactDetails("new/contact-details", "Contact details"),
    ContactEdit("new/contact-edit", "Edit contact"),
    CallDetails("new/call-details", "Call details"),
    UnknownNumberDetails("new/unknown-number-details", "Number details"),
    UniversalSearch("new/universal-search", "Search")
}

private fun NewUiDestination.isMainDestination(): Boolean = this in setOf(
    NewUiDestination.Recents,
    NewUiDestination.Contacts,
    NewUiDestination.Settings,
    NewUiDestination.Dialer
)

private fun PreferenceManager.lastMainDestination(): NewUiDestination {
    val savedRoute = getString(PreferenceManager.KEY_NEW_UI_LAST_MAIN_DESTINATION, null)
    if (savedRoute == "new/favorites") return NewUiDestination.Recents
    return NewUiDestination.entries.firstOrNull {
        it.route == savedRoute && it.isMainDestination()
    } ?: NewUiDestination.Dialer
}

@Stable
class NewUiNavigator(
    initialDestination: NewUiDestination,
    private val onMainDestinationChanged: (NewUiDestination) -> Unit = {}
) {
    var currentDestination by mutableStateOf(initialDestination)
        private set
    var settingsDestination by mutableStateOf<NewUiSettingsDestination?>(null)
        private set
    var settingsHighlightKey by mutableStateOf<String?>(null)
        private set
    var contactDetails by mutableStateOf<Contact?>(null)
        private set
    var contactDetailsPhoneNumber by mutableStateOf<String?>(null)
        private set
    var contactToEdit by mutableStateOf<Contact?>(null)
        private set
    var contactEditInitialPhone by mutableStateOf<String?>(null)
        private set
    var contactEditInitialName by mutableStateOf<String?>(null)
        private set
    var dialerInitialNumber by mutableStateOf<String?>(null)
        private set
    var unknownNumber by mutableStateOf<String?>(null)
        private set
    var callDetails by mutableStateOf<CallLogEntry?>(null)
        private set
    private val backStack = mutableListOf<NewUiDestination>()
    private val settingsBackStack = mutableListOf<Pair<NewUiSettingsDestination, String?>>()

    fun navigate(destination: NewUiDestination) {
        if (destination == currentDestination) {
            return
        }
        if (destination == NewUiDestination.Recents) {
            NewUiNavigationTiming.recentsNavigationStarted()
        }
        if (settingsDestination != null) {
            settingsDestination = null
            settingsHighlightKey = null
            settingsBackStack.clear()
        }

        contactDetails = null
        contactDetailsPhoneNumber = null
        contactToEdit = null
        contactEditInitialPhone = null
        contactEditInitialName = null
        unknownNumber = null
        callDetails = null
        if (destination.isMainDestination()) {
            backStack.clear()
        } else if (
            currentDestination != NewUiDestination.ContactDetails &&
            currentDestination != NewUiDestination.ContactEdit
        ) {
            backStack += currentDestination
        }
        currentDestination = destination
        if (destination.isMainDestination()) {
            onMainDestinationChanged(destination)
        }
    }

    fun navigateSettings(destination: NewUiSettingsDestination, highlightKey: String? = null) {
        settingsDestination
            ?.takeIf { it != destination || settingsHighlightKey != highlightKey }
            ?.let { settingsBackStack.add(it to settingsHighlightKey) }
        settingsDestination = destination
        settingsHighlightKey = highlightKey
    }

    fun restoreLastMainDestination(destination: NewUiDestination) {
        currentDestination = destination.takeIf { it.isMainDestination() }
            ?: NewUiDestination.Dialer
        settingsDestination = null
        settingsHighlightKey = null
        settingsBackStack.clear()
        backStack.clear()
        contactDetails = null
        contactDetailsPhoneNumber = null
        contactToEdit = null
        contactEditInitialPhone = null
        contactEditInitialName = null
        unknownNumber = null
        callDetails = null
    }

    fun openContactDetails(contact: Contact, phoneNumber: String? = null) {
        if (currentDestination != NewUiDestination.ContactDetails) {
            backStack += currentDestination
        }
        contactDetails = contact
        contactDetailsPhoneNumber = phoneNumber
        currentDestination = NewUiDestination.ContactDetails
    }

    fun openContactEdit(contact: Contact? = null) {
        if (currentDestination != NewUiDestination.ContactEdit) {
            backStack += currentDestination
        }
        contactToEdit = contact
        contactEditInitialPhone = null
        contactEditInitialName = null
        currentDestination = NewUiDestination.ContactEdit
    }

    fun openCreateContact(phoneNumber: String? = null, name: String? = null) {
        if (currentDestination != NewUiDestination.ContactEdit) {
            backStack += currentDestination
        }
        contactToEdit = null
        contactEditInitialPhone = phoneNumber
        contactEditInitialName = name
        currentDestination = NewUiDestination.ContactEdit
    }

    fun openDialer(number: String) {
        dialerInitialNumber = number
        navigate(NewUiDestination.Dialer)
    }

    fun consumeDialerInitialNumber() {
        dialerInitialNumber = null
    }

    fun openUnknownNumberDetails(number: String) {
        if (currentDestination != NewUiDestination.UnknownNumberDetails) {
            backStack += currentDestination
        }
        unknownNumber = number
        currentDestination = NewUiDestination.UnknownNumberDetails
    }

    fun openCallDetails(entry: CallLogEntry) {
        if (currentDestination != NewUiDestination.CallDetails) {
            backStack += currentDestination
        }
        callDetails = entry
        currentDestination = NewUiDestination.CallDetails
    }

    fun back(): Boolean {
        if (settingsDestination != null) {
            val previousSettingsDestination = settingsBackStack.removeLastOrNull()
            if (previousSettingsDestination != null) {
                settingsDestination = previousSettingsDestination.first
                settingsHighlightKey = previousSettingsDestination.second
                return true
            }
            settingsDestination = null
            settingsHighlightKey = null
            return true
        }
        if (currentDestination.isMainDestination()) {
            if (currentDestination == NewUiDestination.Dialer) return false
            currentDestination = NewUiDestination.Dialer
            backStack.clear()
            onMainDestinationChanged(currentDestination)
            return true
        }
        if (currentDestination == NewUiDestination.ContactDetails) {
            contactDetails = null
            contactDetailsPhoneNumber = null
        }
        if (currentDestination == NewUiDestination.ContactEdit) {
            contactToEdit = null
            contactEditInitialPhone = null
        }
        if (currentDestination == NewUiDestination.UnknownNumberDetails) {
            unknownNumber = null
        }
        if (currentDestination == NewUiDestination.CallDetails) {
            callDetails = null
        }
        val previous = backStack.removeLastOrNull() ?: return false
        currentDestination = previous
        return true
    }

    fun canGoBack(): Boolean =
        settingsDestination != null ||
            backStack.isNotEmpty() ||
            currentDestination != NewUiDestination.Dialer

}

internal object NewUiNavigationTiming {
    private const val TAG = "NewUiNavigationTiming"
    @Volatile private var recentsNavigationStartNanos = 0L

    fun recentsNavigationStarted() {
        recentsNavigationStartNanos = SystemClock.elapsedRealtimeNanos()
        Log.d(TAG, "Recents navigation requested")
    }

    fun recentsFirstCompositionCommitted() {
        val startedAt = recentsNavigationStartNanos
        if (startedAt == 0L) return
        recentsNavigationStartNanos = 0L
        val elapsedMs = (SystemClock.elapsedRealtimeNanos() - startedAt) / 1_000_000
        Log.d(TAG, "Recents first composition committed after ${elapsedMs}ms")
    }
}

enum class NewUiSettingsDestination(val title: String) {
    AppAndCallBehavior("App & Call Behavior"),
    SimAndCallPlacement("SIM & Call Placement"),
    CallAccounts("Call Accounts"),
    ColorsAndTheme("Themes"),
    SoundAndVibration("Sound & Vibration"),
    BiometricAndAppLock("Biometric & App Lock"),
    ContactsHider("Contacts Hider"),
    CallRecording("Call Recording"),
    RaiseToAnswer("Raise to Answer"),
    RainMode("Rain Mode"),
    VolumeDnd("Volume DND"),
    FakeCalls("Fake Calls"),
    BackupRestore("Backup & Restore"),
    NetworkSwitcher("Network Switcher"),
    About("About LibreDialer")
}

typealias NewUiSettingsNavigation = (NewUiSettingsDestination, String?) -> Unit

sealed interface NewUiNavigationRequest {
    data object OpenUniversalSearch : NewUiNavigationRequest

    data class OpenContacts(
        val groupId: String? = null,
        val clearGroupFilter: Boolean = false
    ) : NewUiNavigationRequest

    data class OpenSettings(
        val destination: NewUiSettingsDestination,
        val highlightKey: String? = null
    ) : NewUiNavigationRequest
}

object NewUiNavigationRequests {
    private val channel = Channel<NewUiNavigationRequest>(Channel.BUFFERED)
    internal val requests = channel.receiveAsFlow()

    fun openContacts(groupId: String? = null, clearGroupFilter: Boolean = false) {
        channel.trySend(NewUiNavigationRequest.OpenContacts(groupId, clearGroupFilter)).getOrThrow()
    }

    fun openUniversalSearch() {
        channel.trySend(NewUiNavigationRequest.OpenUniversalSearch).getOrThrow()
    }

    fun openSettings(destination: NewUiSettingsDestination, highlightKey: String? = null) {
        channel.trySend(NewUiNavigationRequest.OpenSettings(destination, highlightKey)).getOrThrow()
    }
}

@Composable
fun rememberNewUiNavigator(
    startDestination: NewUiDestination = NewUiDestination.Dialer,
    onMainDestinationChanged: (NewUiDestination) -> Unit = {}
): NewUiNavigator = remember(startDestination) {
    NewUiNavigator(startDestination, onMainDestinationChanged)
}

@Composable
fun NewUiHost(
    navigator: NewUiNavigator = rememberNewUiNavigator(),
    onCall: (String, String?) -> Unit,
    onOpenRecording: (String) -> Unit,
) {
    val destination = navigator.currentDestination
    val settingsDestination = navigator.settingsDestination
    val settingsHighlightKey = navigator.settingsHighlightKey
    val saveableStateHolder = rememberSaveableStateHolder()
    val spatialMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effectsMotion = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    AnimatedContent(
        targetState = Triple(destination, settingsDestination, settingsHighlightKey),
        transitionSpec = {
            (scaleIn(
                initialScale = 0.92f,
                animationSpec = spatialMotion
            ) + fadeIn(animationSpec = effectsMotion)) togetherWith
                (scaleOut(
                    targetScale = 0.96f,
                    animationSpec = spatialMotion
                ) + fadeOut(animationSpec = effectsMotion))
        },
        label = "newUiDestination"
    ) { (currentDestination, selectedSettingsDestination, selectedSettingsHighlightKey) ->
        saveableStateHolder.SaveableStateProvider(
            key = "$currentDestination:$selectedSettingsDestination:$selectedSettingsHighlightKey"
        ) {
            when (currentDestination) {
                NewUiDestination.Dialer -> NewDialerScreen(
                    onCall = { number -> onCall(number, null) },
                    onSettings = { navigator.navigate(NewUiDestination.Settings) },
                    onAddContact = navigator::openCreateContact,
                    initialNumber = navigator.dialerInitialNumber,
                    onInitialNumberConsumed = navigator::consumeDialerInitialNumber
                )
                NewUiDestination.Recents -> NewRecentsScreen(
                    onCallLogClick = navigator::openCallDetails,
                    onContactClick = navigator::openContactDetails,
                    onSettings = { navigator.navigate(NewUiDestination.Settings) },
                    onCall = { number -> onCall(number, null) },
                    onAddContact = navigator::openCreateContact
                )
                NewUiDestination.Contacts -> NewContactsScreen(
                    onContactClick = navigator::openContactDetails,
                    onCreateContact = { navigator.openContactEdit() },
                    onSettings = { navigator.navigate(NewUiDestination.Settings) }
                )
                NewUiDestination.ContactDetails -> {
                    val details = navigator.contactDetails
                    if (details != null) {
                        NewContactDetailsScreen(
                            contact = details,
                            phoneNumber = navigator.contactDetailsPhoneNumber,
                            onBack = navigator::back,
                            onEdit = navigator::openContactEdit,
                            onCall = { number, contactId -> onCall(number, contactId) }
                        )
                    }
                }
                NewUiDestination.UnknownNumberDetails -> {
                    val number = navigator.unknownNumber
                    if (number != null) {
                        NewContactDetailsScreen(
                            contact = Contact(
                                id = "unknown:$number",
                                name = number,
                                phoneNumbers = listOf(number)
                            ),
                            phoneNumber = number,
                            isUnknownNumber = true,
                            onBack = navigator::back,
                            onEdit = {},
                            onAddContact = { navigator.openCreateContact(number) },
                            onCall = { target, _ -> onCall(target, null) }
                        )
                    }
                }
                NewUiDestination.CallDetails -> {
                    val details = navigator.callDetails
                    if (details != null) {
                        NewCallDetailsScreen(
                            entry = details,
                            onCall = onCall,
                            onOpenContact = { selected, number ->
                                navigator.openContactDetails(selected, number)
                            },
                            onAddContact = navigator::openCreateContact
                        )
                    }
                }
                NewUiDestination.ContactEdit -> NewContactEditScreen(
                    contact = navigator.contactToEdit,
                    initialPhone = navigator.contactEditInitialPhone,
                    initialName = navigator.contactEditInitialName,
                    onSaved = navigator::back
                )
                NewUiDestination.UniversalSearch -> NewUniversalSearchScreen(
                    onBack = navigator::back,
                    onContactClick = navigator::openContactDetails,
                    onNumberClick = navigator::openDialer,
                    onUnknownNumberClick = navigator::openUnknownNumberDetails,
                    onCallLogClick = navigator::openCallDetails,
                    onRecordingClick = onOpenRecording,
                    onSettings = { destination, key ->
                        navigator.navigate(NewUiDestination.Settings)
                        navigator.navigateSettings(destination, key)
                    }
                )
                NewUiDestination.Settings -> NewSettingsScreen(
                    selectedDestination = selectedSettingsDestination,
                    selectedHighlightKey = selectedSettingsHighlightKey,
                    onNavigate = navigator::navigateSettings,
                    onBack = navigator::back
                )
            }
        }
    }
}

@Composable
fun NewUiAppShell(
    onCall: (String, String?) -> Unit,
    onOpenRecording: (String) -> Unit = {},
    launcherResetVersion: Int = 0,
    externalIntent: Intent? = null,
    onExternalCallIntent: (Intent) -> Boolean = { false },
    onResolveDialIntent: (Intent) -> String? = { null },
    onExternalIntentHandled: () -> Unit = {},
) {
    val prefs: PreferenceManager = org.koin.compose.koinInject()
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    val callLogs by callLogViewModel.allCallLogs.collectAsState()
    val startDestination = remember { prefs.lastMainDestination() }
    val navigator = rememberNewUiNavigator(startDestination) { destination ->
        prefs.setString(PreferenceManager.KEY_NEW_UI_LAST_MAIN_DESTINATION, destination.route)
    }
    LaunchedEffect(navigator) {
        NewUiNavigationRequests.requests.collect { request ->
            when (request) {
                NewUiNavigationRequest.OpenUniversalSearch -> navigator.navigate(NewUiDestination.UniversalSearch)
                is NewUiNavigationRequest.OpenContacts -> {
                    when {
                        request.groupId != null -> contactsViewModel.setGroupFilter(request.groupId)
                        request.clearGroupFilter -> contactsViewModel.clearFilters()
                    }
                    navigator.navigate(NewUiDestination.Contacts)
                }
                is NewUiNavigationRequest.OpenSettings -> {
                    navigator.navigate(NewUiDestination.Settings)
                    navigator.navigateSettings(request.destination, request.highlightKey)
                }
            }
        }
    }
    LaunchedEffect(launcherResetVersion) {
        if (launcherResetVersion > 0) {
            navigator.restoreLastMainDestination(prefs.lastMainDestination())
        }
    }
    LaunchedEffect(externalIntent) {
        val intent = externalIntent ?: return@LaunchedEffect
        val data = intent.data
        val action = intent.action
        val contactId = data?.lastPathSegment ?: intent.getStringExtra("contact_id")

        when {
            intent.getBooleanExtra("NAV_TO_RECENTS", false) ||
                action == "com.android.libredialer.OPEN_RECENTS" ||
                action == "com.android.libredialer.OPEN_CALL_LOGS_DETAIL" ||
                (action == Intent.ACTION_VIEW &&
                    (intent.type == "vnd.android.cursor.dir/calls" ||
                        data?.toString()?.contains("call_log") == true ||
                        data?.toString()?.contains("calls") == true)) -> {
                val phoneNumber = intent.getStringExtra("phone_number")
                val matchingEntry = callLogs.firstOrNull { entry ->
                    (!contactId.isNullOrBlank() && entry.contactId == contactId) ||
                        (!phoneNumber.isNullOrBlank() &&
                            numbersLikelyMatch(entry.number, phoneNumber))
                }
                if (matchingEntry != null && action == "com.android.libredialer.OPEN_CALL_LOGS_DETAIL") {
                    navigator.openCallDetails(matchingEntry)
                } else {
                    navigator.navigate(NewUiDestination.Recents)
                }
            }
            action == Intent.ACTION_CALL ||
                action == "android.intent.action.CALL_PRIVILEGED" ||
                ((action == Intent.ACTION_VIEW || action == Intent.ACTION_DIAL) &&
                    (intent.getBooleanExtra("android.intent.extra.CALL_NOW", false) ||
                        intent.getBooleanExtra("direct_call", false))) -> {
                val callStarted = onExternalCallIntent(intent)
                if (!callStarted && action != Intent.ACTION_CALL &&
                    action != "android.intent.action.CALL_PRIVILEGED" &&
                    data?.scheme == "tel"
                ) {
                    navigator.openDialer(data.schemeSpecificPart)
                }
            }
            action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW -> {
                if (action == Intent.ACTION_VIEW &&
                    (intent.type == "vnd.android.cursor.item/phone" ||
                        intent.type == "vnd.android.cursor.item/phone_v2")) {
                    if (!onExternalCallIntent(intent) && data?.scheme == "tel") {
                        navigator.openDialer(data.schemeSpecificPart)
                    }
                } else if (data?.scheme == "tel") {
                    navigator.openDialer(data.schemeSpecificPart)
                } else if (
                    data?.toString()?.contains("contacts") == true ||
                        data?.toString()?.contains("com.android.contacts") == true ||
                        intent.hasExtra("contact_id")
                ) {
                    val contact = contactId?.let {
                        withContext(Dispatchers.IO) { contactsViewModel.getContactById(it) }
                    }
                    if (contact != null) {
                        navigator.openContactDetails(contact)
                    } else {
                        navigator.navigate(NewUiDestination.Contacts)
                    }
                } else if (action == Intent.ACTION_DIAL) {
                    val number = onResolveDialIntent(intent)
                    if (number.isNullOrBlank()) {
                        navigator.navigate(NewUiDestination.Dialer)
                    } else {
                        navigator.openDialer(number)
                    }
                }
            }
            action == Intent.ACTION_INSERT -> {
                navigator.openCreateContact(
                    phoneNumber = intent.getStringExtra(ContactsContract.Intents.Insert.PHONE),
                    name = intent.getStringExtra(ContactsContract.Intents.Insert.NAME)
                )
            }
            action == Intent.ACTION_EDIT -> {
                val contact = contactId?.let {
                    withContext(Dispatchers.IO) { contactsViewModel.getContactById(it) }
                }
                if (contact != null) {
                    navigator.openContactEdit(contact)
                } else {
                    navigator.navigate(NewUiDestination.Contacts)
                }
            }
        }
        onExternalIntentHandled()
    }
    val predictiveBackEnabled = remember(prefs.settingsChanged.collectAsState().value) {
        prefs.getBoolean(PreferenceManager.KEY_PREDICTIVE_BACK_GESTURE, true)
    }
    var rawBackProgress by remember { mutableFloatStateOf(0f) }
    val backMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val tabDestinations = remember {
        listOf(
            NewUiDestination.Recents,
            NewUiDestination.Contacts,
            NewUiDestination.Dialer
        )
    }
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val backProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rawBackProgress,
        animationSpec = backMotion,
        label = "newUiBackProgress"
    )
    if (predictiveBackEnabled) {
        PredictiveBackHandler(enabled = navigator.canGoBack()) { progressFlow ->
            try {
                progressFlow.collect { event ->
                    rawBackProgress = event.progress
                }
                navigator.back()
                rawBackProgress = 0f
            } catch (_: kotlinx.coroutines.CancellationException) {
                rawBackProgress = 0f
            }
        }
    } else {
        BackHandler(enabled = navigator.canGoBack()) {
            navigator.back()
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalNewUiScrollClearance provides
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                NewUiNavigationBarScrollClearance
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .newUiScrollHaptics(
                    enabled = navigator.settingsDestination != NewUiSettingsDestination.CallRecording
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(navigator.currentDestination, navigator.settingsDestination) {
                        if (navigator.settingsDestination != null) return@pointerInput
                        val currentIndex = tabDestinations.indexOf(navigator.currentDestination)
                        if (currentIndex < 0) return@pointerInput

                        var horizontalDistance = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                horizontalDistance += dragAmount
                            },
                            onDragEnd = {
                                val distance = horizontalDistance
                                horizontalDistance = 0f
                                val destinationIndex = when {
                                    distance <= -swipeThresholdPx ->
                                        (currentIndex + 1).takeIf { it < tabDestinations.size }
                                    distance >= swipeThresholdPx ->
                                        (currentIndex - 1).takeIf { it >= 0 }
                                    else -> null
                                }
                                destinationIndex?.let { navigator.navigate(tabDestinations[it]) }
                            },
                            onDragCancel = { horizontalDistance = 0f }
                        )
                    }
                    .graphicsLayer {
                        val scale = 1f - backProgress * 0.06f
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                NewUiHost(
                    navigator = navigator,
                    onCall = onCall,
                    onOpenRecording = onOpenRecording,
                )
            }
            if (
                navigator.currentDestination == NewUiDestination.Dialer ||
                    navigator.currentDestination == NewUiDestination.Recents ||
                    navigator.currentDestination == NewUiDestination.Contacts
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    com.android.libredialer.view.newui.components.NewUiNavigationBar(navigator)
                }
            }
        }
    }
}
