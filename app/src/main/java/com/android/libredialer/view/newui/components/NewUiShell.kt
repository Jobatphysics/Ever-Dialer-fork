package com.android.libredialer.view.newui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiNavigator
import com.android.libredialer.view.newui.theme.NewUiDimensions

@Composable
fun NewUiScreenShell(
    destination: NewUiDestination,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(NewUiDimensions.PagePadding),
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = NewUiDimensions.PagePadding, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .shadow(elevation = 8.dp, shape = MaterialTheme.shapes.extraLarge),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NewUiDestination.entries
                    .filter { it != NewUiDestination.Settings }
                    .forEach { destination ->
                    NewUiNavigationItem(
                        destination = destination,
                        selected = navigator.currentDestination == destination,
                        onClick = { navigator.navigate(destination) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NewUiNavigationItem(
    destination: NewUiDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "newUiNavigationScale"
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 4.dp,
                vertical = 5.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = destination.icon(selected),
                contentDescription = destination.title,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = destination.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

private fun NewUiDestination.icon(selected: Boolean) =
    when (this) {
        NewUiDestination.Dialer -> if (selected) Icons.Filled.Dialpad else Icons.Outlined.Dialpad
        NewUiDestination.Recents -> if (selected) Icons.Filled.History else Icons.Outlined.History
        NewUiDestination.Contacts -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
        NewUiDestination.Favorites -> if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
        NewUiDestination.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
