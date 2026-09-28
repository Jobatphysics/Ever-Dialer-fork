package com.android.libredialer.view.newui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.PhonePaused
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.libredialer.controller.RainModeManager
import com.android.libredialer.controller.RaiseToAnswerManager
import com.android.libredialer.controller.VolumeDndAccessibilityService
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.view.components.SettingsSearchEntryPoint
import com.android.libredialer.view.components.settingsSearchHighlight
import com.android.libredialer.view.newui.components.NewUiScreenShell
import com.android.libredialer.view.newui.components.newUiClickable
import com.android.libredialer.view.newui.components.newUiScrollContentPadding
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun NewUiRaiseToAnswerScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs: PreferenceManager = koinInject()
    val context = LocalContext.current
    var highlightedKey by remember { mutableStateOf(highlightKey) }
    val isSupported = remember { RaiseToAnswerManager.hasRequiredSensors(context) }
    val hasMagnetometer = remember { RaiseToAnswerManager.hasMagnetometer(context) }
    var enabled by remember {
        mutableStateOf(
            isSupported && prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ENABLED, false)
        )
    }
    var anyAngle by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ANY_ANGLE, false))
    }
    var declineByFlip by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_DECLINE_FLIP, false))
    }
    var beepFeedback by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_BEEP, true))
    }
    var vibrateFeedback by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_VIBRATE, false))
    }

    NewUiScreenShell(NewUiDestination.Settings, titleOverride = "Raise to Answer") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = 72.dp, bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettingsSearchEntryPoint(navigator) }
            if (!isSupported) {
                item {
                    NewUiMotionSection {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                "This device is missing the proximity or motion sensor required for Raise to Answer.",
                                Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                NewUiMotionSection("Raise to Answer") {
                    NewUiMotionSwitchRow(
                        "Enable Raise to Answer",
                        "Automatically answer incoming calls by raising the phone to your ear",
                        Icons.Outlined.Vibration,
                        enabled,
                        enabled = isSupported,
                        modifier = Modifier.settingsSearchHighlight("enable_raise_to_answer", highlightedKey) {
                            highlightedKey = null
                        }
                    ) {
                        if (isSupported) {
                            enabled = it
                            prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ENABLED, it)
                        }
                    }
                }
            }
            if (enabled && isSupported) {
                item {
                    NewUiMotionSection("Detection") {
                        NewUiMotionSwitchRow(
                            "Answer at Any Angle",
                            if (hasMagnetometer) {
                                "Use a simpler detection mode that doesn't require holding the phone upright"
                            } else {
                                "Always on — this device has no compass sensor for precise detection"
                            },
                            Icons.Outlined.FlipCameraAndroid,
                            anyAngle || !hasMagnetometer,
                            modifier = Modifier.settingsSearchHighlight("answer_any_angle", highlightedKey) {
                                highlightedKey = null
                            }
                        ) {
                            if (hasMagnetometer) {
                                anyAngle = it
                                prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ANY_ANGLE, it)
                            }
                        }
                        NewUiMotionSwitchRow(
                            "Decline by Flipping",
                            "Flip the phone face-down to decline an incoming call",
                            Icons.Outlined.FlipCameraAndroid,
                            declineByFlip,
                            modifier = Modifier.settingsSearchHighlight("decline_by_flipping", highlightedKey) {
                                highlightedKey = null
                            }
                        ) {
                            declineByFlip = it
                            prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_DECLINE_FLIP, it)
                        }
                    }
                }
                item {
                    NewUiMotionSection("Feedback") {
                        NewUiMotionSwitchRow(
                            "Beep Feedback",
                            "Play a short tone while sensors are tracking the gesture",
                            Icons.Outlined.VolumeUp,
                            beepFeedback,
                            modifier = Modifier.settingsSearchHighlight("raise_beep_feedback", highlightedKey) {
                                highlightedKey = null
                            }
                        ) {
                            beepFeedback = it
                            prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_BEEP, it)
                        }
                        NewUiMotionSwitchRow(
                            "Vibrate Feedback",
                            "Vibrate briefly while sensors are tracking the gesture",
                            Icons.Outlined.Vibration,
                            vibrateFeedback,
                            modifier = Modifier.settingsSearchHighlight("raise_vibrate_feedback", highlightedKey) {
                                highlightedKey = null
                            }
                        ) {
                            vibrateFeedback = it
                            prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_VIBRATE, it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewUiRainModeScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs: PreferenceManager = koinInject()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var highlightedKey by remember { mutableStateOf(highlightKey) }
    val hasAccelerometer = remember { RainModeManager.hasRequiredSensors(context) }
    var enabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, false))
    }
    var vibrateFeedback by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_VIBRATE, true))
    }
    var shakeIntensity by remember {
        mutableFloatStateOf(
            prefs.getFloat(
                PreferenceManager.KEY_RAIN_MODE_SHAKE_INTENSITY,
                PreferenceManager.DEFAULT_RAIN_MODE_SHAKE_INTENSITY
            )
        )
    }
    var incomingAction by remember {
        mutableStateOf(
            prefs.getString(
                PreferenceManager.KEY_RAIN_MODE_INCOMING_ACTION,
                PreferenceManager.DEFAULT_RAIN_MODE_INCOMING_ACTION
            ) ?: PreferenceManager.DEFAULT_RAIN_MODE_INCOMING_ACTION
        )
    }
    var endActiveCall by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_END_ACTIVE_CALL, true))
    }
    var showActionDialog by remember { mutableStateOf(false) }
    var lastLiveShakeTime by remember { mutableLongStateOf(0L) }
    var isShakingActive by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, shakeIntensity) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val detector = RainModeManager.ShakePatternDetector(shakeIntensity) {
            lastLiveShakeTime = System.currentTimeMillis()
            isShakingActive = true
            VolumeDndAccessibilityService.performVibration(context, longArrayOf(0, 70))
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) detector.processEvent(event)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            detector.reset()
            sensorManager?.unregisterListener(listener)
        }
    }
    LaunchedEffect(lastLiveShakeTime) {
        if (lastLiveShakeTime > 0L) {
            delay(1500L)
            isShakingActive = false
        }
    }

    NewUiScreenShell(NewUiDestination.Settings, titleOverride = "Rain Mode") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = 72.dp, bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettingsSearchEntryPoint(navigator) }
            if (!hasAccelerometer) {
                item {
                    NewUiMotionSection {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    "Accelerometer Sensor Missing",
                                    Modifier.padding(start = 12.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                "Rain Mode requires an accelerometer hardware sensor to detect shake gestures, but none was detected on this device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                NewUiMotionSection("Rain Mode") {
                    NewUiMotionSwitchRow(
                        "Enable Rain Mode",
                        "Shake your phone to answer ringing calls or decline/end active calls",
                        Icons.Outlined.WaterDrop,
                        enabled,
                        modifier = Modifier.settingsSearchHighlight("enable_rain_mode", highlightedKey) {
                            highlightedKey = null
                        }
                    ) {
                        enabled = it
                        prefs.setBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, it)
                    }
                }
            }
            if (enabled) {
                item {
                    NewUiMotionSection("Shake Sensitivity") {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Shake Intensity", style = MaterialTheme.typography.titleMedium)
                                    val percent = (shakeIntensity * 100).toInt()
                                    val threshold = RainModeManager.calculateThresholdG(shakeIntensity)
                                    val formatted = String.format(Locale.US, "%.1f", threshold)
                                    val label = when {
                                        shakeIntensity < 0.34f -> "Firm shake ($percent% • ~$formatted g)"
                                        shakeIntensity < 0.67f -> "Moderate shake ($percent% • ~$formatted g)"
                                        else -> "Gentle shake ($percent% • ~$formatted g)"
                                    }
                                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = {
                                    shakeIntensity = PreferenceManager.DEFAULT_RAIN_MODE_SHAKE_INTENSITY
                                    prefs.setFloat(PreferenceManager.KEY_RAIN_MODE_SHAKE_INTENSITY, shakeIntensity)
                                }) { Text("Reset") }
                            }
                            Slider(
                                value = shakeIntensity,
                                onValueChange = { shakeIntensity = it },
                                onValueChangeFinished = {
                                    prefs.setFloat(PreferenceManager.KEY_RAIN_MODE_SHAKE_INTENSITY, shakeIntensity)
                                },
                                valueRange = 0f..1f
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Firm (Strong)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Gentle (Light)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val testBackground by animateColorAsState(
                                if (isShakingActive) Color(0xFF2ECC71).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                label = "newRainTestBackground"
                            )
                            val testBorder by animateColorAsState(
                                if (isShakingActive) Color(0xFF2ECC71) else Color.Transparent,
                                label = "newRainTestBorder"
                            )
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = testBackground,
                                modifier = Modifier.fillMaxWidth().border(1.dp, testBorder, MaterialTheme.shapes.medium)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        Modifier.size(10.dp).clip(CircleShape).background(
                                            if (isShakingActive) Color(0xFF2ECC71) else MaterialTheme.colorScheme.outline
                                        )
                                    )
                                    Text(
                                        if (isShakingActive) "Shake Gesture Detected!"
                                        else "Live test: Shake left-right-left-right to test threshold",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isShakingActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isShakingActive) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    NewUiMotionSection("Call Actions & Feedback") {
                        NewUiMotionActionRow(
                            "Incoming Call Action",
                            if (incomingAction == "decline") "Decline incoming call on shake"
                            else "Answer incoming call on shake",
                            if (incomingAction == "decline") Icons.Outlined.CallEnd else Icons.Outlined.Call,
                            trailing = true,
                            modifier = Modifier.settingsSearchHighlight("rain_mode_action", highlightedKey) {
                                highlightedKey = null
                            },
                            onClick = { showActionDialog = true }
                        )
                        NewUiMotionSwitchRow(
                            "Shake to End Active Call",
                            "Shake your device while in an active call to hang up",
                            Icons.Outlined.PhonePaused,
                            endActiveCall,
                            modifier = Modifier.settingsSearchHighlight("rain_mode_end_active", highlightedKey) {
                                highlightedKey = null
                            }
                        ) {
                            endActiveCall = it
                            prefs.setBoolean(PreferenceManager.KEY_RAIN_MODE_END_ACTIVE_CALL, it)
                        }
                        NewUiMotionSwitchRow(
                            "Vibration Feedback",
                            "Vibrate when a call is answered or hung up via shake gesture",
                            Icons.Outlined.Vibration,
                            vibrateFeedback,
                            modifier = Modifier.settingsSearchHighlight("rain_mode_vibrate", highlightedKey) {
                                highlightedKey = null
                            }
                        ) {
                            vibrateFeedback = it
                            prefs.setBoolean(PreferenceManager.KEY_RAIN_MODE_VIBRATE, it)
                        }
                    }
                }
            }
        }
    }

    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            icon = { Icon(Icons.Outlined.Call, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Incoming Call Action") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("answer" to "Answer Call (Default)", "decline" to "Decline Call")
                        .forEach { (key, label) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .newUiClickable(MaterialTheme.shapes.medium) {
                                        incomingAction = key
                                        prefs.setString(PreferenceManager.KEY_RAIN_MODE_INCOMING_ACTION, key)
                                        showActionDialog = false
                                    }
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = incomingAction == key,
                                    onClick = {
                                        incomingAction = key
                                        prefs.setString(PreferenceManager.KEY_RAIN_MODE_INCOMING_ACTION, key)
                                        showActionDialog = false
                                    }
                                )
                                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                }
            },
            confirmButton = { TextButton(onClick = { showActionDialog = false }) { Text("Cancel") } },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }
}

