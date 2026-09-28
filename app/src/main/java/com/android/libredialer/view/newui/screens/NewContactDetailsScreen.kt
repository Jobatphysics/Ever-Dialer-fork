package com.android.libredialer.view.newui.screens

import android.content.Intent
import android.net.Uri
import android.media.RingtoneManager
import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.android.libredialer.controller.CallLogViewModel
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.controller.util.NoteManager
import com.android.libredialer.controller.util.QrCodeUtils
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.controller.util.BlockedNumbersManager
import com.android.libredialer.controller.util.ContactRingtoneUtils
import com.android.libredialer.controller.util.ContactShortcutUtils
import com.android.libredialer.controller.util.isWhatsAppInstalled
import com.android.libredialer.controller.util.isWhatsAppBusinessInstalled
import com.android.libredialer.controller.util.isTelegramInstalled
import com.android.libredialer.controller.util.isGoogleMeetInstalled
import com.android.libredialer.controller.util.openWhatsAppChat
import com.android.libredialer.controller.util.openWhatsAppBusinessChat
import com.android.libredialer.controller.util.openTelegramChat
import com.android.libredialer.controller.util.startWhatsAppVoiceCall
import com.android.libredialer.controller.util.startWhatsAppBusinessVoiceCall
import com.android.libredialer.controller.util.startWhatsAppVideoCall
import com.android.libredialer.controller.util.startWhatsAppBusinessVideoCall
import com.android.libredialer.controller.util.startTelegramVoiceCall
import com.android.libredialer.controller.util.startTelegramVideoCall
import com.android.libredialer.controller.util.startGoogleMeetVoiceCall
import com.android.libredialer.controller.util.startGoogleMeetVideoCall
import com.android.libredialer.controller.util.formatDate
import com.android.libredialer.controller.util.numbersLikelyMatch
import com.android.libredialer.modal.data.CallLogEntry
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.view.newui.theme.NewUiDimensions
import com.android.libredialer.view.newui.components.newUiScrollContentPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.android.libredialer.view.newui.components.NewUiShortcutActionDialog
import com.android.libredialer.view.newui.components.NewUiChooseSimDialog
import com.android.libredialer.view.newui.components.NewUiNumberPickerDialog
import com.android.libredialer.view.newui.components.NewUiAppQuickActionsDialog
import com.android.libredialer.view.newui.components.newUiClickable

internal val NewUiContactAvatarSize = 192.dp

