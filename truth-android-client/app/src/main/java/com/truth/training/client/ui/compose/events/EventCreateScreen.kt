package com.truth.training.client.ui.compose.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.network.dto.CreateEventRequest

/**
 * Event Create/Edit Screen (Compose) - Form for creating or editing events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreateScreen(
    onSave: (CreateEventRequest) -> Unit,
    onCancel: () -> Unit,
    selectedTemplateContext: ContextFields? = null,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(selectedTemplateContext?.categoryId?.toString() ?: "") }
    var formaId by remember { mutableStateOf(selectedTemplateContext?.formaId?.toString() ?: "") }
    var causeId by remember { mutableStateOf(selectedTemplateContext?.causeId?.toString() ?: "") }
    var developId by remember { mutableStateOf(selectedTemplateContext?.developId?.toString() ?: "") }
    var effectId by remember { mutableStateOf(selectedTemplateContext?.effectId?.toString() ?: "") }
    var timestampStart by remember { mutableStateOf("") }
    var timestampEnd by remember { mutableStateOf("") }
    var vector by remember { mutableStateOf(true) }

    val canSave = description.isNotBlank() && timestampStart.toLongOrNull() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Event") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                CreateEventRequest(
                                    description = description,
                                    categoryId = categoryId.toIntOrNull(),
                                    formaId = formaId.toIntOrNull(),
                                    causeId = causeId.toIntOrNull(),
                                    developId = developId.toIntOrNull(),
                                    effectId = effectId.toIntOrNull(),
                                    vector = vector,
                                    timestampStart = timestampStart.toLong(),
                                    timestampEnd = timestampEnd.toLongOrNull()
                                )
                            )
                        },
                        enabled = canSave
                    ) {
                        Text("Save")
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
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            HorizontalDivider()

            Text(
                text = "Context Fields (optional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = categoryId,
                    onValueChange = { categoryId = it },
                    label = { Text("Category ID") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formaId,
                    onValueChange = { formaId = it },
                    label = { Text("Forma ID") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = causeId,
                    onValueChange = { causeId = it },
                    label = { Text("Cause ID") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = developId,
                    onValueChange = { developId = it },
                    label = { Text("Develop ID") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = effectId,
                onValueChange = { effectId = it },
                label = { Text("Effect ID") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = timestampStart,
                onValueChange = { timestampStart = it },
                label = { Text("Start Timestamp (epoch ms) *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = timestampEnd,
                onValueChange = { timestampEnd = it },
                label = { Text("End Timestamp (epoch ms)") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text(
                text = "Direction",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vector,
                    onClick = { vector = true },
                    label = { Text("Outgoing") }
                )
                FilterChip(
                    selected = !vector,
                    onClick = { vector = false },
                    label = { Text("Incoming") }
                )
            }

            if (selectedTemplateContext != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Context fields prefilled from template",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Context fields from template (for prefilling).
 */
data class ContextFields(
    val categoryId: Int? = null,
    val formaId: Int? = null,
    val causeId: Int? = null,
    val developId: Int? = null,
    val effectId: Int? = null
)

