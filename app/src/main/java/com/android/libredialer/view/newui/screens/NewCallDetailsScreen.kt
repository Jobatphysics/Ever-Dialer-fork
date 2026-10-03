package com.android.libredialer.view.newui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.libredialer.controller.CallLogViewModel
import com.android.libredialer.view.components.providedMessageIcon
import com.android.libredialer.view.components.providedCallIcon
import com.android.libredialer.view.components.providedVideoCallIcon
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.controller.util.BlockedNumbersManager
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.controller.util.formatDate
import com.android.libredialer.controller.util.isGoogleMeetInstalled
import com.android.libredialer.controller.util.isTelegramInstalled
import com.android.libredialer.controller.util.isWhatsAppBusinessInstalled
import com.android.libredialer.controller.util.isWhatsAppInstalled
import com.android.libredialer.controller.util.numbersLikelyMatch
import com.android.libredialer.controller.util.startGoogleMeetVideoCall
import com.android.libredialer.controller.util.startTelegramVideoCall
import com.android.libredialer.controller.util.startWhatsAppBusinessVideoCall
import com.android.libredialer.controller.util.startWhatsAppVideoCall
import com.android.libredialer.modal.data.CallLogEntry
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.view.newui.components.newUiScrollContentPadding
import com.android.libredialer.view.newui.theme.NewUiDimensions
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun NewCallDetailsScreen(
    entry: CallLogEntry,
    onCall: (String, String?) -> Unit,
    onOpenContact: (Contact, String) -> Unit,
    onAddContact: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val prefs = koinInject<PreferenceManager>()
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val logs by callLogViewModel.allCallLogs.collectAsState()
    val contacts by contactsViewModel.allContacts.collectAsState()
    val contact = remember(contacts, entry) {
        contacts.firstOrNull { it.id == entry.contactId } ?: contacts.firstOrNull {
            it.phoneNumbers.any { number -> numbersLikelyMatch(number, entry.number) }
        }
    }
    val history = remember(logs, contact, entry.number) {
        logs.filter { log ->
            (contact != null && log.contactId == contact.id) ||
                numbersLikelyMatch(log.number, entry.number)
        }.sortedByDescending { it.date }
    }
    var blocked by remember(entry.number) {
        mutableStateOf(BlockedNumbersManager.isBlocked(context, prefs, entry.number))
    }
    var showBlockDialog by remember { mutableStateOf(false) }
    val displayName = contact?.name?.takeIf { it.isNotBlank() } ?: entry.number
    val videoApp = remember(context) {
        when {
            isWhatsAppInstalled(context) -> "whatsapp"
            isWhatsAppBusinessInstalled(context) -> "whatsapp_business"
            isTelegramInstalled(context) -> "telegram"
            isGoogleMeetInstalled(context) -> "googlemeet"
            else -> null
        }
    }
    val email = contact?.emails?.firstOrNull(String::isNotBlank)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = NewUiDimensions.PagePadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(
                PaddingValues(top = 12.dp, bottom = 24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NewUiContactAvatar(
                        contact?.photoUri ?: entry.photoUri,
                        displayName,
                        stableContactId = contact?.id
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    if (contact == null) {
                        Spacer(Modifier.size(4.dp))
                        Text(
                            callTypeLabel(entry.type),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (entry.number.isNotBlank()) {
                        PrimaryContactAction(
                            icon = providedCallIcon(),
                            label = "Call",
                            enabled = true,
                            onClick = { onCall(entry.number, contact?.id) },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryContactAction(
                            icon = providedMessageIcon(),
                            label = "Message",
                            enabled = true,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("sms:${entry.number}"))
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (videoApp != null && entry.number.isNotBlank()) {
                        PrimaryContactAction(
                            icon = providedVideoCallIcon(),
                            label = "Video",
                            enabled = true,
                            onClick = {
                                val started = when (videoApp) {
                                    "whatsapp" -> startWhatsAppVideoCall(context, entry.number)
                                    "whatsapp_business" -> startWhatsAppBusinessVideoCall(context, entry.number)
                                    "telegram" -> startTelegramVideoCall(context, entry.number)
                                    else -> startGoogleMeetVideoCall(context, entry.number)
                                }
                                if (!started) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Video call could not be started",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (email != null) {
                        PrimaryContactAction(
                            icon = Icons.Default.Email,
                            label = "Email",
                            enabled = true,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                val secondaryActions = listOf(
                    ContactActionTile(Icons.Default.ContentCopy, "Copy", "Copy number") {
                        clipboard.setText(AnnotatedString(entry.number))
                    },
                    ContactActionTile(Icons.Default.Share, "Share", "Share number") {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, entry.number)
                        }
                        context.startActivity(Intent.createChooser(share, "Share number"))
                    },
                    ContactActionTile(
                        Icons.Default.Block,
                        if (blocked) "Unblock" else "Block",
                        if (blocked) "Unblock number" else "Block number"
                    ) { showBlockDialog = true },
                    if (contact == null) {
                        ContactActionTile(Icons.Default.PersonAdd, "Add", "Add to Contacts") {
                            onAddContact(entry.number)
                        }
                    } else {
                        ContactActionTile(Icons.Default.ContactPage, "Contact", "View contact") {
                            onOpenContact(contact, entry.number)
                        }
                    }
                )
                ActionTileRow(*secondaryActions.toTypedArray())
            }
            item {
                DetailsCard("Call information") {
                    if (contact != null) {
                        DetailRow(entry.number, "Phone number", providedCallIcon())
                    }
                    if (contact != null) {
                        DetailRow(callTypeLabel(entry.type), "Call type", callTypeIcon(entry.type))
                    }
                    DetailRow(formatDate(entry.date), "Date and time", Icons.Default.Event)
                    DetailRow(durationLabel(entry.duration), "Duration", Icons.Default.History)
                    if (entry.simSlot >= 0) {
                        DetailRow("SIM ${entry.simSlot + 1}", "Call placement", providedCallIcon())
                    }
                }
            }
            item {
                Text(
                    "Call history",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (history.isEmpty()) {
                item {
                    Text(
                        "No call history found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(history, key = { it.callIds.firstOrNull() ?: "${it.number}:${it.date}" }) {
                    CallHistoryRow(it)
                }
            }
        }
    }
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(if (blocked) "Unblock number?" else "Block number?") },
            text = {
                Text(
                    if (blocked) "Calls and messages will be allowed again."
                    else "Calls and messages will be blocked."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (blocked) BlockedNumbersManager.unblock(context, prefs, entry.number)
                    else BlockedNumbersManager.block(context, prefs, entry.number)
                    blocked = !blocked
                    showBlockDialog = false
                }) { Text(if (blocked) "Unblock" else "Block") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CallHistoryRow(entry: CallLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                callTypeIcon(entry.type),
                callTypeLabel(entry.type),
                tint = callTypeColor(entry.type)
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(callTypeLabel(entry.type))
                Text(
                    formatDate(entry.date),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                durationLabel(entry.duration),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun callTypeColor(type: Int) = when (type) {
    CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun callTypeIcon(type: Int): ImageVector = when (type) {
    CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
    CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
    CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
    else -> Icons.Default.Call
}

private fun callTypeLabel(type: Int): String = when (type) {
    CallLog.Calls.INCOMING_TYPE -> "Incoming"
    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
    CallLog.Calls.MISSED_TYPE -> "Missed"
    CallLog.Calls.REJECTED_TYPE -> "Rejected"
    else -> "Call"
}

private fun durationLabel(duration: Long): String {
    val minutes = duration / 60
    val seconds = duration % 60
    return if (minutes > 0) "%d:%02d".format(minutes, seconds) else "${seconds}s"
}
