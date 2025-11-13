package com.truth.training.client.ui.compose.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.database.entities.EventEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    event: EventEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToJudgments: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (event == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
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
            Text(
                text = event.description,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            AssistChip(
                onClick = {},
                label = { Text(if (event.vector) "Outgoing" else "Incoming") }
            )

            val contextFields = listOfNotNull(
                event.categoryId?.let { "Category: $it" },
                event.formaId?.let { "Forma: $it" },
                event.causeId?.let { "Cause: $it" },
                event.developId?.let { "Develop: $it" },
                event.effectId?.let { "Effect: $it" }
            )
            if (contextFields.isNotEmpty()) {
                Text(
                    text = "Context Fields",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contextFields.forEach { field ->
                        AssistChip(onClick = {}, label = { Text(field) })
                    }
                }
            }

            Text(
                text = "Timestamps",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Start: ${event.timestampStart}",
                style = MaterialTheme.typography.bodyMedium
            )
            event.timestampEnd?.let {
                Text(
                    text = "End: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val stateChips = listOfNotNull(
                event.detected?.let { if (it) "Detected" else "Not Detected" },
                if (event.corrected) "Corrected" else null
            )
            if (stateChips.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stateChips.forEach { label ->
                        AssistChip(onClick = {}, label = { Text(label) })
                    }
                }
            }

            Divider()

            Button(onClick = onNavigateToJudgments, modifier = Modifier.fillMaxWidth()) {
                Text("View Judgments")
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit Event")
            }
        }
    }
}

