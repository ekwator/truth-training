package com.truth.training.client.ui.compose.judgments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.network.dto.CreateJudgmentRequest

/**
 * Judgment Submission Screen (Compose) - Form for submitting judgments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JudgmentSubmissionScreen(
    eventId: String,
    eventTitle: String,
    onSubmit: (CreateJudgmentRequest) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var assessment by remember { mutableStateOf("true") }
    var confidenceLevel by remember { mutableStateOf("0.8") }
    var reasoning by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submit Judgment") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Cancel"
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
                        Text("Submit")
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
                        text = "Event",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = eventTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Assessment *",
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
                    label = { Text("True") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = assessment == "false",
                    onClick = { assessment = "false" },
                    label = { Text("False") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = assessment == "uncertain",
                    onClick = { assessment = "uncertain" },
                    label = { Text("Uncertain") },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Confidence Level *",
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
                        Text("${(value * 100).toInt()}% confidence")
                    } else {
                        Text("Must be between 0.0 and 1.0", color = MaterialTheme.colorScheme.error)
                    }
                },
                isError = confidenceLevel.toDoubleOrNull()?.let { it !in 0.0..1.0 } ?: false
            )

            Slider(
                value = confidenceLevel.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.5f,
                onValueChange = { confidenceLevel = it.toString() },
                valueRange = 0f..1f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Reasoning (optional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = reasoning,
                onValueChange = { reasoning = it },
                label = { Text("Reasoning") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("Explain your assessment...") }
            )

            Text(
                text = "Collective Intelligence: Your judgment contributes to truth convergence and consensus.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

