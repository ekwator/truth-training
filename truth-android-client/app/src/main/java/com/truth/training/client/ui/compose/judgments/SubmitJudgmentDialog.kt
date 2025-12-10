package com.truth.training.client.ui.compose.judgments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.utils.EmojiMapping

/**
 * Dialog for submitting a judgment for an event.
 * Allows user to select assessment (true/false/uncertain), set confidence level (0.0-1.0), and add optional reasoning.
 */
@Composable
fun SubmitJudgmentDialog(
    eventId: Long,
    onSubmit: (assessment: String, confidenceLevel: Double, reasoning: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var assessment by remember { mutableStateOf("true") }
    var confidenceLevel by remember { mutableStateOf("0.8") }
    var reasoning by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val confidence = confidenceLevel.toDoubleOrNull() ?: 0.5
    val isValid = remember(assessment, confidenceLevel) {
        assessment in listOf("true", "false", "uncertain") &&
        confidence >= 0.0 && confidence <= 1.0
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${EmojiMapping.getEmoji("screens", "judgments")} ${context.getString(R.string.submit_judgment)}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Assessment Selection
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "assessment")} ${context.getString(R.string.assessment)} *",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = assessment == "true",
                        onClick = { assessment = "true" },
                        label = { Text(context.getString(R.string.assessment_true)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = assessment == "false",
                        onClick = { assessment = "false" },
                        label = { Text(context.getString(R.string.assessment_false)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = assessment == "uncertain",
                        onClick = { assessment = "uncertain" },
                        label = { Text(context.getString(R.string.uncertain)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Confidence Level
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "confidence")} ${context.getString(R.string.confidence_level)} *",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = confidenceLevel,
                    onValueChange = { newValue ->
                        val doubleValue = newValue.toDoubleOrNull()
                        if (doubleValue == null || (doubleValue in 0.0..1.0)) {
                            confidenceLevel = newValue
                        }
                    },
                    label = { Text("0.0 - 1.0") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.8") },
                    supportingText = {
                        val value = confidenceLevel.toDoubleOrNull()
                        if (value != null && value in 0.0..1.0) {
                            Text(context.getString(R.string.confidence_percent, (value * 100).toInt()))
                        } else {
                            Text(context.getString(R.string.must_be_between), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    isError = confidenceLevel.toDoubleOrNull()?.let { it !in 0.0..1.0 } ?: false
                )
                
                Slider(
                    value = confidence.coerceIn(0.0, 1.0).toFloat(),
                    onValueChange = { confidenceLevel = it.toString() },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Reasoning Field
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "reasoning")} ${context.getString(R.string.reasoning)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = reasoning,
                    onValueChange = { reasoning = it },
                    label = { Text(context.getString(R.string.reasoning)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text(context.getString(R.string.reasoning_placeholder)) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onSubmit(assessment, confidence, reasoning.takeIf { it.isNotBlank() })
                    }
                },
                enabled = isValid
            ) {
                Text("${EmojiMapping.getEmoji("actions", "submit")} ${context.getString(R.string.submit)}")
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

