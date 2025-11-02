package com.truth.training.client.ui.compose.contexts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import com.truth.training.client.ui.compose.events.ContextFields

/**
 * Context Template Selection Screen (Compose) - Allows selecting a template for event creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextTemplateSelectionScreen(
    templates: List<ContextTemplateEntity>,
    onTemplateSelected: (ContextFields) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Template") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (templates.isEmpty()) {
            Box(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No templates available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Select a template to prefill context fields:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(templates, key = { it.id }) { template ->
                    TemplateSelectionCard(
                        template = template,
                        onClick = {
                            onTemplateSelected(
                                ContextFields(
                                    categoryId = template.categoryId,
                                    formaId = template.formaId,
                                    causeId = template.causeId,
                                    developId = template.developId,
                                    effectId = template.effectId
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateSelectionCard(
    template: ContextTemplateEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!template.description.isNullOrEmpty()) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            
            val contextFields = listOfNotNull(
                template.categoryId?.let { "Cat: $it" },
                template.formaId?.let { "Form: $it" },
                template.causeId?.let { "Cause: $it" },
                template.developId?.let { "Dev: $it" },
                template.effectId?.let { "Eff: $it" }
            )
            
            if (contextFields.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    contextFields.forEach { field ->
                        AssistChip(
                            onClick = {},
                            label = { Text(field, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            } else {
                Text(
                    text = "Empty template (no context fields)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

