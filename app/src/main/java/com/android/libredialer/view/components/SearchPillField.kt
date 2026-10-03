package com.android.libredialer.view.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

@Composable
fun SearchPillInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    leadingAction: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = androidx.compose.material3.LocalTextStyle.current,
    placeholderTextAlign: TextAlign = TextAlign.Start,
    placeholderTextOffsetX: Dp = 0.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val placeholderOffsetXPx = with(LocalDensity.current) { placeholderTextOffsetX.roundToPx() }
    SearchPillContent(modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            placeholder = {
                Text(
                    placeholder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(placeholderOffsetXPx, 0) },
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = placeholderTextAlign
                )
            },
            leadingIcon = leadingAction ?: {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = searchPillTextFieldColors(),
            singleLine = true,
            maxLines = 1
        )
    }

}

@Composable
fun SearchPillContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SearchPillContent(modifier, content)
}

@Composable
fun SearchPillInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    leadingAction: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    SearchPillContent(modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = leadingAction ?: {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = searchPillTextFieldColors(),
            singleLine = true,
            maxLines = 1
        )
    }
}

@Composable
private fun SearchPillContent(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f)
    ) {
        content()
    }
}

@Composable
private fun searchPillTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Composable
fun SearchClearButton(
    onClick: () -> Unit,
    contentDescription: String = "Clear search",
    modifier: Modifier = Modifier,
    buttonSize: Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Close,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
