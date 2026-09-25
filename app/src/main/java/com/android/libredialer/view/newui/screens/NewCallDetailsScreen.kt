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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.libredialer.controller.CallLogViewModel
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.controller.util.BlockedNumbersManager
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.controller.util.formatDate
import com.android.libredialer.controller.util.numbersLikelyMatch
import com.android.libredialer.modal.data.CallLogEntry
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.view.newui.components.newUiScrollContentPadding
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun NewCallDetailsScreen(
    entry: CallLogEntry,
    onBack: () -> Unit,
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
    val displayName = contact?.name?.takeIf { it.isNotBlank() }
        ?: entry.name?.takeIf { it.isNotBlank() && it != entry.number }
        ?: entry.number

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(displayName, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(bottom = 24.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!contact?.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = contact?.photoUri,
                            contentDescription = "$displayName photo",
                            modifier = Modifier.size(96.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                displayName.firstOrNull()?.uppercase() ?: "?",
                                modifier = Modifier.padding(top = 28.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(entry.number, style = MaterialTheme.typography.titleMedium)
                    Text(callTypeLabel(entry.type), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onCall(entry.number, contact?.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, "Call")
                        Spacer(Modifier.size(4.dp))
                        Text("Call")
                    }
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:${entry.number}")))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Message, "Message")
                        Spacer(Modifier.size(4.dp))
                        Text("Message")
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(entry.number)) }) {
                        Icon(Icons.Default.ContentCopy, "Copy number")
                        Text("Copy")
                    }
                    TextButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, entry.number)
                        }
                        context.startActivity(Intent.createChooser(share, "Share number"))
                    }) {
                        Icon(Icons.Default.Share, "Share number")
                        Text("Share")
                    }
                    TextButton(onClick = { showBlockDialog = true }) {
                        Icon(Icons.Default.Block, if (blocked) "Unblock number" else "Block number")
                        Text(if (blocked) "Unblock" else "Block")
                    }
                }
            }
            if (contact == null) {
                item {
                    Button(onClick = { onAddContact(entry.number) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PersonAdd, "Add to Contacts")
                        Spacer(Modifier.size(6.dp))
                        Text("Add to Contacts")
                    }
                }
            } else {
                item {
                    TextButton(
                        onClick = { onOpenContact(contact, entry.number) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View contact")
                    }
                }
            }
            item {
                CallInfoCard(entry)
            }
            item {
                Text("Call history", style = MaterialTheme.typography.titleLarge)
            }
            if (history.isEmpty()) {
                item { Text("No call history found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            text = { Text(if (blocked) "Calls and messages will be allowed again." else "Calls and messages will be blocked.") },
            confirmButton = {
                TextButton(onClick = {
                    if (blocked) BlockedNumbersManager.unblock(context, prefs, entry.number)
                    else BlockedNumbersManager.block(context, prefs, entry.number)
                    blocked = !blocked
                    showBlockDialog = false
                }) { Text(if (blocked) "Unblock" else "Block") }
            },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CallInfoCard(entry: CallLogEntry) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Call information", style = MaterialTheme.typography.titleMedium)
            Text(callTypeLabel(entry.type))
            Text(formatDate(entry.date))
            Text("Duration: ${durationLabel(entry.duration)}")
            if (entry.simSlot >= 0) Text("SIM ${entry.simSlot + 1}")
        }
    }
}

@Composable
private fun CallHistoryRow(entry: CallLogEntry) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Call, callTypeLabel(entry.type), tint = typeColor(entry.type))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(callTypeLabel(entry.type))
                Text(formatDate(entry.date), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(durationLabel(entry.duration), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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

@Composable
private fun typeColor(type: Int) = when (type) {
    CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> androidx.compose.ui.graphics.Color(0xFFB3261E)
    else -> androidx.compose.material3.MaterialTheme.colorScheme.primary
}
