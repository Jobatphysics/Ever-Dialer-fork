package com.android.libredialer.view.newui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.modal.data.CallLogEntry
import com.android.libredialer.view.newui.screens.NewContactsScreen
import com.android.libredialer.view.newui.screens.NewDialerScreen
import com.android.libredialer.view.newui.screens.NewFavoritesScreen
import com.android.libredialer.view.newui.screens.NewRecentsScreen
import com.android.libredialer.view.newui.screens.NewSettingsScreen

@Immutable
enum class NewUiDestination(val route: String, val title: String) {
    Favorites("new/favorites", "Favorites"),
    Recents("new/recents", "Recents"),
    Contacts("new/contacts", "Contacts"),
    Dialer("new/dialer", "Dialer"),
    Settings("new/settings", "Settings")
}

@Stable
class NewUiNavigator(initialDestination: NewUiDestination) {
    var currentDestination by mutableStateOf(initialDestination)
        private set
    var settingsDestination by mutableStateOf<NewUiSettingsDestination?>(null)
        private set
    private val backStack = mutableListOf<NewUiDestination>()

    fun navigate(destination: NewUiDestination) {
        if (destination == NewUiDestination.Settings && currentDestination != destination) {
            backStack += currentDestination
        } else if (destination != NewUiDestination.Settings) {
            backStack.clear()
            settingsDestination = null
        }
        currentDestination = destination
    }

    fun navigateSettings(destination: NewUiSettingsDestination) {
        settingsDestination = destination
    }

    fun back(): Boolean {
        if (settingsDestination != null) {
            settingsDestination = null
            return true
        }
        val previous = backStack.removeLastOrNull() ?: return false
        currentDestination = previous
        return true
    }
}

enum class NewUiSettingsDestination(val title: String) {
    AppAndCallBehavior("App & Call Behavior"),
    SimAndCallPlacement("SIM & Call Placement"),
    CallAccounts("Call Accounts"),
    ColorsAndTheme("Colors & Theme"),
    SoundAndVibration("Sound & Vibration"),
    BiometricAndAppLock("Biometric & App Lock"),
    ContactsHider("Contacts Hider"),
    CallRecording("Call Recording"),
    RaiseToAnswer("Raise to Answer"),
    RainMode("Rain Mode"),
    VolumeDnd("Volume DND"),
    NetworkSwitcher("Network Switcher"),
    Updates("Updates"),
    About("About")
}

@Composable
fun rememberNewUiNavigator(
    startDestination: NewUiDestination = NewUiDestination.Dialer
): NewUiNavigator = remember(startDestination) { NewUiNavigator(startDestination) }

@Composable
fun NewUiHost(
    navigator: NewUiNavigator = rememberNewUiNavigator(),
    onCall: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    onCallLogClick: (CallLogEntry) -> Unit,
) {
    when (navigator.currentDestination) {
        NewUiDestination.Dialer -> NewDialerScreen(
            onCall = onCall,
            onSettings = { navigator.navigate(NewUiDestination.Settings) }
        )
        NewUiDestination.Recents -> NewRecentsScreen(onCallLogClick = onCallLogClick)
        NewUiDestination.Contacts -> NewContactsScreen(onContactClick = onContactClick)
        NewUiDestination.Favorites -> NewFavoritesScreen(onContactClick = onContactClick)
        NewUiDestination.Settings -> NewSettingsScreen(
            selectedDestination = navigator.settingsDestination,
            onNavigate = navigator::navigateSettings
        )
    }
}

@Composable
fun NewUiAppShell(
    onCall: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    onCallLogClick: (CallLogEntry) -> Unit
) {
    val navigator = rememberNewUiNavigator()
    BackHandler(enabled = navigator.currentDestination == NewUiDestination.Settings) {
        navigator.back()
    }
    androidx.compose.material3.Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        bottomBar = {
            com.android.libredialer.view.newui.components.NewUiNavigationBar(navigator)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            NewUiHost(
                navigator = navigator,
                onCall = onCall,
                onContactClick = onContactClick,
                onCallLogClick = onCallLogClick
            )
        }
    }
}
