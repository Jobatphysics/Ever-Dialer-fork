package com.android.libredialer.view.newui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import com.android.libredialer.controller.util.PreferenceManager
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
        if (destination == currentDestination) {
            return
        }
        if (settingsDestination != null) {
            settingsDestination = null
        }
        if (currentDestination != destination) {
            backStack += currentDestination
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

    fun canGoBack(): Boolean = settingsDestination != null || backStack.isNotEmpty()
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
                    onCall = onCall,
                    onSettings = { navigator.navigate(NewUiDestination.Settings) }
                )
                NewUiDestination.Recents -> NewRecentsScreen(onCallLogClick = onCallLogClick)
                NewUiDestination.Contacts -> NewContactsScreen(onContactClick = onContactClick)
                NewUiDestination.Favorites -> NewFavoritesScreen(onContactClick = onContactClick)
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
    onCall: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    onCallLogClick: (CallLogEntry) -> Unit
) {
    val navigator = rememberNewUiNavigator()
    val prefs: PreferenceManager = org.koin.compose.koinInject()
    val predictiveBackEnabled = remember(prefs.settingsChanged.collectAsState().value) {
        prefs.getBoolean(PreferenceManager.KEY_PREDICTIVE_BACK_GESTURE, true)
    }
    var rawBackProgress by remember { mutableFloatStateOf(0f) }
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
                onContactClick = onContactClick,
                onCallLogClick = onCallLogClick
            )
        }
    }
}
