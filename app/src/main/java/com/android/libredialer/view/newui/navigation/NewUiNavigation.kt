package com.android.libredialer.view.newui.navigation

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
import com.android.libredialer.view.newui.screens.NewFavoritesScreen
import com.android.libredialer.view.newui.screens.NewRecentsScreen
import com.android.libredialer.view.newui.screens.NewSettingsScreen
import com.android.libredialer.view.newui.components.LocalNewUiScrollClearance
import com.android.libredialer.view.newui.components.NewUiNavigationBarScrollClearance
import com.android.libredialer.view.newui.components.newUiScrollHaptics

@Immutable
enum class NewUiDestination(val route: String, val title: String) {
    Favorites("new/favorites", "Favorites"),
    Recents("new/recents", "Recents"),
    Contacts("new/contacts", "Contacts"),
    Dialer("new/dialer", "Dialer"),
    Settings("new/settings", "Settings"),
    ContactDetails("new/contact-details", "Contact details"),
    ContactEdit("new/contact-edit", "Edit contact"),
    CallDetails("new/call-details", "Call details"),
    UnknownNumberDetails("new/unknown-number-details", "Number details")
}

private fun NewUiDestination.isMainDestination(): Boolean = this in setOf(
    NewUiDestination.Recents,
    NewUiDestination.Favorites,
    NewUiDestination.Contacts,
    NewUiDestination.Settings,
    NewUiDestination.Dialer
)

private fun PreferenceManager.lastMainDestination(): NewUiDestination {
    val savedRoute = getString(PreferenceManager.KEY_NEW_UI_LAST_MAIN_DESTINATION, null)
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
    var contactDetails by mutableStateOf<Contact?>(null)
        private set
    var contactDetailsPhoneNumber by mutableStateOf<String?>(null)
        private set
    var contactToEdit by mutableStateOf<Contact?>(null)
        private set
    var contactEditInitialPhone by mutableStateOf<String?>(null)
        private set
    var unknownNumber by mutableStateOf<String?>(null)
        private set
    var callDetails by mutableStateOf<CallLogEntry?>(null)
        private set
    private val backStack = mutableListOf<NewUiDestination>()
    private val settingsBackStack = mutableListOf<NewUiSettingsDestination>()

    fun navigate(destination: NewUiDestination) {
        if (destination == currentDestination) {
            return
        }
        if (destination == NewUiDestination.Recents) {
            NewUiNavigationTiming.recentsNavigationStarted()
        }
        if (settingsDestination != null) {
            settingsDestination = null
            settingsBackStack.clear()
        }

        contactDetails = null
        contactDetailsPhoneNumber = null
        contactToEdit = null
        contactEditInitialPhone = null
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

    fun navigateSettings(destination: NewUiSettingsDestination) {
        settingsDestination?.takeIf { it != destination }?.let(settingsBackStack::add)
        settingsDestination = destination
    }

    fun restoreLastMainDestination(destination: NewUiDestination) {
        currentDestination = destination.takeIf { it.isMainDestination() }
            ?: NewUiDestination.Dialer
        settingsDestination = null
        settingsBackStack.clear()
        backStack.clear()
        contactDetails = null
        contactDetailsPhoneNumber = null
        contactToEdit = null
        contactEditInitialPhone = null
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
        currentDestination = NewUiDestination.ContactEdit
    }

    fun openCreateContact(phoneNumber: String? = null) {
        if (currentDestination != NewUiDestination.ContactEdit) {
            backStack += currentDestination
        }
        contactToEdit = null
        contactEditInitialPhone = phoneNumber
        currentDestination = NewUiDestination.ContactEdit
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
                settingsDestination = previousSettingsDestination
                return true
            }
            settingsDestination = null
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
    NetworkSwitcher("Network Switcher")
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
) {
    val destination = navigator.currentDestination
    val settingsDestination = navigator.settingsDestination
    val saveableStateHolder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = destination to settingsDestination,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "newUiDestination"
    ) { (currentDestination, selectedSettingsDestination) ->
        saveableStateHolder.SaveableStateProvider(
            key = "$currentDestination:$selectedSettingsDestination"
        ) {
            when (currentDestination) {
                NewUiDestination.Dialer -> NewDialerScreen(
                    onCall = { number -> onCall(number, null) },
                    onSettings = { navigator.navigate(NewUiDestination.Settings) },
                    onAddContact = navigator::openCreateContact
                )
                NewUiDestination.Recents -> NewRecentsScreen(
                    onCallLogClick = navigator::openCallDetails,
                    onSettings = { navigator.navigate(NewUiDestination.Settings) },
                    onCall = { number -> onCall(number, null) },
                    onAddContact = navigator::openCreateContact
                )
                NewUiDestination.Contacts -> NewContactsScreen(
                    onContactClick = navigator::openContactDetails,
                    onCreateContact = { navigator.openContactEdit() },
                    onSettings = { navigator.navigate(NewUiDestination.Settings) }
                )
                NewUiDestination.Favorites -> NewFavoritesScreen(
                    onContactClick = navigator::openContactDetails,
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
                    onSaved = navigator::back
                )
                NewUiDestination.Settings -> NewSettingsScreen(
                    selectedDestination = selectedSettingsDestination,
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
    launcherResetVersion: Int = 0,
) {
    val prefs: PreferenceManager = org.koin.compose.koinInject()
    val startDestination = remember { prefs.lastMainDestination() }
    val navigator = rememberNewUiNavigator(startDestination) { destination ->
        prefs.setString(PreferenceManager.KEY_NEW_UI_LAST_MAIN_DESTINATION, destination.route)
    }
    LaunchedEffect(launcherResetVersion) {
        if (launcherResetVersion > 0) {
            navigator.restoreLastMainDestination(prefs.lastMainDestination())
        }
    }
    val predictiveBackEnabled = remember(prefs.settingsChanged.collectAsState().value) {
        prefs.getBoolean(PreferenceManager.KEY_PREDICTIVE_BACK_GESTURE, true)
    }
    var rawBackProgress by remember { mutableFloatStateOf(0f) }
    val tabDestinations = remember {
        listOf(
            NewUiDestination.Favorites,
            NewUiDestination.Recents,
            NewUiDestination.Contacts,
            NewUiDestination.Dialer
        )
    }
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val backProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rawBackProgress,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
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
                        translationX = size.width * backProgress * 0.18f
                        scaleX = 1f - backProgress * 0.02f
                        scaleY = 1f - backProgress * 0.02f
                        alpha = 1f - backProgress * 0.08f
                    }
            ) {
                NewUiHost(
                    navigator = navigator,
                    onCall = onCall,
                )
            }
            if (
                navigator.currentDestination == NewUiDestination.Dialer ||
                navigator.currentDestination == NewUiDestination.Recents ||
                navigator.currentDestination == NewUiDestination.Contacts ||
                navigator.currentDestination == NewUiDestination.Favorites
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
