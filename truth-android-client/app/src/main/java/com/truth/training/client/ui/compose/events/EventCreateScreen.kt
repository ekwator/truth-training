package com.truth.training.client.ui.compose.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.ui.compose.components.ContextPicker
import com.truth.training.client.ui.compose.components.DatePickerField
import kotlinx.coroutines.flow.Flow
import com.truth.training.client.data.database.entities.*
import android.util.Log
import java.util.Calendar

/**
 * Normalizes date to start of day (00:00:00) for correct date comparison without time
 */
private fun normalizeToStartOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

/**
 * Event Create/Edit Screen (Compose) - Form for creating or editing events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreateScreen(
    onSave: (CreateEventRequest) -> Unit,
    onCancel: () -> Unit,
    selectedTemplateContext: ContextFields? = null,
    onSelectTemplate: () -> Unit = {},
    categoriesFlow: Flow<List<CategoryEntity>>,
    formasFlow: Flow<List<FormaEntity>>,
    causesFlow: Flow<List<CauseEntity>>,
    developsFlow: Flow<List<DevelopEntity>>,
    effectsFlow: Flow<List<EffectEntity>>,
    modifier: Modifier = Modifier
) {
    // Use rememberSaveable to preserve state during navigation
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    
    // Initialize state with selectedTemplateContext, but allow manual changes
    // Use rememberSaveable to preserve state during navigation
    var categoryId by rememberSaveable { mutableStateOf<Int?>(selectedTemplateContext?.categoryId) }
    var formaId by rememberSaveable { mutableStateOf<Int?>(selectedTemplateContext?.formaId) }
    var causeId by rememberSaveable { mutableStateOf<Int?>(selectedTemplateContext?.causeId) }
    var developId by rememberSaveable { mutableStateOf<Int?>(selectedTemplateContext?.developId) }
    var effectId by rememberSaveable { mutableStateOf<Int?>(selectedTemplateContext?.effectId) }
    
    // Start Timestamp: defaults to current date, cannot be empty
    var timestampStart by rememberSaveable { 
        mutableStateOf<Long?>(System.currentTimeMillis()) 
    }
    // End Timestamp: can be empty, with clear capability
    var timestampEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var vector by rememberSaveable { mutableStateOf(true) }
    
    // Validation: End Timestamp cannot be less than Start Timestamp
    var timestampEndError by remember { mutableStateOf<String?>(null) }
    
    // Track if template was applied to prevent clearing fields on navigation
    var templateApplied by remember { mutableStateOf(false) }
    
    // Update fields when selectedTemplateContext changes
    // Only update if template is selected (not null) and hasn't been applied yet
    LaunchedEffect(selectedTemplateContext) {
        selectedTemplateContext?.let { context ->
            // Only update if this is a new template selection
            // Don't clear existing values if template is null (user might have manually changed them)
            categoryId = context.categoryId
            formaId = context.formaId
            causeId = context.causeId
            developId = context.developId
            effectId = context.effectId
            templateApplied = true
        }
        // Don't clear fields if template is null - user might have manually filled them
    }
    
    // Load knowledge base entities from Flow
    val categories by categoriesFlow.collectAsState(initial = emptyList())
    val formas by formasFlow.collectAsState(initial = emptyList())
    val causes by causesFlow.collectAsState(initial = emptyList())
    val develops by developsFlow.collectAsState(initial = emptyList())
    val effects by effectsFlow.collectAsState(initial = emptyList())
    
    var dataLoadError by remember { mutableStateOf<String?>(null) }
    
    // Validation errors for context fields
    var categoryError by remember { mutableStateOf<String?>(null) }
    var formaError by remember { mutableStateOf<String?>(null) }
    var causeError by remember { mutableStateOf<String?>(null) }
    var developError by remember { mutableStateOf<String?>(null) }
    var effectError by remember { mutableStateOf<String?>(null) }
    
    // Check if knowledge base data is available
    val dataAvailable = categories.isNotEmpty() || formas.isNotEmpty() || 
                        causes.isNotEmpty() || develops.isNotEmpty() || effects.isNotEmpty()

    val canSave = description.isNotBlank() && 
                  timestampStart != null &&
                  timestampEndError == null &&
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
                            // Combine name and description if name is provided
                            val finalDescription = if (name.isNotBlank()) {
                                "$name\n\n$description"
                            } else {
                                description
                            }
                            onSave(
                                CreateEventRequest(
                                    description = finalDescription,
                                    categoryId = categoryId,
                                    formaId = formaId,
                                    causeId = causeId,
                                    developId = developId,
                                    effectId = effectId,
                                    vector = vector,
                                    timestampStart = timestampStart!!,
                                    timestampEnd = timestampEnd
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Context Fields (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onSelectTemplate) {
                    Text("Select Template")
                }
            }
            
            // Show warning if knowledge base data is unavailable
            if (!dataAvailable) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Knowledge base data unavailable",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Please ensure database connection is available. Context fields may be empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    entitiesFlow = categoriesFlow,
                    modifier = Modifier.weight(1f),
                    enabled = dataAvailable,
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
                    entitiesFlow = formasFlow,
                    modifier = Modifier.weight(1f),
                    enabled = dataAvailable,
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
                    entitiesFlow = causesFlow,
                    modifier = Modifier.weight(1f),
                    enabled = dataAvailable,
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
                    entitiesFlow = developsFlow,
                    modifier = Modifier.weight(1f),
                    enabled = dataAvailable,
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
                entitiesFlow = effectsFlow,
                modifier = Modifier.fillMaxWidth(),
                enabled = dataAvailable,
                isError = effectError != null,
                errorMessage = effectError
            )

            HorizontalDivider()

            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            DatePickerField(
                label = "Start Timestamp *",
                selectedDate = timestampStart,
                onDateSelected = { newDate ->
                    timestampStart = newDate
                    // Validation check after Start Timestamp change
                    // End Timestamp can be empty or equal to Start, but cannot be less than Start
                    if (timestampEnd != null) {
                        val normalizedStart = normalizeToStartOfDay(newDate)
                        val normalizedEnd = normalizeToStartOfDay(timestampEnd!!)
                        if (normalizedEnd < normalizedStart) {
                            timestampEndError = "End Timestamp cannot be less than Start Timestamp (can be equal)"
                        } else {
                            timestampEndError = null
                        }
                    } else {
                        timestampEndError = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                label = "End Timestamp",
                selectedDate = timestampEnd,
                onDateSelected = { newDate ->
                    // Validation: End Timestamp cannot be less than Start Timestamp, but can be equal
                    if (timestampStart != null) {
                        val normalizedStart = normalizeToStartOfDay(timestampStart!!)
                        val normalizedEnd = normalizeToStartOfDay(newDate)
                        if (normalizedEnd < normalizedStart) {
                            timestampEndError = "End Timestamp cannot be less than Start Timestamp (can be equal)"
                        } else {
                            timestampEnd = newDate
                            timestampEndError = null
                        }
                    } else {
                        timestampEnd = newDate
                        timestampEndError = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                allowClear = true,
                onDateCleared = {
                    timestampEnd = null
                    timestampEndError = null
                },
                isError = timestampEndError != null,
                errorMessage = timestampEndError
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

