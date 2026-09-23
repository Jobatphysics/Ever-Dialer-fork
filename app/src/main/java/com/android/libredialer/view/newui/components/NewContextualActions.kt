package com.android.libredialer.view.newui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class NewContextualAction(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun NewContextualActionsDialog(
    title: String,
    actions: List<NewContextualAction>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                actions.forEach { action ->
                    TextButton(
                        onClick = {
                            onDismiss()
                            action.onClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(action.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
