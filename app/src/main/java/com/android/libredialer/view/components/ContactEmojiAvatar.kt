package com.android.libredialer.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val contactAvatarEmojis = listOf(
    "🐶", "🐱", "🐼", "🐨", "🦊", "🐸", "🐵", "🦁", "🐯", "🐰",
    "🐻", "🐧", "🦋", "🐢", "🐬", "🦉", "🌻", "🌷", "🌵", "🍀",
    "🌿", "🍄", "🌈", "☀️", "🌙", "⭐", "🌼", "🍎", "🍓", "🍉",
    "🍊", "🍋", "🍇", "🍒", "🥑", "🍕", "🍩", "🧁", "🍪", "🍰",
    "😊", "😄", "🥰", "😎", "🤗", "🙂", "✨", "💛", "💙", "❤️"
)

fun contactAvatarEmoji(stableContactId: String): String {
    val stableKey = stableContactId.ifBlank { "unknown-contact" }
    val mixedHash = stableKey.hashCode() * 31 + stableKey.reversed().hashCode()
    return contactAvatarEmojis[Math.floorMod(mixedHash, contactAvatarEmojis.size)]
}

@Composable
fun ContactEmojiAvatar(
    photoUri: String?,
    displayName: String,
    stableContactId: String?,
    modifier: Modifier = Modifier,
    size: Dp,
    fallbackEmoji: String? = null,
    shape: Shape = CircleShape,
) {
    var photoLoaded by remember(photoUri) { mutableStateOf(false) }
    val emoji = fallbackEmoji
        ?: contactAvatarEmoji(stableContactId?.takeIf(String::isNotBlank) ?: displayName)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!photoLoaded) {
            Text(
                text = emoji,
                fontSize = (size.value * 0.48f).coerceIn(18f, 88f).sp,
                textAlign = TextAlign.Center,
                lineHeight = (size.value * 0.58f).sp
            )
        }
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "$displayName photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { photoLoaded = true },
                onError = { photoLoaded = false }
            )
        }
    }
}
