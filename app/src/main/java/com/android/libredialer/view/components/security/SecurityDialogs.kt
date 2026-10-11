package com.android.libredialer.view.components.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.libredialer.view.components.AppHapticEvent
import com.android.libredialer.view.components.rememberAppHapticFeedback

@Composable
fun PinDialogContent(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set PIN",
    isVerify: Boolean = false,
    expectedPin: String = "",
    showCloseButton: Boolean = true
) {
    var phase by remember { mutableIntStateOf(if (isVerify) 2 else 0) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var shakeState by remember { mutableIntStateOf(0) }
    val appHaptic = rememberAppHapticFeedback()
    val shake by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = keyframes {
            durationMillis = 400
            0.dp at 0; (-12).dp at 60; 12.dp at 120; (-8).dp at 200; 8.dp at 280; 0.dp at 400
        },
        label = "pinShake",
        finishedListener = { shakeState = 0 }
    )

    fun submit() {
        when {
            pin.length < 4 -> {
                shakeState++
                appHaptic(AppHapticEvent.LONG_PRESS, true)
            }
            isVerify -> {
                if (pin == expectedPin) {
                    appHaptic(AppHapticEvent.CLICK, true)
                    onConfirm(pin)
                } else {
                    pin = ""
                    shakeState++
                    appHaptic(AppHapticEvent.LONG_PRESS, true)
                }
            }
            phase == 0 -> {
                firstPin = pin
                pin = ""
                phase = 1
            }
            else -> {
                if (pin == firstPin) {
                    appHaptic(AppHapticEvent.CLICK, true)
                    onConfirm(pin)
                } else {
                    pin = ""
                    firstPin = ""
                    phase = 0
                    shakeState++
                    appHaptic(AppHapticEvent.LONG_PRESS, true)
                }
            }
        }
    }

    Column(
        Modifier.padding(horizontal = 20.dp, vertical = 24.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecurityIcon(Icons.Default.Lock)
                Column {
                    Text(
                        when {
                            isVerify -> title
                            phase == 0 -> "Set PIN"
                            else -> "Confirm PIN"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            isVerify -> "Enter your PIN to verify"
                            phase == 0 -> "At least 4 digits"
                            else -> "Re-enter to confirm"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showCloseButton) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(x = if (shakeState > 0) shake else 0.dp)
        ) {
            repeat(pin.length.coerceAtLeast(4).coerceAtMost(12)) { index ->
                val filled = index < pin.length
                val scale by animateFloatAsState(
                    if (filled) 1f else 0.4f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "pinDotScale$index"
                )
                val color by animateColorAsState(
                    if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    tween(150),
                    label = "pinDotColor$index"
                )
                Box(Modifier.size(20.dp).scale(scale).clip(CircleShape).background(color))
            }
        }
        Spacer(Modifier.height(44.dp))
        PinNumpad(
            onDigit = { if (pin.length < 12) pin += it },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            onSubmit = ::submit
        )
    }
}

@Composable
fun PinSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set PIN",
    isVerify: Boolean = false,
    expectedPin: String = "",
    showCloseButton: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = showCloseButton,
            dismissOnClickOutside = showCloseButton
        )
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            PinDialogContent(onConfirm, onDismiss, title, isVerify, expectedPin, showCloseButton)
        }
    }
}

@Composable
private fun PinNumpad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "→"))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                row.forEach { key ->
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        if (pressed) 0.85f else 1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "pinKeyScale$key"
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Surface(
                            onClick = {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    "→" -> onSubmit()
                                    else -> onDigit(key)
                                }
                            },
                            shape = CircleShape,
                            color = if (key == "→") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(64.dp).scale(scale),
                            interactionSource = interaction
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (key) {
                                    "⌫" -> Icon(Icons.Default.Backspace, null, Modifier.size(22.dp))
                                    "→" -> Icon(Icons.AutoMirrored.Filled.ArrowForward, "Confirm", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                    else -> Text(key, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
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
fun PasswordDialogContent(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set Password",
    isVerify: Boolean = false,
    expectedPassword: String = "",
    showCloseButton: Boolean = true
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecurityIcon(Icons.Default.Key)
                Column {
                    Text(if (isVerify) title else "Set Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (isVerify) "Enter your password to verify" else "Letters, numbers & symbols",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showCloseButton) IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
            }
        }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = "" },
            label = { Text(if (isVerify) "Password" else "Enter Password") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            isError = error.isNotEmpty()
        )
        if (!isVerify) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = "" },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                isError = error.isNotEmpty()
            )
        }
        AnimatedVisibility(error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showCloseButton) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            }
            Button(
                onClick = {
                    when {
                        isVerify && password != expectedPassword -> error = "Incorrect password"
                        isVerify -> onConfirm(password)
                        password.length < 4 -> error = "Password must be at least 4 characters"
                        password != confirmPassword -> error = "Passwords don't match"
                        else -> onConfirm(password)
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Confirm", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PasswordSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set Password",
    isVerify: Boolean = false,
    expectedPassword: String = "",
    showCloseButton: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = showCloseButton,
            dismissOnClickOutside = showCloseButton
        )
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            PasswordDialogContent(onConfirm, onDismiss, title, isVerify, expectedPassword, showCloseButton)
        }
    }
}

@Composable
private fun SecurityIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}
