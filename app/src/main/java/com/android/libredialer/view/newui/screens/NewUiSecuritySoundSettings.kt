package com.android.libredialer.view.newui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.biometric.BiometricManager
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.controller.util.DialpadTonePlayer
import com.android.libredialer.controller.util.DialpadToneStyle
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.modal.`interface`.IContactsRepository
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.view.components.RivoAvatar
import com.android.libredialer.view.components.SettingsSearchHeaderAction
import com.android.libredialer.view.components.settingsSearchHighlight
import com.android.libredialer.view.newui.components.NewUiScreenShell
import com.android.libredialer.view.newui.components.newUiClickable
import com.android.libredialer.view.newui.components.newUiSettingsHeaderContentTopPadding
import com.android.libredialer.view.newui.components.newUiScrollContentPadding
import com.android.libredialer.view.newui.navigation.NewUiDestination
import com.android.libredialer.view.screen.settings.PasswordSetupDialog
import com.android.libredialer.view.screen.settings.PinSetupDialog
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun NewUiSoundVibrationScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs: PreferenceManager = koinInject()
    val context = LocalContext.current
    var highlightedKey by remember { mutableStateOf(highlightKey) }
    var dtmfTone by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, false))
    }
    var toneStyle by remember {
        mutableStateOf(
            DialpadToneStyle.fromKey(
                prefs.getString(
                    PreferenceManager.KEY_DIALPAD_TONE_STYLE,
                    DialpadToneStyle.STANDARD.key
                )
            )
        )
    }
    var showToneStyleDialog by remember { mutableStateOf(false) }

    NewUiScreenShell(
        NewUiDestination.Settings,
        titleOverride = "Sound & Vibration",
        headerAction = { SettingsSearchHeaderAction(navigator) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = newUiSettingsHeaderContentTopPadding(), bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NewUiGroup2Section("Dialpad") {
                    NewUiGroup2SwitchRow(
                        title = "DTMF Tone",
                        description = "Dialpad tone that plays during keypress",
                        icon = Icons.Default.Audiotrack,
                        checked = dtmfTone,
                        modifier = Modifier.settingsSearchHighlight("dtmf_tone", highlightedKey) {
                            highlightedKey = null
                        }
                    ) {
                        dtmfTone = it
                        prefs.setBoolean(PreferenceManager.KEY_DTMF_TONE, it)
                    }
                    NewUiGroup2ActionRow(
                        title = "Dial Pad Tone",
                        description = "${toneStyle.label} — ${toneStyle.description}",
                        icon = toneStyle.icon(),
                        modifier = Modifier.settingsSearchHighlight("dialpad_tone", highlightedKey) {
                            highlightedKey = null
                        },
                        onClick = { showToneStyleDialog = true }
                    )
                }
            }
            item {
                NewUiGroup2Section("System Sounds") {
                    NewUiGroup2ActionRow(
                        title = "Ringtone Settings",
                        description = "Open system sound settings",
                        icon = Icons.Default.MusicNote,
                        modifier = Modifier.settingsSearchHighlight("ringtone_settings", highlightedKey) {
                            highlightedKey = null
                        },
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                        }
                    )
                    NewUiGroup2ActionRow(
                        title = "Do Not Disturb",
                        description = "Manage interruption settings",
                        icon = Icons.Default.DoNotDisturb,
                        modifier = Modifier.settingsSearchHighlight("dnd_settings", highlightedKey) {
                            highlightedKey = null
                        },
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            )
                        }
                    )
                }
            }
        }
    }

    if (showToneStyleDialog) {
        AlertDialog(
            onDismissRequest = { showToneStyleDialog = false },
            icon = { Icon(Icons.Default.Audiotrack, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Dial Pad Tone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Choose the sound that plays when you tap a dialpad key. Tap an option to preview it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DialpadToneStyle.entries.forEach { option ->
                        val selected = toneStyle == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .newUiClickable(MaterialTheme.shapes.medium) {
                                    toneStyle = option
                                    prefs.setString(PreferenceManager.KEY_DIALPAD_TONE_STYLE, option.key)
                                    DialpadTonePlayer.play(context, "5", option)
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                option.icon(),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    toneStyle = option
                                    prefs.setString(PreferenceManager.KEY_DIALPAD_TONE_STYLE, option.key)
                                    DialpadTonePlayer.play(context, "5", option)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToneStyleDialog = false }) { Text("Done") }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }
}

@Composable
fun NewUiBiometricScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs: PreferenceManager = koinInject()
    val contactsRepo: IContactsRepository = koinInject()
    val context = LocalContext.current
    var highlightedKey by remember { mutableStateOf(highlightKey) }
    var authType by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: "")
    }
    var appLockEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false))
    }
    var callLockEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, false))
    }
    var callLockMode by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_MODE, "all") ?: "all")
    }
    var callLockNumbers by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_NUMBERS, "") ?: "")
    }
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPasswordSetup by remember { mutableStateOf(false) }
    val systemBiometricsAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) { contactsRepo.getContacts() }
    }

    val selectedNumbers = remember(callLockNumbers) {
        if (callLockNumbers.isBlank()) emptySet()
        else callLockNumbers.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    val selectedContactCount = remember(selectedNumbers, contacts) {
        contacts.count { contact ->
            contact.phoneNumbers.any { number ->
                selectedNumbers.any { selected ->
                    number.filter(Char::isDigit).takeLast(10) ==
                        selected.filter(Char::isDigit).takeLast(10)
                }
            }
        }
    }
    val authTypeLabel = when (authType) {
        "system" -> "System Biometrics"
        "pin" -> "Custom PIN"
        "password" -> "Custom Password"
        else -> "Not Set"
    }

    NewUiScreenShell(
        NewUiDestination.Settings,
        titleOverride = "Authentication",
        headerAction = { SettingsSearchHeaderAction(navigator) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = newUiSettingsHeaderContentTopPadding(), bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NewUiGroup2Section("Authentication Method") {
                    NewUiGroup2ActionRow(
                        "Authentication Method",
                        authTypeLabel,
                        Icons.Default.Fingerprint,
                        trailing = true,
                        modifier = Modifier.settingsSearchHighlight("auth_method", highlightedKey) {
                            highlightedKey = null
                        },
                        onClick = { showTypeDialog = true }
                    )
                }
            }
            if (authType.isNotEmpty()) {
                item {
                    NewUiGroup2Section("Authentication") {
                        NewUiGroup2SwitchRow(
                            "Lock App on Open",
                            "Require authentication when opening Phone",
                            Icons.Default.LockOpen,
                            appLockEnabled,
                            modifier = Modifier.settingsSearchHighlight("lock_app_open", highlightedKey) {
                                highlightedKey = null
                            }
                        ) {
                            appLockEnabled = it
                            prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, it)
                        }
                        NewUiGroup2SwitchRow(
                            "Lock Call Actions",
                            "Require authentication to answer or reject incoming calls",
                            Icons.Default.Lock,
                            callLockEnabled,
                            modifier = Modifier.settingsSearchHighlight("lock_call_actions", highlightedKey) {
                                highlightedKey = null
                            }
                        ) {
                            callLockEnabled = it
                            prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, it)
                        }
                    }
                }
            }
            if (authType.isNotEmpty() && callLockEnabled) {
                item {
                    NewUiGroup2Section("Lock Scope") {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                listOf("all" to "All Calls", "specified" to "Specified", "skip_specified" to "Exclude")
                                    .forEachIndexed { index, (mode, label) ->
                                        SegmentedButton(
                                            selected = callLockMode == mode,
                                            onClick = {
                                                callLockMode = mode
                                                prefs.setString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_MODE, mode)
                                                if (mode != "all") showContactPicker = true
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                                        ) { Text(label, maxLines = 1) }
                                    }
                            }
                            val scopeDescription = when (callLockMode) {
                                "specified" -> if (selectedContactCount > 0) {
                                    "$selectedContactCount contact${if (selectedContactCount != 1) "s" else ""} will require biometric to answer"
                                } else "Choose contacts that require biometric to answer"
                                "skip_specified" -> if (selectedContactCount > 0) {
                                    "$selectedContactCount contact${if (selectedContactCount != 1) "s" else ""} excluded from biometric lock"
                                } else "Choose contacts to skip the biometric lock"
                                else -> "Biometric required for every incoming call"
                            }
                            Text(
                                scopeDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (callLockMode != "all") {
                                FilledTonalButton(
                                    onClick = { showContactPicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        if (selectedContactCount > 0) "Edit Selection · $selectedContactCount selected"
                                        else "Select Contacts"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTypeDialog) {
        NewUiAuthMethodDialog(
            systemAvailable = systemBiometricsAvailable,
            currentType = authType,
            onDismiss = { showTypeDialog = false },
            onSelect = { type ->
                showTypeDialog = false
                when (type) {
                    "system" -> {
                        authType = "system"
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "system")
                        if (!appLockEnabled) {
                            appLockEnabled = true
                            prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true)
                        }
                    }
                    "pin" -> showPinSetup = true
                    "password" -> showPasswordSetup = true
                    else -> {
                        authType = ""
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "")
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PIN, "")
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "")
                        prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)
                        prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, false)
                        appLockEnabled = false
                        callLockEnabled = false
                    }
                }
            }
        )
    }
    if (showPinSetup) {
        PinSetupDialog(
            onConfirm = { pin ->
                authType = "pin"
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "pin")
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_PIN, pin)
                if (!appLockEnabled) {
                    appLockEnabled = true
                    prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true)
                }
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false }
        )
    }
    if (showPasswordSetup) {
        PasswordSetupDialog(
            onConfirm = { password ->
                authType = "password"
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "password")
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, password)
                if (!appLockEnabled) {
                    appLockEnabled = true
                    prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true)
                }
                showPasswordSetup = false
            },
            onDismiss = { showPasswordSetup = false }
        )
    }
    if (showContactPicker) {
        NewUiBiometricContactPicker(
            contacts = contacts,
            initialNumbers = selectedNumbers,
            onDone = { numbers ->
                val joined = numbers.joinToString(",")
                callLockNumbers = joined
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_NUMBERS, joined)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false }
        )
    }
}

