package com.android.libredialer.view.newui.screens

import androidx.compose.runtime.Composable
import com.android.libredialer.view.newui.components.NewUiPlaceholderCard
import com.android.libredialer.view.newui.components.NewUiScreenShell
import com.android.libredialer.view.newui.navigation.NewUiDestination

@Composable
fun NewDialerScreen() = NewDestinationShell(NewUiDestination.Dialer)

@Composable
fun NewRecentsScreen() = NewDestinationShell(NewUiDestination.Recents)

@Composable
fun NewContactsScreen() = NewDestinationShell(NewUiDestination.Contacts)

@Composable
fun NewFavoritesScreen() = NewDestinationShell(NewUiDestination.Favorites)

@Composable
fun NewSettingsScreen() = NewDestinationShell(NewUiDestination.Settings)

@Composable
private fun NewDestinationShell(destination: NewUiDestination) {
    NewUiScreenShell(destination = destination) {
        NewUiPlaceholderCard(destination)
    }
}
