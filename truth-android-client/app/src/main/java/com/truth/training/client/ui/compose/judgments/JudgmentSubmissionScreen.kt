package com.truth.training.client.ui.compose.judgments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.data.network.dto.CreateJudgmentRequest
import com.truth.training.client.utils.EmojiMapping

/**
 * Judgment Submission Screen (Compose) - Form for submitting judgments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JudgmentSubmissionScreen(
    eventId: Long,
    eventDescription: String,
    onSubmit: (CreateJudgmentRequest) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var assessment by remember { mutableStateOf("true") }
    var confidenceLevel by remember { mutableStateOf("0.8") }
    var reasoning by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${EmojiMapping.getEmoji("screens", "judgments")} ${context.getString(R.string.submit_judgment)}") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = context.getString(R.string.cancel)
                    )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val confidence = confidenceLevel.toDoubleOrNull() ?: 0.5
                            if (confidence in 0.0..1.0) {
                                onSubmit(
                                    CreateJudgmentRequest(
                                        eventId = eventId,
                                        assessment = assessment,
                                        confidenceLevel = confidence,
                                        reasoning = reasoning.takeIf { it.isNotEmpty() }
                                    )
                                )
                            }
                        },
                        enabled = assessment in listOf("true", "false", "uncertain")
                    ) {
                        Text("${EmojiMapping.getEmoji("actions", "submit")} ${context.getString(R.string.submit)}")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = context.getString(R.string.events),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = eventDescription,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

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
                value = (confidenceLevel.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.5).toFloat(),
                onValueChange = { confidenceLevel = it.toString() },
                valueRange = 0f..1f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${EmojiMapping.getEmoji("fields", "reasoning")} ${context.getString(R.string.reasoning)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = reasoning,
                onValueChange = { reasoning = it },
                label = { Text("${EmojiMapping.getEmoji("fields", "reasoning")} ${context.getString(R.string.reasoning)}") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text(context.getString(R.string.reasoning_placeholder)) }
            )

            Text(
                text = context.getString(R.string.collective_intelligence_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

