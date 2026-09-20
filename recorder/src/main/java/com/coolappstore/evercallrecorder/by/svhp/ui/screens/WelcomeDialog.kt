package com.coolappstore.evercallrecorder.by.svhp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(380), label = "alpha")
    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.91f, animationSpec = tween(380), label = "scale")
    LaunchedEffect(Unit) { visible = true }

    val primary         = MaterialTheme.colorScheme.primary
    val primaryCont     = MaterialTheme.colorScheme.primaryContainer
    val secondaryCont   = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryCont    = MaterialTheme.colorScheme.tertiaryContainer
    val onPrimaryCont   = MaterialTheme.colorScheme.onPrimaryContainer
    val onSecondaryCont = MaterialTheme.colorScheme.onSecondaryContainer
    val surface         = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface       = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar    = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(32.dp))
                .background(surface)
        ) {
            Column {
                // ── Banner ────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(
                            Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.0f to primaryCont,
                                    0.5f to secondaryCont,
                                    1.0f to tertiaryCont
                                )
                            )
                        )
                ) {
                    // — Decorative circles (no blur, clean alpha shapes) —

                    // Large ring top-left
                    Box(
                        Modifier
                            .size(100.dp)
                            .offset(x = (-28).dp, y = (-28).dp)
                            .clip(CircleShape)
                            .background(onPrimaryCont.copy(alpha = 0.12f))
                    )
                    // Medium filled circle top-left inside
                    Box(
                        Modifier
                            .size(54.dp)
                            .offset(x = (-10).dp, y = (-10).dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.18f))
                    )
                    // Large ring bottom-right
                    Box(
                        Modifier
                            .size(90.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 26.dp, y = 26.dp)
                            .clip(CircleShape)
                            .background(onPrimaryCont.copy(alpha = 0.10f))
                    )
                    // Small accent circle top-right
                    Box(
                        Modifier
                            .size(26.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-22).dp, y = 20.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.22f))
                    )
                    // Tiny dot bottom-left
                    Box(
                        Modifier
                            .size(14.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 30.dp, y = (-18).dp)
                            .clip(CircleShape)
                            .background(onPrimaryCont.copy(alpha = 0.20f))
                    )

                    // Content centred
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(onPrimaryCont.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Celebration, null, tint = onPrimaryCont, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Welcome!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = onPrimaryCont, letterSpacing = (-0.4).sp)
                        Text("Ever Call Recorder", style = MaterialTheme.typography.labelSmall, color = onPrimaryCont.copy(alpha = 0.70f), letterSpacing = 0.8.sp)
                    }
                }

                // ── Body ──────────────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(2.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = CircleShape) {
                            Text("Continue", style = MaterialTheme.typography.labelLarge, color = onSurfaceVar)
                        }
                    }
                }
            }
        }
    }
}
