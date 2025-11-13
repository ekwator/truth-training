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
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.network.dto.UpdateEventRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    event: EventEntity,
    onSave: (Long, UpdateEventRequest) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf(event.description) }
    var categoryId by remember { mutableStateOf(event.categoryId?.toString() ?: "") }
    var formaId by remember { mutableStateOf(event.formaId?.toString() ?: "") }
    var causeId by remember { mutableStateOf(event.causeId?.toString() ?: "") }
    var developId by remember { mutableStateOf(event.developId?.toString() ?: "") }
    var effectId by remember { mutableStateOf(event.effectId?.toString() ?: "") }
    var timestampStart by remember { mutableStateOf(event.timestampStart.toString()) }
    var timestampEnd by remember { mutableStateOf(event.timestampEnd?.toString() ?: "") }
    var vector by remember { mutableStateOf(event.vector) }
    var detected by remember { mutableStateOf(event.detected ?: false) }
    var corrected by remember { mutableStateOf(event.corrected) }

    val canSave = description.isNotBlank() && timestampStart.toLongOrNull() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Event") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                event.id,
                                UpdateEventRequest(
                                    description = description.takeIf { it != event.description },
                                    categoryId = categoryId.toIntOrNull()?.takeIf { it != event.categoryId },
                                    formaId = formaId.toIntOrNull()?.takeIf { it != event.formaId },
                                    causeId = causeId.toIntOrNull()?.takeIf { it != event.causeId },
                                    developId = developId.toIntOrNull()?.takeIf { it != event.developId },
                                    effectId = effectId.toIntOrNull()?.takeIf { it != event.effectId },
                                    vector = vector.takeIf { it != event.vector },
                                    detected = detected.takeIf { event.detected != it },
                                    corrected = corrected.takeIf { event.corrected != it },
                                    timestampStart = timestampStart.toLongOrNull()?.takeIf { it != event.timestampStart },
                                    timestampEnd = timestampEnd.toLongOrNull()?.takeIf { it != event.timestampEnd },
                                    code = null,
                                    collectiveScore = null
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

            Text("Context Fields", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Text("Timestamps", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

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

            Text("Direction", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = vector, onClick = { vector = true }, label = { Text("Outgoing") })
                FilterChip(selected = !vector, onClick = { vector = false }, label = { Text("Incoming") })
            }

            Text("Flags", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = detected, onClick = { detected = !detected }, label = { Text("Detected") })
                FilterChip(selected = corrected, onClick = { corrected = !corrected }, label = { Text("Corrected") })
            }
        }
    }
}

