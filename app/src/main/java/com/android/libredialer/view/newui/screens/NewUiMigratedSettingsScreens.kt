package com.android.libredialer.view.newui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.controller.MissedCallPopupService
import com.android.libredialer.controller.RaiseToAnswerManager
import com.android.libredialer.controller.util.MissedCallBadgeManager
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.view.components.ContactsToDisplaySheet
import com.android.libredialer.view.components.SettingsSearchHeaderAction
import com.android.libredialer.view.components.SimColorsCustomizationDialog
import com.android.libredialer.view.components.settingsSearchHighlight
import com.android.libredialer.view.newui.components.NewUiScreenShell
import com.android.libredialer.view.newui.components.newUiClickable
import com.android.libredialer.view.newui.components.newUiSettingsHeaderContentTopPadding
import com.android.libredialer.view.newui.components.newUiScrollContentPadding
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiSettingsDestination
import com.ramcosta.composedestinations.generated.destinations.RainModeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RaiseToAnswerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SoundVibrationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VolumeDndScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun NewUiAppSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs: PreferenceManager = koinInject()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var highlightedKey by remember { mutableStateOf(highlightKey) }
    var proximityBg by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)) }
    var proximityOrientationBg by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_ORIENTATION_BG, false)) }
    var slantThreshold by remember {
        mutableFloatStateOf(
            prefs.getFloat(
                PreferenceManager.KEY_PROXIMITY_ORIENTATION_SLANT_THRESHOLD,
                PreferenceManager.DEFAULT_PROXIMITY_ORIENTATION_SLANT_THRESHOLD
            )
        )
    }
    var previewWouldTurnOff by remember { mutableStateOf(false) }
    var pocketModePrevention by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, false)) }
    var floatingCall by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_FLOATING_CALL, false)) }
    var directCallOnTap by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)) }
    var autoSpeaker by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_SPEAKER, false)) }
    val raiseToAnswerSupported = remember { RaiseToAnswerManager.hasRequiredSensors(context) }
    val raiseToAnswerEnabled = remember {
        prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ENABLED, false) && raiseToAnswerSupported
    }
    val rainModeEnabled = remember { prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, false) }
    var autoRedial by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_ENABLED, false)) }
    var missedCallPopupEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, false)) }
    var quickReply1 by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_1, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_1) ?: "")
    }
    var quickReply2 by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_2, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_2) ?: "")
    }
    var quickReply3 by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_3, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_3) ?: "")
    }
    var customFirst by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_CUSTOM_FIRST, false)) }
    var alwaysShowAfterCallEnds by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ALWAYS_SHOW_MISSED_CALL_POPUP_AFTER_CALL_END, false))
    }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var integrateNotes by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, true)) }
    var deleteNotesWithRecording by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DELETE_NOTES_WITH_RECORDING, false)) }

    DisposableEffect(proximityOrientationBg, slantThreshold) {
        if (!proximityOrientationBg) {
            previewWouldTurnOff = false
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val normOfG = kotlin.math.sqrt(ax * ax + ay * ay + az * az)
                if (normOfG > 0f) {
                    val nx = ax / normOfG
                    val ny = ay / normOfG
                    val nz = az / normOfG
                    val inclination = Math.toDegrees(kotlin.math.atan2(nx, ny).toDouble()).toInt()
                    val angleFromFlatDeg = Math.toDegrees(kotlin.math.acos(nz.coerceIn(-1f, 1f).toDouble()))
                    val angleFromFlatOrBelow = kotlin.math.min(angleFromFlatDeg, 180.0 - angleFromFlatDeg)
                    previewWouldTurnOff =
                        angleFromFlatOrBelow > slantThreshold.toDouble() && inclination in -90..90
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) canDrawOverlays = Settings.canDrawOverlays(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.CallMissed, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Display Over Other Apps") },
            text = {
                Text("To display the missed call popup over other apps when an incoming call is missed, Phone requires the 'Display over other apps' permission.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayPermissionDialog = false
                    prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, true)
                    try {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    } catch (_: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                }) { Text("Grant Permission") }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }

    NewUiScreenShell(
        NewUiDestination.Settings,
        titleOverride = "App & Call Behavior",
        headerAction = { SettingsSearchHeaderAction(navigator) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = newUiSettingsHeaderContentTopPadding(), bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NewUiSettingsSection("Call Behavior") {
                    NewUiSettingsSwitchRow(
                        "Device Orientation with Proximity Sensor",
                        "Uses raise-to-ear orientation together with the proximity sensor to prevent false screen-offs on sensitive proximity sensors.",
                        Icons.Outlined.ScreenLockPortrait,
                        proximityOrientationBg,
                        modifier = Modifier.settingsSearchHighlight("proximity_orientation_bg", highlightedKey) { highlightedKey = null }
                    ) {
                        proximityOrientationBg = it
                        prefs.setBoolean(PreferenceManager.KEY_PROXIMITY_ORIENTATION_BG, it)
                    }
                    AnimatedVisibility(
                        visible = proximityOrientationBg,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Slant sensitivity", style = MaterialTheme.typography.labelLarge)
                                TextButton(onClick = {
                                    slantThreshold = PreferenceManager.DEFAULT_PROXIMITY_ORIENTATION_SLANT_THRESHOLD
                                    prefs.setFloat(
                                        PreferenceManager.KEY_PROXIMITY_ORIENTATION_SLANT_THRESHOLD,
                                        slantThreshold
                                    )
                                }) { Text("Reset") }
                            }
                            Slider(
                                value = 115f - slantThreshold,
                                onValueChange = { slantThreshold = 115f - it },
                                onValueChangeFinished = {
                                    prefs.setFloat(PreferenceManager.KEY_PROXIMITY_ORIENTATION_SLANT_THRESHOLD, slantThreshold)
                                },
                                valueRange = 30f..85f,
                                steps = 10
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (previewWouldTurnOff) Color(0xFF2ECC71) else Color(0xFFE74C3C))
                                )
                                Text(
                                    if (previewWouldTurnOff) "Live test: screen would turn OFF right now"
                                    else "Live test: screen stays ON right now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    NewUiSettingsSwitchRow(
                        "Proximity Sensor on in background",
                        "Turn off screen when phone is near ear during a call",
                        Icons.Outlined.Sensors,
                        proximityBg,
                        modifier = Modifier.settingsSearchHighlight("proximity_sensor_bg", highlightedKey) { highlightedKey = null }
                    ) {
                        proximityBg = it
                        prefs.setBoolean(PreferenceManager.KEY_PROXIMITY_BG, it)
                    }
                    NewUiSettingsSwitchRow(
                        "Pocket Mode Prevention",
                        "Block accidental answer/decline when phone is in pocket",
                        Icons.Outlined.Sensors,
                        pocketModePrevention,
                        modifier = Modifier.settingsSearchHighlight("pocket_mode_prevention", highlightedKey) { highlightedKey = null }
                    ) {
                        pocketModePrevention = it
                        prefs.setBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, it)
                    }
                    NewUiSettingsSwitchRow(
                        "Floating Ongoing Call",
                        "Show a draggable floating bubble during calls. Requires Display over other apps permission.",
                        Icons.Outlined.Sensors,
                        floatingCall,
                        modifier = Modifier.settingsSearchHighlight("floating_ongoing_call", highlightedKey) { highlightedKey = null }
                    ) { enabled ->
                        if (enabled && !Settings.canDrawOverlays(context)) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } else {
                            floatingCall = enabled
                            prefs.setBoolean(PreferenceManager.KEY_FLOATING_CALL, enabled)
                        }
                    }
                    NewUiSettingsSwitchRow(
                        "Direct Call on Tap",
                        "Tap a call log entry to call directly instead of viewing contact info",
                        Icons.Filled.Call,
                        directCallOnTap,
                        modifier = Modifier.settingsSearchHighlight("direct_call_on_tap", highlightedKey) { highlightedKey = null }
                    ) {
                        directCallOnTap = it
                        prefs.setBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, it)
                    }
                    NewUiSettingsSwitchRow(
                        "Auto Speaker",
                        "Automatically use loudspeaker when the phone is away from your ear",
                        Icons.Filled.VolumeUp,
                        autoSpeaker,
                        modifier = Modifier.settingsSearchHighlight("auto_speaker", highlightedKey) { highlightedKey = null }
                    ) {
                        autoSpeaker = it
                        prefs.setBoolean(PreferenceManager.KEY_AUTO_SPEAKER, it)
                    }
                    NewUiSettingsActionRow(
                        "Raise to Answer",
                        if (!raiseToAnswerSupported) "Not supported on this device" else if (raiseToAnswerEnabled) "On" else "Off",
                        Icons.Filled.Vibration,
                        onClick = { navigator.navigate(RaiseToAnswerScreenDestination()) }
                    )
                    NewUiSettingsActionRow(
                        "Rain Mode",
                        if (rainModeEnabled) "On (Shake to answer/reject)" else "Off",
                        Icons.Filled.WaterDrop,
                        onClick = { navigator.navigate(RainModeScreenDestination()) }
                    )
                    NewUiSettingsSwitchRow(
                        "Auto Redial",
                        "When a call is rejected, unanswered, or busy, show an option to automatically redial",
                        Icons.Filled.Replay,
                        autoRedial,
                        modifier = Modifier.settingsSearchHighlight("auto_redial", highlightedKey) { highlightedKey = null }
                    ) {
                        autoRedial = it
                        prefs.setBoolean(PreferenceManager.KEY_AUTO_REDIAL_ENABLED, it)
                    }
                }
            }
            item {
                NewUiSettingsSection("Missed Call Popup") {
                    NewUiSettingsSwitchRow(
                        "Missed Call Popup",
                        "Show an interactive popup over other apps with caller info, quick responses, and social apps",
                        Icons.AutoMirrored.Filled.CallMissed,
                        missedCallPopupEnabled && canDrawOverlays,
                        modifier = Modifier.settingsSearchHighlight("missed_call_popup", highlightedKey) { highlightedKey = null }
                    ) { enabled ->
                        if (enabled) {
                            if (!Settings.canDrawOverlays(context)) {
                                showOverlayPermissionDialog = true
                            } else {
                                missedCallPopupEnabled = true
                                prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, true)
                            }
                        } else {
                            missedCallPopupEnabled = false
                            prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, false)
                        }
                    }
                    AnimatedVisibility(visible = missedCallPopupEnabled && canDrawOverlays) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Custom Quick Responses", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Customize the 3 quick reply messages. Leave a box empty to hide that response.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                quickReply1,
                                { quickReply1 = it; prefs.setString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_1, it) },
                                label = { Text("Response 1") },
                                placeholder = { Text(PreferenceManager.DEFAULT_MISSED_CALL_REPLY_1) },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                quickReply2,
                                { quickReply2 = it; prefs.setString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_2, it) },
                                label = { Text("Response 2") },
                                placeholder = { Text(PreferenceManager.DEFAULT_MISSED_CALL_REPLY_2) },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                quickReply3,
                                { quickReply3 = it; prefs.setString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_3, it) },
                                label = { Text("Response 3") },
                                placeholder = { Text(PreferenceManager.DEFAULT_MISSED_CALL_REPLY_3) },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            )
                            NewUiSettingsCheckboxRow(
                                "Put \"Type custom\" first",
                                "Show the custom message button first in the response list",
                                customFirst
                            ) {
                                customFirst = it
                                prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_CUSTOM_FIRST, it)
                            }
                            NewUiSettingsCheckboxRow(
                                "Always show missed call popup after every call ends",
                                "Show popup after every call ends with only the custom response option",
                                alwaysShowAfterCallEnds
                            ) {
                                alwaysShowAfterCallEnds = it
                                prefs.setBoolean(PreferenceManager.KEY_ALWAYS_SHOW_MISSED_CALL_POPUP_AFTER_CALL_END, it)
                            }
                            FilledTonalButton(
                                onClick = { MissedCallPopupService.previewLastMissedCall(context) },
                                modifier = Modifier.align(Alignment.End),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Filled.Visibility, null, Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Preview Popup", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            item {
                NewUiSettingsSection("More Settings") {
                    NewUiSettingsActionRow(
                        "4G/5G Switcher",
                        "Quickly toggle your network mode",
                        Icons.Filled.SignalCellularAlt,
                        modifier = Modifier.settingsSearchHighlight("network_switcher", highlightedKey) { highlightedKey = null },
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(context, com.supernova.networkswitch.presentation.ui.activity.MainActivity::class.java)
                                )
                            } catch (_: Exception) {}
                        }
                    )
                    NewUiSettingsActionRow(
                        "Volume DND",
                        "Toggle Do Not Disturb using a volume button sequence",
                        Icons.Filled.DoNotDisturbOn,
                        modifier = Modifier.settingsSearchHighlight("volume_dnd", highlightedKey) { highlightedKey = null },
                        onClick = { navigator.navigate(VolumeDndScreenDestination()) }
                    )
                    NewUiSettingsSwitchRow(
                        "Integrate Notes Section",
                        if (integrateNotes) "Call recording notes stay separate from the app's Notes section"
                        else "Call recording notes are merged into the app's Notes section",
                        Icons.Filled.Note,
                        integrateNotes,
                        modifier = Modifier.settingsSearchHighlight("integrate_notes", highlightedKey) { highlightedKey = null }
                    ) {
                        integrateNotes = it
                        prefs.setBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, it)
                    }
                    AnimatedVisibility(visible = integrateNotes) {
                        NewUiSettingsSwitchRow(
                            "Delete Notes With Recording",
                            "Also delete the linked note when its call recording is deleted",
                            Icons.Filled.DeleteSweep,
                            deleteNotesWithRecording,
                            modifier = Modifier.settingsSearchHighlight("delete_notes_with_recording", highlightedKey) { highlightedKey = null }
                        ) {
                            deleteNotesWithRecording = it
                            prefs.setBoolean(PreferenceManager.KEY_DELETE_NOTES_WITH_RECORDING, it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewUiSimAndCallPlacementScreen(navigator: DestinationsNavigator) {
    val prefs: PreferenceManager = koinInject()
    val context = LocalContext.current
    var showContactsSheet by remember { mutableStateOf(false) }
    var showSimDialog by remember { mutableStateOf(false) }
    var showSimColorDialog by remember { mutableStateOf(false) }
    var highlightedKey by remember { mutableStateOf<String?>(null) }
    var defaultSim by remember {
        val saved = prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, prefs.getDefaultSimIndexDefault())
        mutableIntStateOf(if (saved == 0 && prefs.getBoolean(PreferenceManager.KEY_USE_SIM_FROM_CALL_LOG, false)) 3 else saved)
    }
    val activeSimCount = remember { prefs.getActiveSimCount() }
    val hasTwoSims = remember {
        activeSimCount >= 2 || run {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
            try {
                (telecom?.callCapablePhoneAccounts?.size ?: 0) >= 2
            } catch (_: Throwable) {
                false
            }
        }
    }
    var showSimButtons by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_SIM_BUTTONS_IN_DIALPAD, false))
    }
    var confirmCall by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONFIRM_PLACING_CALL, false)) }
    var missedCallNotification by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATION, false))
    }

    if (showContactsSheet) {
        val contactsVM: ContactsViewModel = koinActivityViewModel()
        val accounts by contactsVM.availableAccounts.collectAsState()
        val groups by contactsVM.contactGroups.collectAsState()
        val selectedAccountKey by contactsVM.selectedAccountKey.collectAsState()
        val selectedGroupId by contactsVM.selectedGroupId.collectAsState()
        val contacts by contactsVM.allContacts.collectAsState()
        LaunchedEffect(Unit) {
            contactsVM.fetchAvailableAccounts()
            contactsVM.fetchContactGroups()
        }
        ContactsToDisplaySheet(
            accounts = accounts,
            groups = groups,
            selectedAccountKey = selectedAccountKey,
            selectedGroupId = selectedGroupId,
            totalCount = accounts.sumOf { it.contactCount }.takeIf { it > 0 } ?: contacts.size,
            onSelectAccount = {
                contactsVM.setAccountFilter(it)
                showContactsSheet = false
            },
            onSelectGroup = {
                contactsVM.setGroupFilter(it)
                showContactsSheet = false
            },
            onDismiss = { showContactsSheet = false },
            contactsVM = contactsVM
        )
    }
    if (showSimDialog) {
        NewUiDefaultSimDialog(
            selected = defaultSim,
            options = buildList {
                add(0 to "Ask every time")
                add(1 to "SIM 1")
                if (hasTwoSims) {
                    add(2 to "SIM 2")
                    add(3 to "Use SIM based on call logs")
                }
            },
            onDismiss = { showSimDialog = false },
            onSelect = {
                defaultSim = it
                prefs.setInt(PreferenceManager.KEY_DEFAULT_SIM, it)
                prefs.setBoolean(PreferenceManager.KEY_USE_SIM_FROM_CALL_LOG, it == 3)
                showSimDialog = false
            }
        )
    }
    if (showSimColorDialog) {
        SimColorsCustomizationDialog(onDismissRequest = { showSimColorDialog = false })
    }

    NewUiScreenShell(
        NewUiDestination.Settings,
        titleOverride = "SIM & Call Placement",
        headerAction = { SettingsSearchHeaderAction(navigator) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = newUiSettingsHeaderContentTopPadding(), bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NewUiSettingsSection("Accounts & Placement") {
                    NewUiSettingsActionRow(
                        "Default SIM",
                        when (defaultSim) {
                            1 -> "SIM 1"
                            2 -> "SIM 2"
                            3 -> "Use SIM based on call logs"
                            else -> "Ask every time"
                        },
                        Icons.Filled.SimCard,
                        trailing = true,
                        modifier = Modifier.settingsSearchHighlight(
                            if (highlightedKey == "use_sim_from_call_log") "use_sim_from_call_log" else "default_sim",
                            highlightedKey
                        ) { highlightedKey = null },
                        onClick = { showSimDialog = true }
                    )
                    NewUiSettingsSwitchRow(
                        "Confirm placing a call",
                        "Ask for confirmation before placing any outgoing call",
                        Icons.Filled.CheckCircle,
                        confirmCall
                    ) {
                        confirmCall = it
                        prefs.setBoolean(PreferenceManager.KEY_CONFIRM_PLACING_CALL, it)
                    }
                    if (hasTwoSims) {
                        NewUiSettingsSwitchRow(
                            "Show SIM buttons instead of dial button",
                            "Show SIM 1 and SIM 2 buttons on the dialpad so calls can be placed from either SIM.",
                            Icons.Outlined.Dialpad,
                            showSimButtons
                        ) {
                            showSimButtons = it
                            prefs.setBoolean(PreferenceManager.KEY_SHOW_SIM_BUTTONS_IN_DIALPAD, it)
                        }
                        NewUiSettingsActionRow(
                            "Customize SIM Colors",
                            "Choose custom colors for SIM 1 and SIM 2",
                            Icons.Filled.Palette,
                            trailing = true,
                            onClick = { showSimColorDialog = true }
                        )
                    }
                    NewUiSettingsActionRow(
                        "Contacts to display",
                        "Choose which accounts' contacts are shown",
                        Icons.Outlined.Contacts,
                        trailing = true,
                        modifier = Modifier.settingsSearchHighlight("contacts_to_display", highlightedKey) { highlightedKey = null },
                        onClick = { showContactsSheet = true }
                    )
                    NewUiSettingsSwitchRow(
                        "Missed Call Notification",
                        if (missedCallNotification) "Showing missed call notifications through Phone"
                        else "Missed call notifications disabled in Phone",
                        Icons.AutoMirrored.Filled.CallMissed,
                        missedCallNotification
                    ) {
                        missedCallNotification = it
                        prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATION, it)
                        if (it) {
                            MissedCallBadgeManager.updateBadge(context)
                        } else {
                            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager)
                                ?.cancel(MissedCallBadgeManager.MISSED_CALLS_NOTIF_ID)
                        }
                    }
                }
            }
            item {
                NewUiSettingsSection("Sound & Vibration") {
                    NewUiSettingsActionRow(
                        "Sound & Vibration",
                        "Ringtones and dialpad tones",
                        Icons.Filled.VolumeUp,
                        trailing = true,
                        modifier = Modifier.settingsSearchHighlight("sound_vibration_link", highlightedKey) { highlightedKey = null },
                        onClick = { navigator.navigate(SoundVibrationScreenDestination()) }
                    )
                }
            }
            item {
                NewUiSettingsSection("System Accounts") {
                    NewUiSettingsActionRow(
                        "Open System Additional Settings",
                        "Manage phone accounts in Android system settings",
                        Icons.Filled.Settings,
                        trailing = true,
                        onClick = {
                            try {
                                val intent = Intent().apply {
                                    component = ComponentName(
                                        "com.android.phone",
                                        "com.android.phone.settings.PhoneAccountSettingsActivity"
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Couldn't open system settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NewUiCallAccountsScreen() {
    NewUiScreenShell(NewUiDestination.Settings, titleOverride = "Call Accounts") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = newUiSettingsHeaderContentTopPadding(), bottom = 16.dp))
        ) {
            item {
                NewUiSettingsSection {
                    NewUiSettingsActionRow(
                        "Manage Calling Accounts",
                        "SIM cards and calling accounts",
                        Icons.Filled.SimCard,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun NewUiSettingsSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
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
private fun NewUiSettingsSwitchRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .newUiClickable(MaterialTheme.shapes.medium) { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NewUiSettingIcon(icon)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun NewUiSettingsActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    trailing: Boolean = false,
    modifier: Modifier = Modifier,
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
            NewUiSettingIcon(icon)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun NewUiSettingIcon(icon: ImageVector) {
    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
}

@Composable
private fun NewUiSettingsCheckboxRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .newUiClickable(MaterialTheme.shapes.medium) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NewUiDefaultSimDialog(
    selected: Int,
    options: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default SIM") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (index, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .newUiClickable(MaterialTheme.shapes.medium) { onSelect(index) }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == index, onClick = { onSelect(index) })
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}
