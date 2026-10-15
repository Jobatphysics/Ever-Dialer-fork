package com.android.libredialer.view.newui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.libredialer.R
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiNavigator
import com.android.libredialer.view.newui.theme.NewUiDimensions
import com.android.libredialer.view.newui.motion.NewUiMotion
import com.android.libredialer.controller.util.PreferenceManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.android.libredialer.view.components.liquidglass.LiquidGlassActionSurface
import com.android.libredialer.view.components.liquidglass.LiquidGlassHeadingSurface
import com.android.libredialer.view.components.liquidglass.LiquidGlassNavigationSurface
import com.android.libredialer.view.components.liquidglass.LiquidGlassTabSelectionIndicator
import com.android.libredialer.view.components.liquidglass.LocalLiquidGlassBackdropSource
import com.android.libredialer.view.components.liquidglass.rememberLiquidGlassTint
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

val LocalNewUiScrollClearance = compositionLocalOf { 0.dp }
internal val LocalNewUiScreenTargetContent = compositionLocalOf { true }
internal val LocalNewUiHeaderAlpha = compositionLocalOf { 1f }

// Covers the 48 dp navigation item, capsule padding/margins, and an 8 dp scroll gap.
val NewUiNavigationBarScrollClearance = 104.dp

@Composable
fun newUiSettingsHeaderContentTopPadding(): Dp {
    val density = LocalDensity.current
    val systemTopInset = with(density) {
        WindowInsets.statusBars.union(WindowInsets.displayCutout).getTop(this).toDp()
    }
    return systemTopInset + 64.dp
}

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
    contentUnderStatusBar: Boolean = false,
    sectionSpacing: androidx.compose.ui.unit.Dp = NewUiDimensions.SectionSpacing,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val topSystemInset = with(LocalDensity.current) {
            WindowInsets.statusBars.union(WindowInsets.displayCutout).getTop(this).toFloat()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (contentUnderStatusBar || destination == NewUiDestination.Settings) {
                        Modifier.drawWithContent {
                            clipRect(top = topSystemInset) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    } else {
                        Modifier.statusBarsPadding()
                    }
                )
                .padding(NewUiDimensions.PagePadding)
        ) {
            content()
        }

        val glassBackdropSource = LocalView.current
        if ((showTitle || headerAction != null) && LocalNewUiScreenTargetContent.current) {
            Popup(
                alignment = Alignment.TopCenter,
                properties = PopupProperties(
                    focusable = false,
                    clippingEnabled = false
                )
            ) {
                val headerAlpha = LocalNewUiHeaderAlpha.current
                CompositionLocalProvider(
                    LocalLiquidGlassBackdropSource provides glassBackdropSource
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = headerAlpha
                        }
                    ) {
                        if (destination == NewUiDestination.Contacts) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(NewUiDimensions.PagePadding)
                            ) {
                                if (showTitle) {
                                    LiquidGlassHeadingSurface(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .wrapContentWidth(),
                                        cornerRadius = 28.dp,
                                        backdropSource = glassBackdropSource
                                    ) {
                                        Text(
                                            titleOverride ?: destination.title,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            )
                                        )
                                    }
                                }

                                headerAction?.let { action ->
                                    Box(
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        action()
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(NewUiDimensions.PagePadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showTitle) {
                                    LiquidGlassHeadingSurface(
                                        modifier = Modifier.wrapContentWidth(),
                                        cornerRadius = 28.dp,
                                        backdropSource = glassBackdropSource
                                    ) {
                                        Text(
                                            titleOverride ?: destination.title,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            )
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }

                                if (showTitle) {
                                    Spacer(Modifier.weight(1f))
                                }

                                headerAction?.let { action ->
                                    LiquidGlassActionSurface(
                                        modifier = Modifier.size(48.dp),
                                        cornerRadius = 24.dp,
                                        backdropSource = glassBackdropSource,
                                        fallbackColor = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        action()
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
fun NewUiSettingsAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun Modifier.newUiScrollHaptics(enabled: Boolean = true): Modifier {
    val hapticFeedback = com.android.libredialer.view.components.rememberComposeAppHaptic()
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
                    hapticFeedback(HapticFeedbackType.TextHandleMove)
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
fun NewUiNavigationBar(
    navigator: NewUiNavigator,
    backdropSource: android.view.View,
    dynamicBackdrop: Boolean = false
) {
    val glassTintAlpha = rememberLiquidGlassTint()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassNavigationSurface(
                modifier = Modifier.weight(1f),
                itemSpacing = 2.dp,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                backdropSource = backdropSource,
                dynamicBackground = dynamicBackdrop,
                tintAlpha = glassTintAlpha
            ) {
                val destinations = remember {
                    listOf(
                        NewUiDestination.Recents,
                        NewUiDestination.Contacts,
                        NewUiDestination.Dialer
                    )
                }
                val tabBounds = remember {
                    mutableStateListOf<Rect?>().apply {
                        repeat(destinations.size) { add(null) }
                    }
                }
                var indicatorInitialized by remember { mutableStateOf(false) }
                var indicatorAnimating by remember { mutableStateOf(false) }
                var indicatorX by remember { mutableFloatStateOf(0f) }
                var indicatorY by remember { mutableFloatStateOf(0f) }
                var indicatorScaleX by remember { mutableFloatStateOf(1f) }
                var indicatorScaleY by remember { mutableFloatStateOf(1f) }
                val selectedIndex = destinations.indexOf(navigator.currentDestination)
                    .coerceAtLeast(0)

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val density = LocalDensity.current
                    val rowWidthPx = with(density) { maxWidth.toPx() }
                    val targetBounds = tabBounds.getOrNull(selectedIndex)

                    LaunchedEffect(selectedIndex, targetBounds, rowWidthPx) {
                        indicatorAnimating = false
                        val target = targetBounds ?: return@LaunchedEffect
                        val targetX = target.left
                        val targetY = target.top
                        if (!indicatorInitialized) {
                            indicatorX = targetX
                            indicatorY = targetY
                            indicatorInitialized = true
                            return@LaunchedEffect
                        }

                        val startX = indicatorX
                        val distance = targetX - startX
                        val width = target.width
                        val stretch = 0.22f * min(
                            1f,
                            abs(distance) / (width * 3f).coerceAtLeast(1f)
                        )
                        indicatorY = targetY
                        if (abs(distance) < 0.5f) {
                            indicatorX = targetX
                            indicatorScaleX = 1f
                            indicatorScaleY = 1f
                            return@LaunchedEffect
                        }

                        // Motion follows LiquidGlassTabBar v2.0.11 (QWEA0, MIT), adapted to Compose.
                        var startTimeNanos: Long? = null
                        indicatorAnimating = true
                        try {
                            while (true) {
                                val frameTimeNanos = withFrameNanos { it }
                                val startTime = startTimeNanos ?: frameTimeNanos.also {
                                    startTimeNanos = it
                                }
                                val progress = (
                                    (frameTimeNanos - startTime) / 380_000_000f
                                    ).coerceIn(0f, 1f)
                                val overshootProgress = liquidTabOvershoot(progress)
                                val stretchPulse = sin(
                                    Math.PI.toFloat() * min(progress * 1.15f, 1f)
                                )
                                indicatorScaleX = 1f + stretch * stretchPulse
                                indicatorScaleY = 1f - stretch * 0.55f * stretchPulse
                                indicatorX = clampTabIndicatorX(
                                    startX + distance * overshootProgress,
                                    indicatorScaleX,
                                    width,
                                    rowWidthPx
                                )
                                if (progress >= 1f) {
                                    indicatorX = targetX
                                    indicatorScaleX = 1f
                                    indicatorScaleY = 1f
                                    break
                                }
                            }
                        } finally {
                            indicatorAnimating = false
                        }
                    }

                    if (targetBounds != null && indicatorInitialized) {
                        LiquidGlassTabSelectionIndicator(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        indicatorX.roundToInt(),
                                        indicatorY.roundToInt()
                                    )
                                }
                                .size(
                                    with(density) { targetBounds.width.toDp() },
                                    with(density) { targetBounds.height.toDp() }
                                )
                                .graphicsLayer {
                                    scaleX = indicatorScaleX
                                    scaleY = indicatorScaleY
                                },
                            backdropSource = LocalLiquidGlassBackdropSource.current
                                ?: backdropSource,
                            dynamicBackground = dynamicBackdrop || indicatorAnimating,
                            tintAlpha = glassTintAlpha
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        destinations.forEachIndexed { index, destination ->
                            NewUiNavigationItem(
                                destination = destination,
                                selected = navigator.currentDestination == destination,
                                onClick = { navigator.navigate(destination) },
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coordinates ->
                                        val position = coordinates.positionInParent()
                                        val size = coordinates.size
                                        tabBounds[index] = Rect(
                                            position.x,
                                            position.y,
                                            position.x + size.width,
                                            position.y + size.height
                                        )
                                    }
                            )
                        }
                    }
                }
            }

            LiquidGlassActionSurface(
                modifier = Modifier.size(72.dp),
                cornerRadius = 36.dp,
                backdropSource = backdropSource,
                fallbackColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                IconButton(
                    onClick = { navigator.navigate(NewUiDestination.UniversalSearch) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun liquidTabOvershoot(progress: Float): Float {
    val shifted = progress - 1f
    return shifted * shifted * (2.1f * shifted + 1.1f) + 1f
}

private fun clampTabIndicatorX(
    x: Float,
    scaleX: Float,
    indicatorWidth: Float,
    rowWidth: Float
): Float {
    val bulge = (scaleX - 1f) * indicatorWidth / 2f
    val minX = bulge
    val maxX = (rowWidth - indicatorWidth - bulge).coerceAtLeast(minX)
    return x.coerceIn(minX, maxX)
}

@Composable
private fun NewUiNavigationItem(
    destination: NewUiDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 4.dp,
                vertical = 6.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val navigationIconPainter = when (destination) {
                NewUiDestination.Recents -> painterResource(
                    if (selected) R.drawable.ic_nav_calls_unselected
                    else R.drawable.ic_nav_calls_selected
                )
                else -> null
            }
            if (navigationIconPainter != null) {
                Icon(
                    painter = navigationIconPainter,
                    contentDescription = destination.title,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = destination.icon(selected),
                    contentDescription = destination.title,
                    modifier = Modifier.size(28.dp)
                )
            }
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
        NewUiDestination.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
        NewUiDestination.ContactDetails -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
        NewUiDestination.ContactEdit -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
        NewUiDestination.CallDetails -> if (selected) Icons.Filled.History else Icons.Outlined.History
        NewUiDestination.UnknownNumberDetails -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
        NewUiDestination.UniversalSearch -> Icons.Filled.Search
    }
