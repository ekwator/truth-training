package com.truth.training.client.ui.compose.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.network.dto.UpdateEventRequest

/**
 * Event Edit Screen (Compose) - Form for editing existing events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    event: EventEntity,
    onSave: (String, UpdateEventRequest) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description ?: "") }
    var categoryId by remember { mutableStateOf(event.categoryId?.toString() ?: "") }
    var formaId by remember { mutableStateOf(event.formaId?.toString() ?: "") }
    var causeId by remember { mutableStateOf(event.causeId?.toString() ?: "") }
    var developId by remember { mutableStateOf(event.developId?.toString() ?: "") }
    var effectId by remember { mutableStateOf(event.effectId?.toString() ?: "") }
    var startDate by remember { mutableStateOf(event.startDate ?: "") }
    var endDate by remember { mutableStateOf(event.endDate ?: "") }
    var status by remember { mutableStateOf(event.status) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Event") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                event.id,
                                UpdateEventRequest(
                                    title = title.takeIf { it != event.title },
                                    description = description.takeIf { it.isNotEmpty() },
                                    categoryId = categoryId.toIntOrNull() ?: event.categoryId,
                                    formaId = formaId.toIntOrNull() ?: event.formaId,
                                    causeId = causeId.toIntOrNull() ?: event.causeId,
                                    developId = developId.toIntOrNull() ?: event.developId,
                                    effectId = effectId.toIntOrNull() ?: event.effectId,
                                    startDate = startDate.takeIf { it.isNotEmpty() },
                                    endDate = endDate.takeIf { it.isNotEmpty() },
                                    status = status.takeIf { it != event.status }
                                )
                            )
                        },
                        enabled = title.isNotEmpty()
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Divider()

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
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ID") }
                )
                OutlinedTextField(
                    value = formaId,
                    onValueChange = { formaId = it },
                    label = { Text("Forma ID") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ID") }
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
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ID") }
                )
                OutlinedTextField(
                    value = developId,
                    onValueChange = { developId = it },
                    label = { Text("Develop ID") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ID") }
                )
            }

            OutlinedTextField(
                value = effectId,
                onValueChange = { effectId = it },
                label = { Text("Effect ID") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ID") }
            )

            Divider()

            Text(
                text = "Dates (optional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Start Date (ISO 8601)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("2024-01-01T00:00:00Z") }
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("End Date (ISO 8601)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("2024-01-02T00:00:00Z") }
            )

            Divider()

            Text(
                text = "Status",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = status == "active",
                    onClick = { status = "active" },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = status == "completed",
                    onClick = { status = "completed" },
                    label = { Text("Completed") }
                )
                FilterChip(
                    selected = status == "archived",
                    onClick = { status = "archived" },
                    label = { Text("Archived") }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Event Information",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ID: ${event.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Created: ${event.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    event.updatedAt?.let {
                        Text(
                            text = "Updated: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

