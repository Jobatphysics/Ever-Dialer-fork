package com.android.libredialer.view.newui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.libredialer.controller.util.BackupManager
import com.android.libredialer.view.components.RivoExpressiveCard
import com.android.libredialer.view.components.RivoListItem
import com.android.libredialer.view.components.SettingsSearchHeaderAction
import com.android.libredialer.view.newui.components.NewUiScreenShell
import com.android.libredialer.view.newui.components.newUiScrollContentPadding
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.newui.navigation.NewUiSettingsDestination
import com.android.libredialer.view.newui.navigation.NewUiSettingsNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BackupOptions(
    val settings: Boolean = true,
    val callingCards: Boolean = true,
    val notes: Boolean = true,
    val contactGroups: Boolean = false,
    val recordings: Boolean = true,
    val contacts: Boolean = false,
    val callLogs: Boolean = false
) {
    val hasSelection: Boolean
        get() = settings || callingCards || notes || contactGroups || recordings || contacts || callLogs
}

private data class PendingRestore(
    val file: File,
    val contents: BackupManager.BackupContents
)

private sealed interface BackupOperation {
    data object Idle : BackupOperation
    data object Reading : BackupOperation
    data object Creating : BackupOperation
    data object Restoring : BackupOperation
    data class Success(val message: String) : BackupOperation
    data class Error(val message: String) : BackupOperation
}

