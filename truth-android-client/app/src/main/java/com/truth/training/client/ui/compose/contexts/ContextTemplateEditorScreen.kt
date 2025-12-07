package com.truth.training.client.ui.compose.contexts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.network.dto.CreateContextRequest
import com.truth.training.client.ui.compose.components.ContextPicker
import com.truth.training.client.data.database.entities.*
import kotlinx.coroutines.flow.Flow

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
    categoriesFlow: Flow<List<CategoryEntity>>,
    formasFlow: Flow<List<FormaEntity>>,
    causesFlow: Flow<List<CauseEntity>>,
    developsFlow: Flow<List<DevelopEntity>>,
    effectsFlow: Flow<List<EffectEntity>>,
    onSave: (CreateContextRequest) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialName) }
    var categoryId by remember { mutableStateOf<Int?>(initialCategoryId) }
    var formaId by remember { mutableStateOf<Int?>(initialFormaId) }
    var causeId by remember { mutableStateOf<Int?>(initialCauseId) }
    var developId by remember { mutableStateOf<Int?>(initialDevelopId) }
    var effectId by remember { mutableStateOf<Int?>(initialEffectId) }
    var description by remember { mutableStateOf(initialDescription) }
    var duplicateError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId == null) "New Template" else "Edit Template") },
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
                            try {
                                onSave(
                                    CreateContextRequest(
                                        name = name,
                                        categoryId = categoryId,
                                        formaId = formaId,
                                        causeId = causeId,
                                        developId = developId,
                                        effectId = effectId,
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

            HorizontalDivider()

            Text(
                text = "Context Fields (optional, NULL values ignored in duplicate detection)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContextPicker(
                    label = "Category",
                    selectedId = categoryId,
                    onSelectionChange = { categoryId = it },
                    entitiesFlow = categoriesFlow,
                    modifier = Modifier.weight(1f)
                )
                ContextPicker(
                    label = "Forma",
                    selectedId = formaId,
                    onSelectionChange = { formaId = it },
                    entitiesFlow = formasFlow,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContextPicker(
                    label = "Cause",
                    selectedId = causeId,
                    onSelectionChange = { causeId = it },
                    entitiesFlow = causesFlow,
                    modifier = Modifier.weight(1f)
                )
                ContextPicker(
                    label = "Develop",
                    selectedId = developId,
                    onSelectionChange = { developId = it },
                    entitiesFlow = developsFlow,
                    modifier = Modifier.weight(1f)
                )
            }

            ContextPicker(
                label = "Effect",
                selectedId = effectId,
                onSelectionChange = { effectId = it },
                entitiesFlow = effectsFlow,
                modifier = Modifier.fillMaxWidth()
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

