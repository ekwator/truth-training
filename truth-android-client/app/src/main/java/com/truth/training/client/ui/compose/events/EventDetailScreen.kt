package com.truth.training.client.ui.compose.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.database.entities.EventEntity

/**
 * Event Detail Screen (Compose) - Displays full event details with judgments and impacts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: EventEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToJudgments: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (event == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
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
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                            contentDescription = "Delete"
                        )
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
            // Title
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Status chip
            AssistChip(
                onClick = {},
                label = { Text(event.status) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = when (event.status) {
                        "active" -> MaterialTheme.colorScheme.primaryContainer
                        "inactive" -> MaterialTheme.colorScheme.surfaceVariant
                        "archived" -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            )

            // Description
            if (!event.description.isNullOrEmpty()) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Context fields
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    contextFields.forEach { field ->
                        AssistChip(
                            onClick = {},
                            label = { Text(field) }
                        )
                    }
                }
            }

            // Dates
            if (event.startDate != null || event.endDate != null) {
                Text(
                    text = "Dates",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (event.startDate != null) {
                    Text(
                        text = "Start: ${event.startDate}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (event.endDate != null) {
                    Text(
                        text = "End: ${event.endDate}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Divider()

            // Actions
            Button(
                onClick = onNavigateToJudgments,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Judgments")
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Event")
            }

            // Metadata
            Text(
                text = "Created: ${event.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (event.updatedAt != null) {
                Text(
                    text = "Updated: ${event.updatedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