@Composable
fun NewUiBackupRestoreScreen(
    onNavigateSettings: NewUiSettingsNavigation
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBackupOptions by remember { mutableStateOf(false) }
    var backupOptions by remember { mutableStateOf(BackupOptions()) }
    var pendingBackupOptions by remember { mutableStateOf(BackupOptions()) }
    var pendingRestore by remember { mutableStateOf<PendingRestore?>(null) }
    var operation by remember { mutableStateOf<BackupOperation>(BackupOperation.Idle) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            val options = pendingBackupOptions
            operation = BackupOperation.Creating
            scope.launch {
                operation = try {
                    val saved = withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            BackupManager.writeBackup(
                                context,
                                output,
                                options.settings,
                                options.callingCards,
                                options.notes,
                                options.contactGroups,
                                options.recordings,
                                options.contacts,
                                options.callLogs
                            )
                        } ?: false
                    }
                    if (saved) BackupOperation.Success("Backup saved successfully")
                    else BackupOperation.Error("Failed to save backup")
                } catch (error: Exception) {
                    BackupOperation.Error(error.message ?: "Failed to save backup")
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            operation = BackupOperation.Reading
            scope.launch {
                operation = try {
                    val backup = withContext(Dispatchers.IO) {
                        val file = File.createTempFile("restore_", ".libredialer", context.cacheDir)
                        try {
                            val input = context.contentResolver.openInputStream(uri)
                                ?: error("Could not open the selected backup")
                            input.use { source ->
                                file.outputStream().use { output -> source.copyTo(output) }
                            }
                            PendingRestore(file, BackupManager.inspectBackup(file))
                        } catch (error: Exception) {
                            file.delete()
                            throw error
                        }
                    }
                    if (!backup.contents.hasAny) {
                        backup.file.delete()
                        BackupOperation.Error("The selected file does not contain restorable LibreDialer data")
                    } else {
                        pendingRestore = backup
                        BackupOperation.Idle
                    }
                } catch (error: Exception) {
                    BackupOperation.Error(error.message ?: "Failed to read backup file")
                }
            }
        }
    }

    NewUiScreenShell(
        destination = NewUiDestination.Settings,
        titleOverride = "Backup & Restore",
        headerAction = { SettingsSearchHeaderAction(onNavigateSettings) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = 112.dp, bottom = 24.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Data & Backup",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Create Backup",
                            supporting = "Save settings, notes, recordings, contacts, and call history",
                            leadingIcon = Icons.Default.Save,
                            trailingIcon = Icons.Default.Save,
                            onClick = { showBackupOptions = true }
                        )
                        androidx.compose.material3.HorizontalDivider()
                        RivoListItem(
                            headline = "Restore Backup",
                            supporting = "Choose a LibreDialer backup file and select what to restore",
                            leadingIcon = Icons.Default.Restore,
                            trailingIcon = Icons.Default.Restore,
                            onClick = { restoreLauncher.launch("*/*") }
                        )
                    }
                }
            }
        }
    }

    if (showBackupOptions) {
        BackupOptionsDialog(
            options = backupOptions,
            onOptionsChange = { backupOptions = it },
            onDismiss = { showBackupOptions = false },
            onSave = {
                pendingBackupOptions = it
                showBackupOptions = false
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                saveBackupLauncher.launch("LibreDialer_Backup_$timestamp.libredialer")
            },
            onShare = { options ->
                showBackupOptions = false
                operation = BackupOperation.Creating
                scope.launch {
                    operation = try {
                        val file = withContext(Dispatchers.IO) {
                            BackupManager.createBackup(
                                context,
                                options.settings,
                                options.callingCards,
                                options.notes,
                                options.contactGroups,
                                options.recordings,
                                options.contacts,
                                options.callLogs
                            )
                        } ?: error("Failed to create backup")
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                        BackupOperation.Success("Backup created at ${file.absolutePath}")
                    } catch (error: Exception) {
                        BackupOperation.Error(error.message ?: "Failed to create backup")
                    }
                }
            }
        )
    }

    pendingRestore?.let { restore ->
        RestoreOptionsDialog(
            contents = restore.contents,
            onDismiss = {
                restore.file.delete()
                pendingRestore = null
            },
            onRestore = { selected ->
                pendingRestore = null
                operation = BackupOperation.Restoring
                scope.launch {
                    operation = try {
                        val restored = withContext(Dispatchers.IO) {
                            BackupManager.restoreBackup(
                                context,
                                restore.file,
                                selected.settings,
                                selected.callingCards,
                                selected.notes,
                                selected.contactGroups,
                                selected.recordings,
                                selected.contacts,
                                selected.callLogs
                            )
                        }
                        restore.file.delete()
                        if (restored) {
                            BackupOperation.Success("Your data has been restored successfully. Please restart the app.")
                        } else {
                            BackupOperation.Error("Restore failed")
                        }
                    } catch (error: Exception) {
                        restore.file.delete()
                        BackupOperation.Error(error.message ?: "Restore failed")
                    }
                }
            }
        )
    }

    when (val result = operation) {
        BackupOperation.Idle -> Unit
        BackupOperation.Reading, BackupOperation.Creating, BackupOperation.Restoring -> Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(
                        when (result) {
                            BackupOperation.Reading -> "Reading backup…"
                            BackupOperation.Creating -> "Creating backup…"
                            else -> "Restoring backup…"
                        }
                    )
                }
            }
        }
        is BackupOperation.Success -> AlertDialog(
            onDismissRequest = { operation = BackupOperation.Idle },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Complete") },
            text = { Text(result.message) },
            confirmButton = {
                TextButton(onClick = { operation = BackupOperation.Idle }) { Text("OK") }
            }
        )
        is BackupOperation.Error -> AlertDialog(
            onDismissRequest = { operation = BackupOperation.Idle },
            icon = { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Operation failed") },
            text = { Text(result.message) },
            confirmButton = {
                TextButton(onClick = { operation = BackupOperation.Idle }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun BackupOptionsDialog(
    options: BackupOptions,
    onOptionsChange: (BackupOptions) -> Unit,
    onDismiss: () -> Unit,
    onSave: (BackupOptions) -> Unit,
    onShare: (BackupOptions) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Backup") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Select items to include in your backup", style = MaterialTheme.typography.bodySmall)
                BackupOptionRow("Settings", options.settings) { onOptionsChange(options.copy(settings = it)) }
                BackupOptionRow("Calling cards and contact appearance", options.callingCards) {
                    onOptionsChange(options.copy(callingCards = it))
                }
                BackupOptionRow("Notes", options.notes) { onOptionsChange(options.copy(notes = it)) }
                BackupOptionRow("Contact groups", options.contactGroups) {
                    onOptionsChange(options.copy(contactGroups = it))
                }
                BackupOptionRow("Call recordings", options.recordings) {
                    onOptionsChange(options.copy(recordings = it))
                }
                BackupOptionRow("Contacts", options.contacts) { onOptionsChange(options.copy(contacts = it)) }
                BackupOptionRow("Call logs", options.callLogs) { onOptionsChange(options.copy(callLogs = it)) }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onShare(options) }, enabled = options.hasSelection) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Share")
                }
                Button(onClick = { onSave(options) }, enabled = options.hasSelection) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Save")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RestoreOptionsDialog(
    contents: BackupManager.BackupContents,
    onDismiss: () -> Unit,
    onRestore: (BackupOptions) -> Unit
) {
    var selected by remember(contents) {
        mutableStateOf(
            BackupOptions(
                settings = contents.hasSettings,
                callingCards = contents.hasCallingCards,
                notes = contents.hasNotes,
                contactGroups = contents.hasContactGroups,
                recordings = contents.hasRecordings,
                contacts = contents.hasContacts,
                callLogs = contents.hasCallLogs
            )
        )
    }
    val anySelected =
        (selected.settings && contents.hasSettings) ||
            (selected.callingCards && contents.hasCallingCards) ||
            (selected.notes && contents.hasNotes) ||
            (selected.contactGroups && contents.hasContactGroups) ||
            (selected.recordings && contents.hasRecordings) ||
            (selected.contacts && contents.hasContacts) ||
            (selected.callLogs && contents.hasCallLogs)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Select items to restore", style = MaterialTheme.typography.bodySmall)
                BackupOptionRow("Settings", selected.settings, contents.hasSettings) {
                    selected = selected.copy(settings = it)
                }
                BackupOptionRow("Calling cards and contact appearance", selected.callingCards, contents.hasCallingCards) {
                    selected = selected.copy(callingCards = it)
                }
                BackupOptionRow("Notes", selected.notes, contents.hasNotes) { selected = selected.copy(notes = it) }
                BackupOptionRow("Contact groups", selected.contactGroups, contents.hasContactGroups) {
                    selected = selected.copy(contactGroups = it)
                }
                BackupOptionRow("Call recordings", selected.recordings, contents.hasRecordings) {
                    selected = selected.copy(recordings = it)
                }
                BackupOptionRow("Contacts", selected.contacts, contents.hasContacts) {
                    selected = selected.copy(contacts = it)
                }
                BackupOptionRow("Call logs", selected.callLogs, contents.hasCallLogs) {
                    selected = selected.copy(callLogs = it)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onRestore(selected) }, enabled = anySelected) {
                Icon(Icons.Default.Restore, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Restore")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BackupOptionRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Text(
            text = if (enabled) label else "$label (not in backup)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
