package com.android.libredialer.view.newui.screens

import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.modal.data.Contact
import com.android.libredialer.modal.data.ContactAccountInfo
import com.android.libredialer.modal.data.ContactPhone
import com.android.libredialer.modal.data.ContactSaveTarget
import com.android.libredialer.modal.data.getPhoneTypeLabel
import com.android.libredialer.view.newui.theme.NewUiDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinActivityViewModel

private data class NewEditablePhone(
    val value: String,
    val type: Int = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
    val label: String? = null
)

@Composable
fun NewContactEditScreen(
    contact: Contact?,
    initialPhone: String? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsViewModel.allContacts.collectAsState()
    val isNew = contact == null
    val currentContact = contact?.let { item ->
        allContacts.firstOrNull { it.id == item.id } ?: item
    }
    val saveTargets = remember { contactsViewModel.getSaveTargets() }
    var initialized by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(contact?.name.orEmpty()) }
    var photoUri by remember { mutableStateOf(contact?.photoUri) }
    var note by remember { mutableStateOf(contact?.note.orEmpty()) }
    val phones = remember { mutableStateListOf(NewEditablePhone(initialPhone.orEmpty())) }
    val emails = remember { mutableStateListOf("") }
    val addresses = remember { mutableStateListOf("") }
    var accounts by remember { mutableStateOf<List<ContactAccountInfo>>(emptyList()) }
    var selectedTarget by remember { mutableStateOf<ContactSaveTarget?>(null) }
    var updateAllAccounts by remember { mutableStateOf(true) }
    var showStoragePicker by remember { mutableStateOf(false) }
    var phoneTypeIndex by remember { mutableIntStateOf(-1) }
    var customPhoneIndex by remember { mutableIntStateOf(-1) }
    var showCustomLabel by remember { mutableStateOf(false) }
    var customLabel by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(contact?.id, allContacts) {
        if (!initialized && currentContact != null) {
            val loaded = withContext(Dispatchers.IO) {
                contactsViewModel.getContactById(currentContact.id) ?: currentContact
            }
            accounts = withContext(Dispatchers.IO) {
                contactsViewModel.getContactAccounts(loaded.id)
            }
            name = loaded.name
            photoUri = loaded.photoUri
            note = loaded.note.orEmpty()
            phones.clear()
            phones += if (loaded.phones.isNotEmpty()) {
                loaded.phones.map { NewEditablePhone(it.number, it.type, it.label) }
            } else {
                loaded.phoneNumbers.map { NewEditablePhone(it) }
            }.ifEmpty { listOf(NewEditablePhone("")) }
            emails.clear()
            emails += loaded.emails.ifEmpty { listOf("") }
            addresses.clear()
            addresses += loaded.addresses.ifEmpty { listOf("") }
            initialized = true
        } else if (!initialized && isNew) {
            initialized = true
        }
    }

    LaunchedEffect(isNew, saveTargets) {
        if (isNew && selectedTarget == null) {
            selectedTarget = saveTargets.firstOrNull {
                it.accountType?.contains("google", ignoreCase = true) == true
            } ?: saveTargets.firstOrNull { !it.isSim } ?: saveTargets.firstOrNull()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) photoUri = uri.toString() }

    fun save(target: ContactSaveTarget?, allAccounts: Boolean) {
        if (saving) return
        val validPhones = phones.map { it.value.trim() }.filter { it.isNotBlank() }
        if (name.trim().isBlank() || validPhones.isEmpty()) return
        saving = true
        val saved = Contact(
            id = if (isNew) "0" else currentContact?.id.orEmpty(),
            name = name.trim(),
            phoneNumbers = validPhones,
            phones = phones.filter { it.value.isNotBlank() }.map {
                ContactPhone(it.value.trim(), it.type, it.label)
            },
            emails = emails.map(String::trim).filter(String::isNotBlank),
            addresses = addresses.map(String::trim).filter(String::isNotBlank),
            events = currentContact?.events ?: emptyList(),
            photoUri = photoUri,
            isFavorite = currentContact?.isFavorite ?: false,
            sourceAccounts = currentContact?.sourceAccounts ?: emptyList(),
            note = note.trim().ifBlank { null }
        )
        if (isNew && target?.isSim == true) {
            contactsViewModel.saveContactToSim(saved, target.simSlotIndex) { success ->
                Toast.makeText(
                    context,
                    if (success) "Saved to ${target.label}" else "Couldn't save to ${target.label}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            contactsViewModel.saveContact(
                contact = saved,
                accountType = target?.accountType,
                accountName = target?.accountName,
                updateAllAccounts = allAccounts,
                originalContact = currentContact
            )
        }
        onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = NewUiDimensions.PagePadding)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isNew) "Create Contact" else "Edit Contact",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !saving) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (isNew || (!updateAllAccounts && selectedTarget == null)) {
                                    showStoragePicker = true
                                } else {
                                    save(selectedTarget, updateAllAccounts)
                                }
                            },
                            enabled = !saving && name.isNotBlank() && phones.any { it.value.isNotBlank() }
                        ) {
                            Icon(Icons.Default.Check, "Save")
                            Spacer(Modifier.size(4.dp))
                            Text("Save")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "$name photo",
                                modifier = Modifier.size(104.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(104.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        name.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.displaySmall
                                    )
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        TextButton(onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.AddAPhoto, "Choose contact photo")
                            Spacer(Modifier.size(6.dp))
                            Text("Choose photo")
                        }
                        if (!photoUri.isNullOrBlank()) {
                            TextButton(onClick = { photoUri = null }) {
                                Icon(Icons.Default.Delete, "Remove contact photo")
                                Spacer(Modifier.size(6.dp))
                                Text("Remove photo")
                            }
                        }
                    }
                }
                item {
                    EditCard("Identity") {
                        EditText(name, { name = it }, "Full name", Icons.Default.Person)
                    }
                }
                item {
                    EditCard("Phone numbers") {
                        phones.forEachIndexed { index, field ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    TextButton(onClick = { phoneTypeIndex = index }) {
                                        Text(getPhoneTypeLabel(field.type, field.label))
                                    }
                                    EditText(
                                        field.value,
                                        { phones[index] = field.copy(value = it) },
                                        "Phone",
                                        Icons.Default.Phone
                                    )
                                }
                                if (phones.size > 1) {
                                    IconButton(onClick = { phones.removeAt(index) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, "Remove phone number")
                                    }
                                }
                            }
                        }
                        AddFieldButton("Add phone number") {
                            phones += NewEditablePhone("")
                        }
                    }
                }
                item {
                    EditCard("Email addresses") {
                        emails.forEachIndexed { index, value ->
                            EditableListRow(
                                value = value,
                                label = "Email",
                                icon = Icons.Default.Email,
                                onValueChange = { emails[index] = it },
                                onRemove = if (emails.size > 1) ({ emails.removeAt(index) }) else null
                            )
                        }
                        AddFieldButton("Add email address") { emails += "" }
                    }
                }
                item {
                    EditCard("Postal addresses") {
                        addresses.forEachIndexed { index, value ->
                            EditableListRow(
                                value = value,
                                label = "Address",
                                icon = Icons.Default.LocationOn,
                                onValueChange = { addresses[index] = it },
                                onRemove = if (addresses.size > 1) ({ addresses.removeAt(index) }) else null
                            )
                        }
                        AddFieldButton("Add address") { addresses += "" }
                    }
                }
                item {
                    EditCard("Notes") {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Description") },
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
                item {
                    val storage = selectedTarget?.label ?: if (updateAllAccounts) {
                        accounts.joinToString { it.displayName }.ifBlank { "All writable locations" }
                    } else "Choose storage location"
                    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, "Storage location")
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (isNew) "Save to" else "Edit location", fontWeight = FontWeight.Bold)
                                Text(storage, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { showStoragePicker = true }) { Text("Change") }
                        }
                    }
                }
            }
        }
    }

    phoneTypeIndex.takeIf { it >= 0 }?.let { index ->
        val options = listOf(
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE to "Mobile",
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME to "Home",
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK to "Work",
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN to "Main",
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER to "Other",
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM to "Custom"
        )
        AlertDialog(
            onDismissRequest = { phoneTypeIndex = -1 },
            title = { Text("Phone type") },
            text = {
                Column {
                    options.forEach { (type, label) ->
                        TextButton(onClick = {
                            if (type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                                customLabel = phones[index].label.orEmpty()
                                customPhoneIndex = index
                                showCustomLabel = true
                                phoneTypeIndex = -1
                            } else {
                                phones[index] = phones[index].copy(type = type, label = null)
                                phoneTypeIndex = -1
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                }
            },
            confirmButton = {}
        )
    }
    if (showCustomLabel) {
        AlertDialog(
            onDismissRequest = { showCustomLabel = false; customPhoneIndex = -1 },
            title = { Text("Custom phone label") },
            text = {
                OutlinedTextField(customLabel, { customLabel = it }, label = { Text("Label") })
            },
            confirmButton = {
                TextButton(onClick = {
                    customPhoneIndex.takeIf { it >= 0 }?.let { index ->
                        phones[index] = phones[index].copy(
                            type = ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,
                            label = customLabel.trim().ifBlank { "Custom" }
                        )
                    }
                    showCustomLabel = false
                    customPhoneIndex = -1
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCustomLabel = false; customPhoneIndex = -1 }) { Text("Cancel") } }
        )
    }
    if (showStoragePicker) {
        val writable = if (isNew) saveTargets else accounts.filter { !it.isReadOnly }.map {
            ContactSaveTarget(it.displayName, it.accountName, it.accountType, it.accountName, it.isSim, it.simSlotIndex)
        }
        AlertDialog(
            onDismissRequest = { showStoragePicker = false },
            title = { Text(if (isNew) "Save contact to" else "Edit location") },
            text = {
                Column {
                    if (!isNew && accounts.isNotEmpty()) {
                        TextButton(onClick = {
                            updateAllAccounts = true
                            selectedTarget = null
                            showStoragePicker = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("All linked writable locations")
                        }
                    }
                    writable.forEach { target ->
                        DropdownMenuItem(
                            text = { Text(target.label) },
                            onClick = {
                                selectedTarget = target
                                updateAllAccounts = false
                                showStoragePicker = false
                                if (isNew) save(target, false)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStoragePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EditCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun EditText(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EditableListRow(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit,
    onRemove: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EditText(value, onValueChange, label, icon)
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.RemoveCircleOutline, "Remove $label")
            }
        }
    }
}

@Composable
private fun AddFieldButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(Icons.Default.Add, "Add")
        Spacer(Modifier.size(6.dp))
        Text(label)
    }
}
