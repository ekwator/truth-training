package com.truth.training.client.ui.compose.impacts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.utils.EmojiMapping
import com.truth.training.client.utils.ImpactLevelMapper

/**
 * Dialog for adding an impact to an event.
 * Allows user to set impact level (1-5) and optional notes.
 */
@Composable
fun AddImpactDialog(
    eventId: Long,
    onSave: (impactLevel: Int, notes: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var impactLevel by remember { mutableStateOf(3) }
    var notes by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val isValid = remember(impactLevel) {
        ImpactLevelMapper.isValid(impactLevel)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${EmojiMapping.getEmoji("screens", "events")} ${context.getString(R.string.add_impact)}")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Impact Level Label
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "assessment")} ${context.getString(R.string.impact_level)} *",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Impact Level Display
                Text(
                    text = context.getString(R.string.impact_level_label, impactLevel),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Impact Level Slider (1-5)
                Slider(
                    value = impactLevel.toFloat(),
                    onValueChange = { impactLevel = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3, // 1, 2, 3, 4, 5 (4 steps between 5 values)
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Notes Field
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "reasoning")} ${context.getString(R.string.impact_notes)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(context.getString(R.string.impact_notes)) },
                    placeholder = { Text(context.getString(R.string.impact_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onSave(impactLevel, notes.takeIf { it.isNotBlank() })
                    }
                },
                enabled = isValid
            ) {
                Text("${EmojiMapping.getEmoji("actions", "save")} ${context.getString(R.string.save_impact)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("${EmojiMapping.getEmoji("actions", "cancel")} ${context.getString(R.string.cancel)}")
            }
        },
        modifier = modifier
    )
}

