package com.android.libredialer.view.newui.screens

import android.telecom.Call
import android.telecom.CallAudioState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.coolappstore.evercallrecorder.by.svhp.services.recording.RecordingForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun NewCallScreen(
    call: Call,
    callState: Int,
    incomingCall: Call?,
    heldCall: Call?,
    audioState: CallAudioState?,
    contactsRepository: IContactsRepository,
    simSlot: Int = -1,
    showSimBadge: Boolean = false,
    onFinish: () -> Unit,
    onMoveToBackground: () -> Unit
) {
    val context = LocalContext.current
    val number = call.details?.handle?.schemeSpecificPart.orEmpty()
    var contactName by remember(number) { mutableStateOf(number.ifBlank { "Unknown caller" }) }
    var photoUri by remember(number) { mutableStateOf<String?>(null) }
    var showDialpad by remember { mutableStateOf(false) }
    var showAddCall by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var addCallNumber by remember { mutableStateOf("") }
    var dtmf by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }

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
        while (callState == Call.STATE_ACTIVE) {
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
    val bluetoothAvailable = (audioState?.supportedRouteMask ?: 0 and CallAudioState.ROUTE_BLUETOOTH) != 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(NewUiDimensions.PagePadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NewUiDimensions.ItemSpacing)
        ) {
            Text(
                text = when {
                    incomingRinging -> "Incoming call"
                    callState == Call.STATE_DIALING || callState == Call.STATE_CONNECTING -> "Calling"
                    callState == Call.STATE_HOLDING -> "On hold"
                    callState == Call.STATE_ACTIVE -> formatCallDuration(duration)
                    callState == Call.STATE_DISCONNECTED -> "Call ended"
                    else -> "Call"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(0.25f))
            if (photoUri != null) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControl(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        active = isMuted
                    ) { CallService.setMuted(!isMuted) }
                    CallControl(
                        icon = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        label = if (isSpeaker) "Earpiece" else "Speaker",
                        active = isSpeaker
                    ) {
                        CallService.setAudioRoute(
                            if (isSpeaker) CallAudioState.ROUTE_EARPIECE
                            else CallAudioState.ROUTE_SPEAKER
                        )
                    }
                    if (bluetoothAvailable) {
                        CallControl(
                            icon = Icons.Default.RecordVoiceOver,
                            label = if (isBluetooth) "Earpiece" else "Bluetooth",
                            active = isBluetooth
                        ) {
                            CallService.setAudioRoute(
                                if (isBluetooth) CallAudioState.ROUTE_EARPIECE
                                else CallAudioState.ROUTE_BLUETOOTH
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControl(Icons.Default.Dialpad, "Keypad", showDialpad) {
                        showDialpad = !showDialpad
                    }
                    CallControl(
                        if (callState == Call.STATE_HOLDING) Icons.Default.PlayArrow else Icons.Default.Pause,
                        if (callState == Call.STATE_HOLDING) "Resume" else "Hold",
                        callState == Call.STATE_HOLDING
                    ) {
                        runCatching {
                            if (callState == Call.STATE_HOLDING) call.unhold() else call.hold()
                        }
                    }
                    CallControl(
                        if (heldCall != null) Icons.Default.CallMerge else Icons.Default.AddIcCall,
                        if (heldCall != null) "Merge" else "Add call",
                        heldCall != null
                    ) {
                        if (heldCall != null) CallService.mergeCalls() else showAddCall = true
                    }
                }
                if (heldCall != null) {
                    OutlinedButton(onClick = { CallService.swapCalls() }) {
                        Text("Swap calls")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControl(Icons.Default.RecordVoiceOver, if (isRecording) "Stop" else "Record", isRecording) {
                        val action = if (isRecording) {
                            RecordingForegroundService.ACTION_STOP_RECORDING
                        } else RecordingForegroundService.ACTION_MANUAL_START
                        context.startService(
                            android.content.Intent(context, RecordingForegroundService::class.java)
                                .setAction(action)
                        )
                        isRecording = !isRecording
                    }
                    CallControl(Icons.Default.Person, "Note", showNote) {
                        showNote = true
                    }
                }
                IconButton(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            NoteManager.writeNote(context, contactName, number, noteText)
                        }
                        runCatching { call.disconnect() }
                        onFinish()
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CallEnd, "End call", tint = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }
        }
    }

    if (showDialpad) {
        DtmfDialog(
            value = dtmf,
            onDigit = { digit ->
                dtmf += digit
                runCatching { call.playDtmfTone(digit.first()) }
            },
            onDismiss = { showDialpad = false }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = if (active) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label)
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
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
