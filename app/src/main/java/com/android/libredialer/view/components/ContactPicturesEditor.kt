package com.android.libredialer.view.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ContactPicturesEditor(
    name: String,
    contactPhotoUri: String?,
    callingCardUri: String?,
    onContactPhotoSelected: (String) -> Unit,
    onContactPhotoRemoved: () -> Unit,
    onCallingCardSelected: (String) -> Unit,
    onCallingCardRemoved: () -> Unit
) {
    val contactPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { onContactPhotoSelected(it.toString()) } }
    val callingCardPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { onCallingCardSelected(it.toString()) } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Picture and calling card",
                modifier = Modifier.align(Alignment.Start),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                PictureOption(
                    title = "Contact picture",
                    uri = contactPhotoUri,
                    name = name,
                    callingCard = false,
                    onPick = {
                        contactPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = onContactPhotoRemoved
                )
                PictureOption(
                    title = "Calling card",
                    uri = callingCardUri,
                    name = name,
                    callingCard = true,
                    onPick = {
                        callingCardPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = onCallingCardRemoved
                )
            }
        }
    }
}

@Composable
private fun PictureOption(
    title: String,
    uri: String?,
    name: String,
    callingCard: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    var imageFailed by remember(uri) { mutableStateOf(false) }
    Column(
        modifier = Modifier.width(144.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        if (!callingCard) {
            RivoAvatar(
                name = name,
                photoUri = uri,
                modifier = Modifier.size(104.dp),
                shape = CircleShape
            )
        } else {
            Surface(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(18.dp)),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (!uri.isNullOrBlank() && !imageFailed) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "$title preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { imageFailed = true }
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        if (uri.isNullOrBlank()) Text("Add image", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        TextButton(onClick = onPick) {
            Icon(Icons.Default.AddAPhoto, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(if (uri.isNullOrBlank()) "Add" else "Change")
        }
        if (!uri.isNullOrBlank()) {
            TextButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Remove")
            }
        }
    }
}
