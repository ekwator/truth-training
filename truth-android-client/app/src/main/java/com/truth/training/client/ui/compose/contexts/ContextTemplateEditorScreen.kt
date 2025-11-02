package com.truth.training.client.ui.compose.contexts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.network.dto.CreateContextRequest

/**
 * Context Template Editor Screen (Compose) - Form for creating or editing templates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextTemplateEditorScreen(
    templateId: Int? = null,
    initialName: String = "",
    initialCategoryId: Int? = null,
    initialFormaId: Int? = null,
    initialCauseId: Int? = null,
    initialDevelopId: Int? = null,
    initialEffectId: Int? = null,
    initialDescription: String = "",
    onSave: (CreateContextRequest) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialName) }
    var categoryId by remember { mutableStateOf(initialCategoryId?.toString() ?: "") }
    var formaId by remember { mutableStateOf(initialFormaId?.toString() ?: "") }
    var causeId by remember { mutableStateOf(initialCauseId?.toString() ?: "") }
    var developId by remember { mutableStateOf(initialDevelopId?.toString() ?: "") }
    var effectId by remember { mutableStateOf(initialEffectId?.toString() ?: "") }
    var description by remember { mutableStateOf(initialDescription) }
    var duplicateError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId == null) "New Template" else "Edit Template") },
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
                            try {
                                onSave(
                                    CreateContextRequest(
                                        name = name,
                                        categoryId = categoryId.toIntOrNull(),
                                        formaId = formaId.toIntOrNull(),
                                        causeId = causeId.toIntOrNull(),
                                        developId = developId.toIntOrNull(),
                                        effectId = effectId.toIntOrNull(),
                                        description = description.takeIf { it.isNotEmpty() }
                                    )
                                )
                                duplicateError = null
                            } catch (e: Exception) {
                                if (e.message?.contains("409") == true || e.message?.contains("duplicate") == true) {
                                    duplicateError = "Template with identical fields already exists"
                                } else {
                                    duplicateError = e.message ?: "Error saving template"
                                }
                            }
                        },
                        enabled = name.isNotEmpty()
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
            if (duplicateError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = duplicateError!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Template Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isEmpty()
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
                text = "Context Fields (optional, NULL values ignored in duplicate detection)",
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

            Text(
                text = "Note: Templates with identical non-NULL fields cannot be created (409 Conflict)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