@Composable
internal fun NewUiContactAvatar(
    photoUri: String?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    if (!photoUri.isNullOrBlank()) {
        AsyncImage(
            model = photoUri,
            contentDescription = "$displayName photo",
            modifier = modifier.size(NewUiContactAvatarSize).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier.size(NewUiContactAvatarSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun NewContactDetailsScreen(
    contact: Contact,
    phoneNumber: String?,
    isUnknownNumber: Boolean = false,
    onBack: () -> Unit,
    onEdit: (Contact) -> Unit,
    onAddContact: (() -> Unit)? = null,
    onCall: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val prefs = koinInject<PreferenceManager>()
    val contactsViewModel: ContactsViewModel = org.koin.compose.viewmodel.koinActivityViewModel()
    val callLogViewModel: CallLogViewModel = org.koin.compose.viewmodel.koinActivityViewModel()
    val logs by callLogViewModel.allCallLogs.collectAsState()
    val currentContact = contactsViewModel.allContacts.collectAsState().value
        .firstOrNull { it.id == contact.id } ?: contact
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val numbers = remember(currentContact, phoneNumber) {
        (currentContact.phoneNumbers + listOfNotNull(phoneNumber))
            .filter(String::isNotBlank).distinct()
    }
    val contactLogs = remember(logs, currentContact, numbers) {
        logs.filter { log ->
            log.contactId == currentContact.id ||
                numbers.any { numbersLikelyMatch(it, log.number) }
        }
    }
    val contactKey = currentContact.id
    var simChoice by remember(settingsVersion, contactKey) {
        mutableStateOf(prefs.getContactSimChoice(contactKey, numbers.firstOrNull()))
    }
    var showSimDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var showShortcutNumberDialog by remember { mutableStateOf(false) }
    var selectedShortcutNumber by remember { mutableStateOf<String?>(numbers.firstOrNull()) }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    var showSocialActions by remember { mutableStateOf<String?>(null) }
    var socialNumber by remember { mutableStateOf<String?>(null) }
    var blockState by remember { mutableStateOf(false) }
    val socialApps = remember(context) {
        listOf(
            "whatsapp" to isWhatsAppInstalled(context),
            "whatsapp_business" to isWhatsAppBusinessInstalled(context),
            "telegram" to isTelegramInstalled(context),
            "googlemeet" to isGoogleMeetInstalled(context)
        )
    }
    var ringtoneUri by remember(currentContact.id) {
        mutableStateOf(ContactRingtoneUtils.getCustomRingtoneUri(context, currentContact.id))
    }
    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked = result.data?.getParcelableExtra<Uri>(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val stored = when {
                result.data?.hasExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) != true -> Uri.EMPTY
                picked == null || picked == defaultUri -> null
                else -> picked
            }
            ContactRingtoneUtils.setCustomRingtoneUri(context, currentContact.id, stored)
            ringtoneUri = stored
            showRingtoneDialog = false
        }
    }
    var showDelete by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val qrBitmap by produceState<android.graphics.Bitmap?>(null, showQr, currentContact.id, numbers) {
        if (showQr) value = withContext(Dispatchers.Default) {
            QrCodeUtils.generateQrCode(
                QrCodeUtils.generateVCard(currentContact.name, numbers.firstOrNull(), currentContact.emails.firstOrNull()),
                600
            )
        }
    }

    LaunchedEffect(currentContact.id, numbers.firstOrNull()) {
        note = withContext(Dispatchers.IO) {
            NoteManager.readNote(context, currentContact.name, numbers.firstOrNull().orEmpty())
        }
    }
    LaunchedEffect(currentContact.id, numbers) {
        blockState = withContext(Dispatchers.IO) {
            numbers.any { BlockedNumbersManager.isBlocked(context, prefs, it) }
        }
    }

    fun call(number: String) {
        onCall(number, currentContact.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(NewUiDimensions.PagePadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                if (!isUnknownNumber) {
                    IconButton(onClick = { contactsViewModel.toggleFavorite(currentContact) }) {
                        Icon(
                            if (currentContact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (currentContact.isFavorite) "Remove favorite" else "Add favorite"
                        )
                    }
                    IconButton(onClick = { onEdit(currentContact) }) {
                        Icon(Icons.Default.Edit, "Edit contact")
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = newUiScrollContentPadding(androidx.compose.foundation.layout.PaddingValues(
                    top = 12.dp,
                    bottom = 24.dp
                )),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NewUiContactAvatar(currentContact.photoUri, currentContact.name)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            currentContact.name.ifBlank { numbers.firstOrNull() ?: "Contact" },
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (isUnknownNumber && onAddContact != null) {
                    item {
                        Button(onClick = onAddContact, modifier = Modifier.fillMaxWidth()) {
                            Text("Add to Contacts")
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PrimaryContactAction(
                            icon = Icons.Default.Call,
                            label = "Call",
                            enabled = numbers.isNotEmpty(),
                            onClick = { numbers.firstOrNull()?.let(::call) },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryContactAction(
                            icon = Icons.Default.Message,
                            label = "Message",
                            enabled = numbers.isNotEmpty(),
                            onClick = {
                                numbers.firstOrNull()?.let {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$it")))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryContactAction(
                            icon = Icons.Default.VideoCall,
                            label = "Video",
                            enabled = numbers.isNotEmpty() && socialApps.any { it.second },
                            onClick = {
                                val app = socialApps.firstOrNull { it.second }?.first
                                val number = numbers.firstOrNull()
                                if (app != null && number != null) {
                                    socialNumber = number
                                    showSocialActions = app
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryContactAction(
                            icon = Icons.Default.Email,
                            label = "Email",
                            enabled = currentContact.emails.any(String::isNotBlank),
                            onClick = {
                                currentContact.emails.firstOrNull(String::isNotBlank)?.let { email ->
                                    context.startActivity(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (numbers.isNotEmpty() || currentContact.emails.isNotEmpty() || currentContact.addresses.isNotEmpty()) {
                    item {
                        DetailsCard("Contact information") {
                            numbers.forEach { number ->
                                DetailRow(number, "Phone", Icons.Default.Call) { call(number) }
                            }
                            currentContact.emails.forEach { email ->
                                DetailRow(email, "Email", Icons.Default.Email) {
                                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                                }
                            }
                            currentContact.addresses.forEach { address ->
                                DetailRow(address, "Open map", Icons.Default.LocationOn) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}")))
                                }
                            }
                        }
                    }
                }
                if (contactLogs.isNotEmpty()) {
                    item {
                        DetailsCard("Recent activity") {
                            contactLogs.take(10).forEach { log ->
                                DetailRow(formatDate(log.date), "${callTypeLabel(log)} · ${log.duration}s", Icons.Default.History) {
                                    call(log.number)
                                }
                            }
                        }
                    }
                }
                if (!isUnknownNumber && socialApps.any { it.second }) {
                    item {
                        ActionGroup(label = "Connected apps") {
                            val appTiles = buildList {
                                if (socialApps.first { it.first == "whatsapp" }.second) {
                                    add(ContactActionTile(Icons.Default.Message, "WhatsApp", "WhatsApp", {
                                        socialNumber = numbers.firstOrNull()
                                        showSocialActions = "whatsapp"
                                    }))
                                }
                                if (socialApps.first { it.first == "whatsapp_business" }.second) {
                                    add(ContactActionTile(Icons.Default.Message, "WA Business", "WhatsApp Business", {
                                        socialNumber = numbers.firstOrNull()
                                        showSocialActions = "whatsapp_business"
                                    }))
                                }
                                if (socialApps.first { it.first == "telegram" }.second) {
                                    add(ContactActionTile(Icons.Default.Send, "Telegram", "Telegram", {
                                        socialNumber = numbers.firstOrNull()
                                        showSocialActions = "telegram"
                                    }))
                                }
                                if (socialApps.first { it.first == "googlemeet" }.second) {
                                    add(ContactActionTile(Icons.Default.VideoCall, "Meet", "Google Meet", {
                                        socialNumber = numbers.firstOrNull()
                                        showSocialActions = "googlemeet"
                                    }))
                                }
                            }
                            ActionTileGrid(appTiles)
                        }
                    }
                }
                item {
                    DetailsCard("Additional information") {
                        Text(
                            if (note.isBlank()) "No phone note" else note,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showNoteEditor = true }) {
                            Icon(Icons.Default.Edit, if (note.isBlank()) "Add note" else "Edit note")
                            Spacer(Modifier.size(6.dp))
                            Text(if (note.isBlank()) "Add note" else "Edit note")
                        }
                        if (currentContact.sourceAccounts.isNotEmpty() || currentContact.events.isNotEmpty()) {
                            currentContact.events.forEach { event ->
                                DetailRow(event.date, event.label ?: "Event", Icons.Default.Event)
                            }
                            if (currentContact.sourceAccounts.isNotEmpty()) {
                                currentContact.sourceAccounts.forEach { account ->
                                    DetailRow(account, "Saved in", Icons.Default.Info)
                                }
                            }
                        }
                    }
                }
                item {
                    ActionGroup(label = "Contact settings") {
                        val controlTiles = buildList {
                            add(ContactActionTile(Icons.Default.SimCard, "SIM", "Choose SIM") { showSimDialog = true })
                            add(ContactActionTile(
                                Icons.Default.Block,
                                if (blockState) "Unblock" else "Block",
                                if (blockState) "Unblock contact" else "Block contact"
                            ) { showBlockDialog = true })
                            if (!isUnknownNumber) {
                                add(ContactActionTile(Icons.Default.MusicNote, "Ringtone", "Choose ringtone") {
                                    showRingtoneDialog = true
                                })
                                add(ContactActionTile(Icons.Default.Home, "Shortcut", "Add to Home Screen") {
                                    if (numbers.size > 1) showShortcutNumberDialog = true
                                    else if (numbers.isNotEmpty()) showShortcutDialog = true
                                })
                            }
                        }
                        ActionTileGrid(controlTiles)
                    }
                }
                item {
                    ActionGroup(label = "More actions") {
                        ActionTileRow(
                            ContactActionTile(Icons.Default.ContentCopy, "Copy", "Copy phone number") {
                                numbers.firstOrNull()?.let { clipboard.setText(AnnotatedString(it)) }
                            },
                            ContactActionTile(Icons.Default.Share, "Share", "Share contact") {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, listOf(currentContact.name, *numbers.toTypedArray()).joinToString("\n"))
                                }
                                context.startActivity(Intent.createChooser(share, "Share contact"))
                            },
                            ContactActionTile(Icons.Default.QrCode2, "QR code", "Show contact QR code") {
                                showQr = true
                            }
                        )
                    }
                }
                item {
                    TextButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, "Delete contact", tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.size(6.dp))
                        Text("Delete contact", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete contact?") },
            text = { Text("Delete ${currentContact.name} from your contacts?") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    contactsViewModel.deleteContact(currentContact.id, onBack)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
    if (showNoteEditor) {
        NoteEditorDialog(
            value = note,
            onValueChange = { note = it },
            onDismiss = { showNoteEditor = false },
            onSave = {
                scope.launch(Dispatchers.IO) {
                    NoteManager.writeNote(context, currentContact.name, numbers.firstOrNull().orEmpty(), note)
                }
                showNoteEditor = false
            }
        )
    }
    if (showQr) {
        Dialog(onDismissRequest = { showQr = false }) {
            Card {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Contact QR code", style = MaterialTheme.typography.titleLarge)
                    qrBitmap?.let {
                        Image(it.asImageBitmap(), "Contact QR code", Modifier.size(260.dp))
                    }
                    TextButton(onClick = { showQr = false }) { Text("Close") }
                }
            }
            if (showSimDialog) {
                NewUiChooseSimDialog(
                    currentChoice = simChoice,
                    onSelect = {
                        simChoice = it
                        prefs.setContactSimChoice(contactKey, it)
                        showSimDialog = false
                    },
                    onDismiss = { showSimDialog = false }
                )
            }
            if (showBlockDialog) {
                AlertDialog(
                    onDismissRequest = { showBlockDialog = false },
                    title = { Text(if (blockState) "Unblock contact?" else "Block contact?") },
                    text = { Text(if (blockState) "Calls and messages from this contact will be allowed again." else "Calls and messages from this contact will be blocked.") },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                numbers.forEach {
                                    if (blockState) BlockedNumbersManager.unblock(context, prefs, it)
                                    else BlockedNumbersManager.block(context, prefs, it)
                                }
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    blockState = !blockState
                                }
                            }
                            showBlockDialog = false
                        }) { Text(if (blockState) "Unblock" else "Block") }
                    },
                    dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") } }
                )
            }
            if (showRingtoneDialog) {
                AlertDialog(
                    onDismissRequest = { showRingtoneDialog = false },
                    title = { Text("Contact ringtone") },
                    text = { Text(ContactRingtoneUtils.ringtoneLabel(context, ringtoneUri)) },
                    confirmButton = {
                        TextButton(onClick = {
                            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                            ringtonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri ?: defaultUri)
                            })
                        }) { Text("Choose") }
                    },
                    dismissButton = { TextButton(onClick = { showRingtoneDialog = false }) { Text("Cancel") } }
                )
            }
            if (showShortcutNumberDialog) {
                NewUiNumberPickerDialog(
                    numbers = numbers,
                    onDismissRequest = { showShortcutNumberDialog = false },
                    onNumberSelected = {
                        showShortcutNumberDialog = false
                        showShortcutDialog = true
                        selectedShortcutNumber = it
                    }
                )
            }
            if (showShortcutDialog) {
                NewUiShortcutActionDialog(
                    onOpenContactInfo = {
                        ContactShortcutUtils.pinOpenContactShortcut(context, currentContact.id, currentContact.name, currentContact.photoUri)
                        showShortcutDialog = false
                    },
                    onCallDirectly = {
                        selectedShortcutNumber?.let {
                            ContactShortcutUtils.pinCallShortcut(context, currentContact.id, currentContact.name, it, currentContact.photoUri)
                        }
                        showShortcutDialog = false
                    },
                    onDismiss = { showShortcutDialog = false }
                )
            }
            if (showSocialActions != null && socialNumber != null) {
                val app = showSocialActions!!
                val number = socialNumber!!
                val label = when (app) {
                    "whatsapp" -> "WhatsApp"
                    "whatsapp_business" -> "WhatsApp Business"
                    "telegram" -> "Telegram"
                    else -> "Google Meet"
                }
                NewUiAppQuickActionsDialog(
                    appName = label,
                    onChat = if (app == "googlemeet") null else {
                        {
                            showSocialActions = null
                            val opened = when (app) {
                                "whatsapp" -> openWhatsAppChat(context, number)
                                "whatsapp_business" -> openWhatsAppBusinessChat(context, number)
                                else -> openTelegramChat(context, number)
                            }
                            if (!opened) android.widget.Toast.makeText(context, "$label isn't installed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onVoiceCall = {
                        showSocialActions = null
                        val started = when (app) {
                            "whatsapp" -> startWhatsAppVoiceCall(context, number)
                            "whatsapp_business" -> startWhatsAppBusinessVoiceCall(context, number)
                            "telegram" -> startTelegramVoiceCall(context, number)
                            else -> startGoogleMeetVoiceCall(context, number)
                        }
                        if (!started) android.widget.Toast.makeText(context, "$label isn't installed", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onVideoCall = {
                        showSocialActions = null
                        val started = when (app) {
                            "whatsapp" -> startWhatsAppVideoCall(context, number)
                            "whatsapp_business" -> startWhatsAppBusinessVideoCall(context, number)
                            "telegram" -> startTelegramVideoCall(context, number)
                            else -> startGoogleMeetVideoCall(context, number)
                        }
                        if (!started) android.widget.Toast.makeText(context, "$label isn't installed", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showSocialActions = null }
                )
            }
        }
    }
}

@Composable
internal fun DetailsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            content()
        }
    }
}

@Composable
internal fun PrimaryContactAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(68.dp)
                .newUiClickable(
                    shape = CircleShape,
                    enabled = enabled,
                    pressedScale = 0.80f,
                    onClick = onClick
                ),
            shape = CircleShape,
            color = if (enabled) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (enabled) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

internal data class ContactActionTile(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val contentDescription: String,
    val onClick: () -> Unit
)

@Composable
internal fun ActionGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
internal fun ActionTileRow(vararg actions: ContactActionTile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            ContactActionTile(action, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun ActionTileGrid(actions: List<ContactActionTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(4).forEach { row ->
            ActionTileRow(*row.toTypedArray())
        }
    }
}

@Composable
internal fun ContactActionTile(action: ContactActionTile, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .heightIn(min = 68.dp)
            .newUiClickable(MaterialTheme.shapes.medium, onClick = action.onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = action.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun DetailRow(
    headline: String,
    supporting: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.newUiClickable(androidx.compose.ui.graphics.RectangleShape) { onClick() }
                } else Modifier
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = supporting, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(headline, style = MaterialTheme.typography.bodyLarge)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoteEditorDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact note") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Note") }
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun callTypeLabel(log: CallLogEntry): String = when (log.type) {
    android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming call"
    android.provider.CallLog.Calls.MISSED_TYPE -> "Missed call"
    android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing call"
    else -> "Call"
}