@Composable
fun NewUiContactsHiderScreen() {
    val prefs: PreferenceManager = koinInject()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    var secretCode by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_CODE, "") ?: "")
    }
    val isEnabled = secretCode.isNotEmpty()
    var hiddenIds by remember {
        mutableStateOf(readHiddenIds(prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""))
    }
    var hideNames by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false))
    }
    var hideMenu by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, false))
    }
    var hideEverywhere by remember {
        mutableStateOf(
            prefs.getBoolean(
                PreferenceManager.KEY_CONTACTS_HIDER_HIDE_EVERYWHERE,
                prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_IN_CONTACTS, false)
            )
        )
    }
    var showContactPicker by remember { mutableStateOf(false) }
    var contactSearch by remember { mutableStateOf("") }

    fun saveHiddenIds(ids: Set<String>) {
        hiddenIds = ids
        contactsVM.updateHiddenContacts(ids)
    }

    NewUiScreenShell(NewUiDestination.Settings, titleOverride = "Contacts Hider") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = newUiScrollContentPadding(PaddingValues(top = newUiSettingsHeaderContentTopPadding(), bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NewUiGroup2Section("Access Code") {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Type this code in the Dialpad to open your hidden contacts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = secretCode,
                            onValueChange = { value ->
                                val digits = value.filter(Char::isDigit)
                                secretCode = digits
                                prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_CODE, digits)
                                if (digits.isEmpty() && hideMenu) {
                                    hideMenu = false
                                    prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, false)
                                }
                            },
                            label = { Text("Enter a secret code") },
                            placeholder = { Text("Numbers only") },
                            leadingIcon = { Icon(Icons.Default.Pin, null) },
                            trailingIcon = {
                                if (secretCode.isNotEmpty()) {
                                    IconButton(onClick = {
                                        secretCode = ""
                                        prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_CODE, "")
                                        if (hideMenu) {
                                            hideMenu = false
                                            prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, false)
                                        }
                                    }) { Icon(Icons.Default.Close, null) }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AnimatedVisibility(visible = isEnabled) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Contacts Hider is active", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item {
                NewUiGroup2Section("Hidden Contacts") {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Hidden Contacts", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Add contacts to hide them from call logs, contacts, and favourites",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { contactSearch = ""; showContactPicker = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Add")
                            }
                        }
                        val hiddenContacts = allContacts.filter { it.id in hiddenIds }
                        val missingIds = hiddenIds - allContacts.map { it.id }.toSet()
                        if (hiddenIds.isEmpty()) {
                            Text(
                                "No hidden contacts yet",
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (hiddenContacts.isNotEmpty() || missingIds.isNotEmpty()) {
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            hiddenContacts.forEach { contact ->
                                NewUiHiddenContactRow(contact) { saveHiddenIds(hiddenIds - contact.id) }
                                if (contact != hiddenContacts.lastOrNull()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                            missingIds.forEach { id ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PersonOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "Contact deleted ($id)",
                                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(onClick = { saveHiddenIds(hiddenIds - id) }) {
                                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                NewUiGroup2Section("Privacy") {
                    NewUiGroup2SwitchRow(
                        "Hide contact name in incoming calls and call logs",
                        "Hidden contacts will show only their number",
                        Icons.Default.VisibilityOff,
                        hideNames
                    ) {
                        hideNames = it
                        prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, it)
                    }
                    NewUiGroup2SwitchRow(
                        "Hide contacts everywhere",
                        "Hides contacts from the phone book section. They will be unhidden only from this feature",
                        Icons.Default.PersonOff,
                        hideEverywhere
                    ) {
                        hideEverywhere = it
                        contactsVM.setHideEverywhere(it)
                    }
                    NewUiGroup2SwitchRow(
                        "Hide \"Contact Hider\" menu",
                        if (isEnabled) "The Contacts Hider entry in Settings will be hidden"
                        else "Set a secret code first to enable this",
                        Icons.Default.HideSource,
                        hideMenu,
                        enabled = isEnabled
                    ) {
                        if (isEnabled) {
                            hideMenu = it
                            prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, it)
                        }
                    }
                }
            }
        }
    }

    if (showContactPicker) {
        NewUiHiddenContactsPicker(
            contacts = allContacts,
            hiddenIds = hiddenIds,
            search = contactSearch,
            onSearchChange = { contactSearch = it },
            onSelect = { contact ->
                saveHiddenIds(hiddenIds + contact.id)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false }
        )
    }
}

@Composable
private fun NewUiGroup2Section(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
private fun NewUiGroup2SwitchRow(
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
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled
            )
        }
        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NewUiGroup2ActionRow(
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
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NewUiAuthMethodDialog(
    systemAvailable: Boolean,
    currentType: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Authentication Method") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NewUiAuthMethodRow(
                    "System Biometrics",
                    if (systemAvailable) "Fingerprint, face unlock, or device credentials"
                    else "Not available on this device",
                    Icons.Default.Fingerprint,
                    currentType == "system",
                    enabled = systemAvailable
                ) { onSelect("system") }
                NewUiAuthMethodRow(
                    "PIN",
                    "Set a numeric PIN of any length",
                    Icons.Default.Pin,
                    currentType == "pin"
                ) { onSelect("pin") }
                NewUiAuthMethodRow(
                    "Password",
                    "Set a custom alphanumeric password",
                    Icons.Default.Key,
                    currentType == "password"
                ) { onSelect("password") }
                if (currentType.isNotEmpty()) {
                    NewUiAuthMethodRow(
                        "Remove Biometric Lock",
                        "Disable app and call authentication",
                        Icons.Default.LockOpen,
                        selected = false,
                        errorStyle = true
                    ) { onSelect("") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

@Composable
private fun NewUiAuthMethodRow(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    errorStyle: Boolean = false,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        if (errorStyle) MaterialTheme.colorScheme.errorContainer
        else if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "authMethodBackground"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .newUiClickable(MaterialTheme.shapes.medium, enabled = enabled, onClick = onClick)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (errorStyle) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NewUiBiometricContactPicker(
    contacts: List<Contact>,
    initialNumbers: Set<String>,
    onDone: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedNumbers by remember {
        mutableStateOf(initialNumbers.map { it.filter(Char::isDigit).takeLast(10) }.toSet())
    }
    var query by remember { mutableStateOf("") }
    fun normalize(number: String) = number.filter(Char::isDigit).takeLast(10)
    fun isSelected(contact: Contact) = contact.phoneNumbers.any { normalize(it) in selectedNumbers }
    fun toggle(contact: Contact) {
        val numbers = contact.phoneNumbers.map(::normalize).filter(String::isNotEmpty).toSet()
        selectedNumbers = if (isSelected(contact)) selectedNumbers - numbers else selectedNumbers + numbers
    }
    val filtered = remember(contacts, query) {
        (if (query.isBlank()) contacts else contacts.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
                contact.phoneNumbers.any { it.contains(query) }
        }).sortedBy { it.name.lowercase() }
    }
    val selectedCount = contacts.count(::isSelected)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                topBar = {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Select Contacts", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                selectedNumbers = contacts.flatMap { contact ->
                                    contact.phoneNumbers.map(::normalize)
                                }.filter(String::isNotEmpty).toSet()
                            }) { Text("Select All") }
                            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.android.libredialer.view.components.SearchPillInput(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.weight(1f),
                                placeholder = "Search contacts"
                            )
                            com.android.libredialer.view.components.SearchClearButton(
                                onClick = { query = "" }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if (contacts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No contacts available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(filtered, key = { it.id }) { contact ->
                                val checked = isSelected(contact)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .newUiClickable(MaterialTheme.shapes.medium) { toggle(contact) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RivoAvatar(
                                        name = contact.name,
                                        photoUri = contact.photoUri,
                                        emojiFallbackContactId = contact.id,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Text(contact.name.ifBlank { "Unknown" }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        contact.phoneNumbers.firstOrNull()?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Checkbox(checked = checked, onCheckedChange = { toggle(contact) })
                                }
                                HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                    if (selectedCount > 0) {
                        Button(
                            onClick = {
                                val numbers = contacts.filter(::isSelected)
                                    .flatMap { contact -> contact.phoneNumbers.map(::normalize) }
                                    .filter(String::isNotEmpty)
                                    .toSet()
                                onDone(numbers)
                            },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.size(8.dp))
                            Text("Done · $selectedCount selected")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewUiHiddenContactsPicker(
    contacts: List<Contact>,
    hiddenIds: Set<String>,
    search: String,
    onSearchChange: (String) -> Unit,
    onSelect: (Contact) -> Unit,
    onDismiss: () -> Unit
) {
    val visible = contacts.filter { contact ->
        contact.id !in hiddenIds && (search.isBlank() ||
            contact.name.contains(search, ignoreCase = true) ||
            contact.phoneNumbers.any { it.contains(search) })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select contacts to hide") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.android.libredialer.view.components.SearchPillInput(
                        value = search,
                        onValueChange = onSearchChange,
                        modifier = Modifier.weight(1f),
                        placeholder = "Search contacts"
                    )
                    com.android.libredialer.view.components.SearchClearButton(
                        onClick = { onSearchChange("") }
                    )
                }
                if (visible.isEmpty()) {
                    Text(
                        "No contacts found",
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 400.dp)) {
                        items(visible, key = { it.id }) { contact ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .newUiClickable(MaterialTheme.shapes.medium) { onSelect(contact) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RivoAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    emojiFallbackContactId = contact.id,
                                    modifier = Modifier.size(40.dp)
                                )
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(contact.name, style = MaterialTheme.typography.bodyMedium)
                                    contact.phoneNumbers.firstOrNull()?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

@Composable
private fun NewUiHiddenContactRow(contact: Contact, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RivoAvatar(
            name = contact.name,
            photoUri = contact.photoUri,
            emojiFallbackContactId = contact.id,
            modifier = Modifier.size(38.dp)
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            contact.phoneNumbers.firstOrNull()?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
}

private fun readHiddenIds(raw: String): Set<String> =
    if (raw.isBlank()) emptySet() else raw.split(",").filter(String::isNotBlank).toSet()

private fun DialpadToneStyle.icon(): ImageVector = when (this) {
    DialpadToneStyle.STANDARD -> Icons.Default.Audiotrack
    DialpadToneStyle.PIANO -> Icons.Default.MusicNote
    DialpadToneStyle.WATER_DROP -> Icons.Outlined.Opacity
    DialpadToneStyle.MECHANICAL -> Icons.Outlined.Keyboard
    DialpadToneStyle.SCIFI -> Icons.Outlined.VideogameAsset
}