@Composable
fun NewUiVolumeDndScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs: PreferenceManager = koinInject()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var highlightedKey by remember { mutableStateOf(highlightKey) }
    var enabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VOLUME_DND_ENABLED, false)) }
    var sequence by remember {
        mutableStateOf(
            prefs.getString(
                PreferenceManager.KEY_VOLUME_DND_SEQUENCE,
                PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
            ) ?: PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
        )
    }
    var lockScreenOnly by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, false))
    }
    var timeoutMs by remember {
        mutableStateOf(
            prefs.getInt(
                PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS,
                PreferenceManager.DEFAULT_VOLUME_DND_TIMEOUT_MS
            ).toString()
        )
    }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var accessibilityGranted by remember {
        mutableStateOf(VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context))
    }
    var dndGranted by remember { mutableStateOf(VolumeDndAccessibilityService.isDndAccessGranted(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted = VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)
                dndGranted = VolumeDndAccessibilityService.isDndAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showPermissionsDialog && (!accessibilityGranted || !dndGranted)) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            icon = { Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Permissions Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "To detect volume button sequences and toggle Do Not Disturb (DND), please grant the following permissions:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!accessibilityGranted) {
                        NewUiDndPermissionRow(
                            "Accessibility Service",
                            "Required to capture volume key combinations",
                            Icons.Outlined.Accessibility,
                            "Enable"
                        ) { VolumeDndAccessibilityService.openAccessibilitySettings(context) }
                    }
                    if (!dndGranted) {
                        NewUiDndPermissionRow(
                            "Do Not Disturb Access",
                            "Required to toggle system DND state",
                            Icons.Outlined.DoNotDisturbOn,
                            "Grant"
                        ) { VolumeDndAccessibilityService.openDndAccessSettings(context) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPermissionsDialog = false }) { Text("Done") } },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }

    NewUiScreenShell(NewUiDestination.Settings, titleOverride = "Volume DND") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = 72.dp, bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettingsSearchEntryPoint(navigator) }
            item {
                NewUiMotionSection("Volume DND") {
                    NewUiMotionSwitchRow(
                        "Volume DND",
                        "Toggle Do Not Disturb (DND) using a volume button sequence",
                        Icons.Outlined.DoNotDisturbOn,
                        enabled,
                        modifier = Modifier.settingsSearchHighlight("volume_dnd", highlightedKey) {
                            highlightedKey = null
                        }
                    ) { newValue ->
                        enabled = newValue
                        prefs.setBoolean(PreferenceManager.KEY_VOLUME_DND_ENABLED, newValue)
                        if (newValue) {
                            accessibilityGranted = VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)
                            dndGranted = VolumeDndAccessibilityService.isDndAccessGranted(context)
                            if (!accessibilityGranted || !dndGranted) showPermissionsDialog = true
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!accessibilityGranted || !dndGranted) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
                                        Text(
                                            "Permissions Required",
                                            Modifier.padding(start = 8.dp),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Text(
                                        "Volume DND requires Accessibility Service to capture volume button presses and Do Not Disturb permission to toggle DND.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!accessibilityGranted) {
                                            Button(
                                                onClick = { VolumeDndAccessibilityService.openAccessibilitySettings(context) },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Accessibility") }
                                        }
                                        if (!dndGranted) {
                                            Button(
                                                onClick = { VolumeDndAccessibilityService.openDndAccessSettings(context) },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("DND Access") }
                                        }
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Volume button combination", style = MaterialTheme.typography.titleMedium)
                                if (sequence != PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE) {
                                    TextButton(onClick = {
                                        sequence = PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
                                        prefs.setString(
                                            PreferenceManager.KEY_VOLUME_DND_SEQUENCE,
                                            PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
                                        )
                                    }) { Text("Reset (UUDD)") }
                                }
                            }
                            Text(
                                "Click volume buttons in this order with under ${timeoutMs}ms delay to trigger DND:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
                                    horizontalArrangement = if (sequence.isEmpty()) Arrangement.Center
                                    else Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (sequence.isEmpty()) {
                                        Text(
                                            "No keys added (tap below to add)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        sequence.forEach { key ->
                                            Surface(
                                                shape = MaterialTheme.shapes.medium,
                                                color = if (key == 'U') MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.secondary,
                                                contentColor = if (key == 'U') MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSecondary
                                            ) {
                                                Row(
                                                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        if (key == 'U') Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                                        null,
                                                        Modifier.size(14.dp)
                                                    )
                                                    Text(if (key == 'U') "UP" else "DOWN", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (sequence.length < 16) {
                                            sequence += "U"
                                            prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, sequence)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.ArrowUpward, null, Modifier.size(16.dp))
                                    Text("Vol Up", Modifier.padding(start = 4.dp), fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        if (sequence.length < 16) {
                                            sequence += "D"
                                            prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, sequence)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Outlined.ArrowDownward, null, Modifier.size(16.dp))
                                    Text("Vol Down", Modifier.padding(start = 4.dp), fontSize = 12.sp)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        if (sequence.isNotEmpty()) {
                                            sequence = sequence.dropLast(1)
                                            prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, sequence)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = sequence.isNotEmpty()
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(16.dp))
                                    Text("Delete", Modifier.padding(start = 4.dp), fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        "Trigger Delay Timeout",
                                        Modifier.padding(start = 10.dp),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(
                                        "${timeoutMs.ifEmpty { "600" }} ms",
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Text(
                                "Maximum delay in milliseconds between button clicks. Smaller numbers require faster clicks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val currentTimeout = timeoutMs.toFloatOrNull()?.coerceIn(100f, 3000f) ?: 600f
                            Slider(
                                value = currentTimeout,
                                onValueChange = { newMs ->
                                    val rounded = (newMs / 50).toInt() * 50
                                    timeoutMs = rounded.toString()
                                    prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, rounded)
                                },
                                valueRange = 100f..3000f
                            )
                            OutlinedTextField(
                                value = timeoutMs,
                                onValueChange = { input ->
                                    val digits = input.filter(Char::isDigit).take(5)
                                    timeoutMs = digits
                                    val number = digits.toIntOrNull()
                                    if (number != null && number in 100..5000) {
                                        prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, number)
                                    }
                                },
                                label = { Text("Delay (milliseconds)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    Text("ms", Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            )
                            val presets = listOf(
                                300 to "300ms (Fastest)",
                                400 to "400ms (Fast)",
                                600 to "600ms (Default)",
                                800 to "800ms (Normal)",
                                1200 to "1.2s (Relaxed)"
                            )
                            presets.forEach { (preset, label) ->
                                if (timeoutMs.toIntOrNull() == preset) {
                                    FilterChip(
                                        selected = true,
                                        onClick = {
                                            timeoutMs = preset.toString()
                                            prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, preset)
                                        },
                                        label = { Text(label) },
                                        leadingIcon = { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                    )
                                } else {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            timeoutMs = preset.toString()
                                            prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, preset)
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                NewUiMotionSection("Activation") {
                    NewUiMotionSwitchRow(
                        "Enable only in lock screen & screen off",
                        "Only triggers when screen is off or on lock screen; ignored on home screen",
                        Icons.Outlined.ScreenLockPortrait,
                        lockScreenOnly
                    ) {
                        lockScreenOnly = it
                        prefs.setBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, it)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewUiDndPermissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    actionLabel: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick) { Text(actionLabel) }
        }
    }
}

@Composable
private fun NewUiMotionSection(
    title: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun NewUiMotionSwitchRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .newUiClickable(MaterialTheme.shapes.medium, enabled = enabled) {
                    onCheckedChange(!checked)
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = if (enabled) onCheckedChange else null, enabled = enabled)
        }
        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NewUiMotionActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailing: Boolean = false,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .newUiClickable(MaterialTheme.shapes.medium, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing) {
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
