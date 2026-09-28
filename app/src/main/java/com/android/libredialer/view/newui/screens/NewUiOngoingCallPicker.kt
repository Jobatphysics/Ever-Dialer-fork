package com.android.libredialer.view.newui.screens

import android.telecom.Call
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.libredialer.controller.CallSession
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.modal.`interface`.IContactsRepository
import com.android.libredialer.modal.data.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun NewUiOngoingCallPicker(
    session: CallSession,
    onReturnToCall: () -> Unit
) {
    val contactsRepository: IContactsRepository = koinInject()
    val prefs: PreferenceManager = koinInject()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val number = session.call.details?.handle?.schemeSpecificPart
        ?.let { android.net.Uri.decode(it) }
        .orEmpty()
    var contact by remember(number) {
        mutableStateOf<Contact?>(null)
    }
    var durationSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(number) {
        contact = if (number.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching { contactsRepository.getContactByNumber(number) }.getOrNull()
        }
    }
    LaunchedEffect(session.call, session.state) {
        val connectedAt = session.call.details?.connectTimeMillis ?: 0L
        while (session.state == Call.STATE_ACTIVE || session.state == Call.STATE_HOLDING) {
            durationSeconds = if (connectedAt > 0L) {
                ((System.currentTimeMillis() - connectedAt) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
            delay(1000L)
        }
        durationSeconds = 0L
    }

    val hideContactName = remember(settingsVersion, contact?.id) {
        val hiddenIds = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "")
            .orEmpty()
            .split(",")
            .filter(String::isNotBlank)
            .toSet()
        val hideName = prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false) &&
            contact?.id in hiddenIds
        hideName
    }
    val displayName = if (hideContactName) number.ifBlank { "Unknown caller" }
    else contact?.name ?: number.ifBlank { "Unknown caller" }
    val photoUri = contact?.photoUri.takeUnless { hideContactName }
    val status = when (session.state) {
        Call.STATE_DIALING -> "Calling"
        Call.STATE_CONNECTING -> "Connecting"
        Call.STATE_ACTIVE -> "In progress"
        Call.STATE_HOLDING -> "On hold"
        Call.STATE_DISCONNECTING -> "Ending call"
        else -> "Ongoing call"
    }
    val callStatus = if (
        (session.state == Call.STATE_ACTIVE || session.state == Call.STATE_HOLDING) &&
        session.call.details?.connectTimeMillis?.let { it > 0L } == true
    ) {
        "$status · ${formatDuration(durationSeconds)}"
    } else {
        status
    }

    Surface(
        onClick = onReturnToCall,
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (number.isNotBlank() && number != displayName) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = callStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "RETURN TO CALL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
