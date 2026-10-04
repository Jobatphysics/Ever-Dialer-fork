package com.android.libredialer.view.newui.screens

import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.libredialer.controller.CallService
import com.android.libredialer.controller.util.NoteManager
import com.android.libredialer.controller.util.makeCall
import com.android.libredialer.modal.`interface`.IContactsRepository
import com.android.libredialer.view.newui.theme.NewUiDimensions
import com.android.libredialer.view.newui.components.NewUiDialerButton
import com.android.libredialer.view.newui.motion.NewUiMotion
import com.coolappstore.evercallrecorder.by.svhp.services.recording.RecordingForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NewCallScreen(
    call: Call,
    callState: Int,
    incomingCall: Call?,
    heldCall: Call?,
    audioState: CallAudioState?,
    contactsRepository: IContactsRepository,
    callingCardUri: String? = null,
    simSlot: Int = -1,
    showSimBadge: Boolean = false,
    onMoveToBackground: () -> Unit
) {
    val context = LocalContext.current
    val displayCall = incomingCall?.takeIf { it.state == Call.STATE_RINGING } ?: call
    val number = displayCall.details?.handle?.schemeSpecificPart.orEmpty()
    var contactName by remember(number) { mutableStateOf(number.ifBlank { "Unknown caller" }) }
    var photoUri by remember(number) { mutableStateOf<String?>(null) }
    var callingCardLoadFailed by remember(callingCardUri) { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }
    var showAddCall by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var addCallNumber by remember { mutableStateOf("") }
    var dtmf by remember { mutableStateOf("") }
    var dtmfToneJob by remember { mutableStateOf<Job?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    val dtmfScope = rememberCoroutineScope()

    LaunchedEffect(number) {
        if (number.isNotBlank()) {
            val contact = withContext(Dispatchers.IO) {
                runCatching { contactsRepository.getContactByNumber(number) }.getOrNull()
            }
            if (contact != null) {
                contactName = contact.name
                photoUri = contact.photoUri
            }
        }
    }
    LaunchedEffect(callState, call) {
        while (callState == Call.STATE_ACTIVE || callState == Call.STATE_HOLDING) {
            val connectedAt = call.details?.connectTimeMillis ?: 0L
            duration = if (connectedAt > 0L) {
                ((System.currentTimeMillis() - connectedAt) / 1000L).coerceAtLeast(0L)
            } else 0L
            delay(1000L)
        }
    }

    BackHandler {
        if (showDialpad) showDialpad = false else onMoveToBackground()
    }

    val incomingRinging = incomingCall?.state == Call.STATE_RINGING
    val isMuted = audioState?.isMuted == true
    val isSpeaker = audioState?.route == CallAudioState.ROUTE_SPEAKER
    val isBluetooth = audioState?.route == CallAudioState.ROUTE_BLUETOOTH
    val bluetoothAvailable =
        ((audioState?.supportedRouteMask ?: 0) and CallAudioState.ROUTE_BLUETOOTH) != 0
    val canHoldCall = callState == Call.STATE_HOLDING ||
        (callState == Call.STATE_ACTIVE &&
            ((call.details?.callCapabilities ?: 0) and Call.Details.CAPABILITY_HOLD) != 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(NewUiDimensions.PagePadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NewUiDimensions.ItemSpacing)
        ) {
            Spacer(Modifier.weight(1f))
            if (!callingCardUri.isNullOrBlank() && !callingCardLoadFailed) {
                AsyncImage(
                    model = callingCardUri,
                    contentDescription = "$contactName calling card",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 224.dp, height = 248.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    onError = { callingCardLoadFailed = true }
                )
            } else if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(112.dp).clip(CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(112.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Text(
                contactName,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                number.ifBlank { "Private number" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when {
                    incomingRinging -> "Incoming call"
                    callState == Call.STATE_DIALING -> "Calling"
                    callState == Call.STATE_CONNECTING -> "Connecting"
                    callState == Call.STATE_HOLDING -> "On hold · ${formatCallDuration(duration)}"
                    callState == Call.STATE_ACTIVE -> formatCallDuration(duration)
                    callState == Call.STATE_DISCONNECTING -> "Ending call"
                    callState == Call.STATE_DISCONNECTED -> "Call ended"
                    else -> "Call"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showSimBadge && simSlot >= 0) {
                Text(
                    "SIM ${simSlot + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.weight(1f))
            if (incomingRinging) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    FilledTonalButton(onClick = { CallService.declineCall() }) {
                        Icon(Icons.Default.CallEnd, null)
                        Text(" Decline")
                    }
                    Button(onClick = { CallService.answerCall() }) {
                        Icon(Icons.Default.CallMerge, null)
                        Text(" Answer")
                    }
                }
            } else {
                if (heldCall != null) {
                    OutlinedButton(onClick = { CallService.swapCalls() }) {
                        Text("Swap calls")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallControl(
                        Icons.Default.Mic,
                        Icons.Default.MicOff,
                        if (isMuted) "Unmute" else "Mute",
                        isMuted
                    ) {
                        CallService.setMuted(!isMuted)
                    }
                    CallControl(Icons.Default.VolumeDown, Icons.Default.VolumeUp, "Speaker", isSpeaker) {
                        CallService.setAudioRoute(
                            if (isSpeaker) CallAudioState.ROUTE_EARPIECE
                            else CallAudioState.ROUTE_SPEAKER
                        )
                    }
                    CallControl(
                        Icons.Default.BluetoothDisabled,
                        Icons.Default.Bluetooth,
                        "Bluetooth",
                        isBluetooth,
                        enabled = bluetoothAvailable,
                        containerColor = if (bluetoothAvailable) null else MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = if (bluetoothAvailable) null else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    ) {
                        CallService.setAudioRoute(
                            if (isBluetooth) CallAudioState.ROUTE_EARPIECE
                            else CallAudioState.ROUTE_BLUETOOTH
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallControl(Icons.Default.Dialpad, Icons.Default.Dialpad, "Keypad", showDialpad) {
                        showDialpad = !showDialpad
                    }
                    CallControl(
                        if (callState == Call.STATE_HOLDING) Icons.Default.PlayArrow else Icons.Default.Pause,
                        if (callState == Call.STATE_HOLDING) Icons.Default.PlayArrow else Icons.Default.Pause,
                        if (callState == Call.STATE_HOLDING) "Resume" else "Hold",
                        callState == Call.STATE_HOLDING,
                        enabled = canHoldCall
                    ) {
                        try {
                            when (call.state) {
                                Call.STATE_HOLDING -> call.unhold()
                                Call.STATE_ACTIVE -> call.hold()
                            }
                        } catch (error: Exception) {
                            Log.e("NewCallScreen", "Telecom rejected hold/resume request", error)
                            Toast.makeText(context, "Unable to change call hold state", Toast.LENGTH_SHORT).show()
                        }
                    }
                    CallControl(
                        if (heldCall != null) Icons.Default.CallMerge else Icons.Default.AddIcCall,
                        if (heldCall != null) Icons.Default.CallMerge else Icons.Default.AddIcCall,
                        if (heldCall != null) "Merge calls" else "Add call",
                        heldCall != null
                    ) {
                        if (heldCall != null) CallService.mergeCalls() else showAddCall = true
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallControl(
                        Icons.Default.FiberManualRecord,
                        Icons.Default.FiberManualRecord,
                        if (isRecording) "Stop" else "Record",
                        isRecording,
                        diameter = 98.dp
                    ) {
                        val action = if (isRecording) {
                            RecordingForegroundService.ACTION_STOP_RECORDING
                        } else RecordingForegroundService.ACTION_MANUAL_START
                        try {
                            context.startService(
                                android.content.Intent(context, RecordingForegroundService::class.java)
                                    .setAction(action)
                            )
                            isRecording = !isRecording
                        } catch (error: Exception) {
                            Log.e("NewCallScreen", "Unable to send recording command", error)
                            Toast.makeText(context, "Unable to change recording state", Toast.LENGTH_SHORT).show()
                        }
                    }
                    CallControl(
                        Icons.Default.CallEnd,
                        Icons.Default.CallEnd,
                        "End call",
                        active = true,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        diameter = 98.dp,
                        iconSize = 52.dp,
                        pressedScale = NewUiMotion.PressedScale
                    ) {
                        if (noteText.isNotBlank()) {
                            NoteManager.writeNote(context, contactName, number, noteText)
                        }
                        try {
                            call.disconnect()
                        } catch (error: Exception) {
                            Log.e("NewCallScreen", "Telecom rejected end-call request", error)
                            Toast.makeText(context, "Unable to end call", Toast.LENGTH_SHORT).show()
                        }
                    }
                    CallControl(Icons.Default.Person, Icons.Default.EditNote, "Note", showNote) {
                        showNote = true
                    }
                }
                }
                Spacer(Modifier.weight(0.2f))
            }
        }
    }

    if (showDialpad) {
        DtmfDialog(
            value = dtmf,
            onDigit = { digit ->
                dtmf += digit
                dtmfToneJob?.cancel()
                dtmfToneJob = dtmfScope.launch {
                    try {
                        call.stopDtmfTone()
                        call.playDtmfTone(digit.first())
                        delay(150L)
                        call.stopDtmfTone()
                    } catch (error: Exception) {
                        Log.e("NewCallScreen", "Unable to send DTMF tone", error)
                    }
                }
            },
            onDismiss = {
                dtmfToneJob?.cancel()
                try {
                    call.stopDtmfTone()
                } catch (error: Exception) {
                    Log.e("NewCallScreen", "Unable to stop DTMF tone", error)
                }
                showDialpad = false
            }
        )
    }
    if (showAddCall) {
        AlertDialog(
            onDismissRequest = { showAddCall = false },
            title = { Text("Add call") },
            text = {
                TextField(
                    value = addCallNumber,
                    onValueChange = { addCallNumber = it },
                    label = { Text("Phone number") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAddCall = false
                    if (addCallNumber.isNotBlank()) {
                        runCatching { call.hold() }
                        makeCall(context, addCallNumber.trim(), call.details?.accountHandle)
                    }
                }) { Text("Call") }
            },
            dismissButton = { TextButton(onClick = { showAddCall = false }) { Text("Cancel") } }
        )
    }
    if (showNote) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text("Call note") },
            text = {
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteText.isNotBlank()) NoteManager.writeNote(context, contactName, number, noteText)
                    showNote = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNote = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CallControl(
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    diameter: androidx.compose.ui.unit.Dp = 90.dp,
    pressedScale: Float = 0.80f,
    iconSize: androidx.compose.ui.unit.Dp = 42.dp,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NewUiDialerButton(
            onClick = onClick,
            containerColor = containerColor ?: if (active) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = contentColor ?: MaterialTheme.colorScheme.onSurface,
            enabled = enabled,
            diameter = diameter,
            pressedScale = pressedScale
        ) {
            Icon(
                imageVector = if (active) icon else inactiveIcon,
                contentDescription = label,
                modifier = Modifier.size(iconSize)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun DtmfDialog(value: String, onDigit: (String) -> Unit, onDismiss: () -> Unit) {
    val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keypad") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value.ifBlank { " " }, style = MaterialTheme.typography.titleLarge)
                digits.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { digit ->
                            OutlinedButton(
                                onClick = { onDigit(digit) },
                                modifier = Modifier.widthIn(min = 72.dp)
                            ) { Text(digit) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

private fun formatCallDuration(seconds: Long): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
