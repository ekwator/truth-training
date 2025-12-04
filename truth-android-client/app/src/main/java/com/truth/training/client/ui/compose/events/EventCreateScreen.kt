package com.truth.training.client.ui.compose.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.ui.compose.components.ContextPicker
import kotlinx.coroutines.flow.Flow
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import android.util.Log

/**
 * Event Create/Edit Screen (Compose) - Form for creating or editing events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreateScreen(
    onSave: (CreateEventRequest) -> Unit,
    onCancel: () -> Unit,
    selectedTemplateContext: ContextFields? = null,
    contextsFlow: Flow<List<ContextTemplateEntity>>,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Int?>(selectedTemplateContext?.categoryId) }
    var formaId by remember { mutableStateOf<Int?>(selectedTemplateContext?.formaId) }
    var causeId by remember { mutableStateOf<Int?>(selectedTemplateContext?.causeId) }
    var developId by remember { mutableStateOf<Int?>(selectedTemplateContext?.developId) }
    var effectId by remember { mutableStateOf<Int?>(selectedTemplateContext?.effectId) }
    var timestampStart by remember { mutableStateOf("") }
    var timestampEnd by remember { mutableStateOf("") }
    var vector by remember { mutableStateOf(true) }
    
    // Load contexts from Flow
    val contexts by contextsFlow.collectAsState(initial = emptyList())
    var contextsLoadError by remember { mutableStateOf<String?>(null) }
    
    // Validation errors for context fields
    var categoryError by remember { mutableStateOf<String?>(null) }
    var formaError by remember { mutableStateOf<String?>(null) }
    var causeError by remember { mutableStateOf<String?>(null) }
    var developError by remember { mutableStateOf<String?>(null) }
    var effectError by remember { mutableStateOf<String?>(null) }
    
    // Check if contexts are available
    LaunchedEffect(contextsFlow) {
        try {
            // Contexts will be loaded via Flow, error handling is done in repository
            contextsLoadError = null
        } catch (e: Exception) {
            contextsLoadError = "Failed to load contexts: ${e.message}"
            Log.e("EventCreateScreen", "Context loading error", e)
        }
    }
    
    // Disable context pickers if data is unavailable
    val contextsAvailable = contexts.isNotEmpty() && contextsLoadError == null

    val canSave = description.isNotBlank() && 
                  timestampStart.toLongOrNull() != null &&
                  categoryError == null && formaError == null && 
                  causeError == null && developError == null && effectError == null

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
                            // Validate context IDs before submission
                            // Validation will be done in EventRepository, but we can do pre-check here
                            onSave(
                                CreateEventRequest(
                                    description = description,
                                    categoryId = categoryId,
                                    formaId = formaId,
                                    causeId = causeId,
                                    developId = developId,
                                    effectId = effectId,
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
            
            // Show error state if contexts are unavailable
            if (contextsLoadError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Context data unavailable",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = contextsLoadError ?: "Unable to load contexts from database",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(
                            onClick = { 
                                contextsLoadError = null
                                // Retry loading - Flow will automatically update
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContextPicker(
                    label = "Category",
                    selectedId = categoryId,
                    onSelectionChange = { 
                        categoryId = it
                        categoryError = null
                    },
                    contextsFlow = contextsFlow,
                    modifier = Modifier.weight(1f),
                    enabled = contextsAvailable,
                    isError = categoryError != null,
                    errorMessage = categoryError
                )
                ContextPicker(
                    label = "Forma",
                    selectedId = formaId,
                    onSelectionChange = { 
                        formaId = it
                        formaError = null
                    },
                    contextsFlow = contextsFlow,
                    modifier = Modifier.weight(1f),
                    enabled = contextsAvailable,
                    isError = formaError != null,
                    errorMessage = formaError
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContextPicker(
                    label = "Cause",
                    selectedId = causeId,
                    onSelectionChange = { 
                        causeId = it
                        causeError = null
                    },
                    contextsFlow = contextsFlow,
                    modifier = Modifier.weight(1f),
                    isError = causeError != null,
                    errorMessage = causeError
                )
                ContextPicker(
                    label = "Develop",
                    selectedId = developId,
                    onSelectionChange = { 
                        developId = it
                        developError = null
                    },
                    contextsFlow = contextsFlow,
                    modifier = Modifier.weight(1f),
                    isError = developError != null,
                    errorMessage = developError
                )
            }

            ContextPicker(
                label = "Effect",
                selectedId = effectId,
                onSelectionChange = { 
                    effectId = it
                    effectError = null
                },
                contextsFlow = contextsFlow,
                modifier = Modifier.fillMaxWidth(),
                isError = effectError != null,
                errorMessage = effectError
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

