package com.android.libredialer.view.newui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiNavigator
import com.android.libredialer.view.newui.theme.NewUiDimensions
import com.android.libredialer.view.newui.motion.NewUiMotion
import com.android.libredialer.controller.util.PreferenceManager
import org.koin.compose.koinInject

val LocalNewUiScrollClearance = compositionLocalOf { 0.dp }

// Covers the 48 dp navigation item, capsule padding/margins, and an 8 dp scroll gap.
val NewUiNavigationBarScrollClearance = 88.dp

@Composable
fun newUiScrollContentPadding(existing: PaddingValues = PaddingValues()): PaddingValues {
    val clearance = LocalNewUiScrollClearance.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(existing, clearance, layoutDirection) {
        object : PaddingValues {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                existing.calculateLeftPadding(layoutDirection)

            override fun calculateTopPadding(): Dp = existing.calculateTopPadding()

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                existing.calculateRightPadding(layoutDirection)

            override fun calculateBottomPadding(): Dp =
                existing.calculateBottomPadding() + clearance
        }
    }
}

@Composable
fun Modifier.newUiScrollContentPadding(): Modifier =
    padding(bottom = LocalNewUiScrollClearance.current)

@Composable
fun Modifier.newUiClickable(
    shape: Shape,
    enabled: Boolean = true,
    pressedScale: Float = NewUiMotion.PressedScale,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = NewUiMotion.Interaction.animationSpec(),
        label = "newUiClickableScale"
    )
    return this
        .clip(shape)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun NewUiScreenShell(
    destination: NewUiDestination,
    showTitle: Boolean = true,
    titleOverride: String? = null,
    headerAction: (@Composable () -> Unit)? = null,
    sectionSpacing: androidx.compose.ui.unit.Dp = NewUiDimensions.SectionSpacing,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(NewUiDimensions.PagePadding)
        ) {
            content()
        }

        if (showTitle || headerAction != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NewUiDimensions.PagePadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTitle) {
                    Text(
                        titleOverride ?: destination.title,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                headerAction?.invoke()
            }
        }
    }
}

@Composable
fun Modifier.newUiScrollHaptics(enabled: Boolean = true): Modifier {
    val hapticFeedback = LocalHapticFeedback.current
    val prefs: PreferenceManager = koinInject()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val userEnabled = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_SCROLL_HAPTICS, false)
    }
    val milestonePx = with(LocalDensity.current) {
        (prefs.getFloat(PreferenceManager.KEY_SCROLL_CM_PER_HAPTIC, 1.5f).coerceIn(0.5f, 5f) * (160f / 2.54f)).dp.toPx()
    }
    val scrollHaptics = remember(hapticFeedback, milestonePx, enabled, userEnabled) {
        object : NestedScrollConnection {
            private var accumulatedScroll = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled || !userEnabled || source != NestedScrollSource.UserInput) {
                    accumulatedScroll = 0f
                    return Offset.Zero
                }
                accumulatedScroll += consumed.y
                if (kotlin.math.abs(accumulatedScroll) >= milestonePx) {
                    accumulatedScroll %= milestonePx
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                accumulatedScroll = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    return nestedScroll(scrollHaptics)
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
            .padding(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 12.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NewUiDestination.entries
                    .filter {
                        it != NewUiDestination.Settings &&
                            it != NewUiDestination.ContactDetails &&
                            it != NewUiDestination.ContactEdit &&
                            it != NewUiDestination.CallDetails &&
                            it != NewUiDestination.UnknownNumberDetails
                    }
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) NewUiMotion.PressedScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "newUiNavigationScale"
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
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
        NewUiDestination.ContactDetails -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
        NewUiDestination.ContactEdit -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
        NewUiDestination.CallDetails -> if (selected) Icons.Filled.History else Icons.Outlined.History
        NewUiDestination.UnknownNumberDetails -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
    }
