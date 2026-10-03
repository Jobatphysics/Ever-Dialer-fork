package com.android.libredialer.view.newui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

data class NewContextualAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun NewContextualActionsDialog(
    actions: List<NewContextualAction>,
    onDismiss: () -> Unit
) {
    val menuVisibility = remember { MutableTransitionState(false) }
    var isClosing by remember { mutableStateOf(false) }
    var visibleActionCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        menuVisibility.targetState = true
        (0..actions.size).forEach { index ->
            delay(if (index == 0) 30L else 42L)
            if (!menuVisibility.targetState) return@LaunchedEffect
            visibleActionCount = index + 1
        }
    }
    LaunchedEffect(isClosing) {
        if (isClosing) {
            snapshotFlow { menuVisibility.isIdle && !menuVisibility.currentState }
                .first { it }
            onDismiss()
        }
    }

    fun closeMenu() {
        if (!isClosing) {
            isClosing = true
            visibleActionCount = 0
            menuVisibility.targetState = false
        }
    }

    Dialog(onDismissRequest = ::closeMenu) {
        AnimatedVisibility(
            visibleState = menuVisibility,
            enter = scaleIn(
                animationSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialScale = 0.97f
            ) + fadeIn(animationSpec = tween(durationMillis = 120)),
            exit = scaleOut(
                animationSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                targetScale = 0.98f
            ) + fadeOut(animationSpec = tween(durationMillis = 110))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp - 48).coerceAtLeast(240).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions.forEachIndexed { index, action ->
                    key(action.label) {
                        val optionVisible =
                            menuVisibility.targetState && visibleActionCount > index
                        val optionAlpha by animateFloatAsState(
                            targetValue = if (optionVisible) 1f else 0f,
                            animationSpec = tween(durationMillis = 110),
                            label = "contextualActionAlpha"
                        )
                        val optionOffset by animateFloatAsState(
                            targetValue = if (optionVisible) 0f else 8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "contextualActionOffset"
                        )
                        val optionScale by animateFloatAsState(
                            targetValue = if (optionVisible) 1f else 0.94f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "contextualActionScale"
                        )
                        Surface(
                            onClick = {
                                if (!isClosing && optionVisible) {
                                    closeMenu()
                                    action.onClick()
                                }
                            },
                            enabled = !isClosing && optionVisible,
                            modifier = Modifier
                                .wrapContentWidth()
                                .heightIn(min = 56.dp)
                                .graphicsLayer {
                                    alpha = optionAlpha
                                    translationY = optionOffset * density
                                    scaleX = optionScale
                                    scaleY = optionScale
                                },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 12.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }

                val closeVisible =
                    menuVisibility.targetState && visibleActionCount > actions.size
                val closeAlpha by animateFloatAsState(
                    targetValue = if (closeVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 110),
                    label = "contextualActionCloseAlpha"
                )
                val closeScale by animateFloatAsState(
                    targetValue = if (closeVisible) 1f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "contextualActionCloseScale"
                )
                Surface(
                    onClick = ::closeMenu,
                    enabled = !isClosing && closeVisible,
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            alpha = closeAlpha
                            scaleX = closeScale
                            scaleY = closeScale
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close menu",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
