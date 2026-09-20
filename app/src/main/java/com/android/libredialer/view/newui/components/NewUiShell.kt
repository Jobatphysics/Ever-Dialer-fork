package com.android.libredialer.view.newui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiNavigator
import com.android.libredialer.view.newui.theme.NewUiDimensions

@Composable
fun NewUiScreenShell(
    destination: NewUiDestination,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(NewUiDimensions.PagePadding),
        verticalArrangement = Arrangement.spacedBy(NewUiDimensions.SectionSpacing)
    ) {
        Text(destination.title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}

@Composable
fun NewUiPlaceholderCard(destination: NewUiDestination) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${destination.title} foundation",
            modifier = Modifier.padding(NewUiDimensions.CardPadding),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun NewUiNavigationBar(navigator: NewUiNavigator) {
    NavigationBar {
        NewUiDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = navigator.currentDestination == destination,
                onClick = { navigator.navigate(destination) },
                icon = {},
                label = { Text(destination.title) }
            )
        }
    }
}
