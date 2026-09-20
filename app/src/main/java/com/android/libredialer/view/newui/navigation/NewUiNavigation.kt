package com.android.libredialer.view.newui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.libredialer.view.newui.screens.NewContactsScreen
import com.android.libredialer.view.newui.screens.NewDialerScreen
import com.android.libredialer.view.newui.screens.NewFavoritesScreen
import com.android.libredialer.view.newui.screens.NewRecentsScreen
import com.android.libredialer.view.newui.screens.NewSettingsScreen

@Immutable
enum class NewUiDestination(val route: String, val title: String) {
    Dialer("new/dialer", "Dialer"),
    Recents("new/recents", "Recents"),
    Contacts("new/contacts", "Contacts"),
    Favorites("new/favorites", "Favorites"),
    Settings("new/settings", "Settings")
}

@Stable
class NewUiNavigator(initialDestination: NewUiDestination) {
    var currentDestination by mutableStateOf(initialDestination)
        private set

    fun navigate(destination: NewUiDestination) {
        currentDestination = destination
    }
}

@Composable
fun rememberNewUiNavigator(
    startDestination: NewUiDestination = NewUiDestination.Dialer
): NewUiNavigator = remember(startDestination) { NewUiNavigator(startDestination) }

@Composable
fun NewUiHost(
    navigator: NewUiNavigator = rememberNewUiNavigator()
) {
    when (navigator.currentDestination) {
        NewUiDestination.Dialer -> NewDialerScreen()
        NewUiDestination.Recents -> NewRecentsScreen()
        NewUiDestination.Contacts -> NewContactsScreen()
        NewUiDestination.Favorites -> NewFavoritesScreen()
        NewUiDestination.Settings -> NewSettingsScreen()
    }
}
