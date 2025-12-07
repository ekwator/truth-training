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
import com.truth.training.client.ui.compose.components.ContextPicker
import com.truth.training.client.ui.compose.components.DatePickerField
import kotlinx.coroutines.flow.Flow
import com.truth.training.client.data.database.entities.*
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
 * Extract name from description (first line before "\n\n")
 */
private fun extractNameFromDescription(description: String): String {
    val parts = description.split("\n\n", limit = 2)
    return parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: ""
}

/**
 * Extract description without name (everything after "\n\n")
 */
private fun extractDescriptionWithoutName(description: String): String {
    val parts = description.split("\n\n", limit = 2)
    return if (parts.size > 1) parts[1] else description
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    event: EventEntity,
    onSave: (Long, UpdateEventRequest) -> Unit,
    onCancel: () -> Unit,
    categoriesFlow: Flow<List<CategoryEntity>>,
    formasFlow: Flow<List<FormaEntity>>,
    causesFlow: Flow<List<CauseEntity>>,
    developsFlow: Flow<List<DevelopEntity>>,
    effectsFlow: Flow<List<EffectEntity>>,
    modifier: Modifier = Modifier
) {
    // Extract name and description from event.description
    val initialName = remember(event) { extractNameFromDescription(event.description) }
    val initialDescription = remember(event) { extractDescriptionWithoutName(event.description) }
    
    // Only Flags and Timestamp fields are editable
    // Name and Description are read-only
    var detected by remember { mutableStateOf(event.detected ?: false) }
    
    // Timestamp fields according to specification:
    // - Start Timestamp: not available for editing
    // - End Timestamp: always available for editing, defaults to current date (if not filled)
    
    // Initialize timestampStart: always use existing value (read-only)
    val timestampStart = event.timestampStart.takeIf { it > 0L }
    
    // Save initial End Timestamp value for change detection
    val initialTimestampEnd = remember(event) { event.timestampEnd }
    
    // End Timestamp: defaults to current date if not filled
    var timestampEnd by remember(event) { 
        mutableStateOf<Long?>(
            event.timestampEnd ?: System.currentTimeMillis()
        )
    }
    
    // Validation: End Timestamp cannot be less than Start Timestamp
    var timestampEndError by remember { mutableStateOf<String?>(null) }
    
    // Corrected is set automatically:
    // - If End Timestamp was empty before editing → Corrected is not set
    // - If End Timestamp was set and changed → Corrected is automatically set
    val corrected = remember(timestampEnd, initialTimestampEnd) {
        if (initialTimestampEnd == null) {
            // If End Timestamp was initially empty, Corrected is not set
            event.corrected
        } else {
            // If End Timestamp was set and changed, Corrected is automatically set
            if (timestampEnd != null && timestampEnd != initialTimestampEnd) {
                true
            } else {
                event.corrected
            }
        }
    }
    
    val canSave = (detected != (event.detected ?: false) || 
                   corrected != event.corrected ||
                   (timestampEnd != null && timestampEnd != initialTimestampEnd && timestampEndError == null))

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
                                    description = null, // Name and description are not editable
                                    categoryId = null,
                                    formaId = null,
                                    causeId = null,
                                    developId = null,
                                    effectId = null,
                                    vector = null, // Not editable
                                    detected = detected.takeIf { event.detected != it },
                                    corrected = corrected.takeIf { event.corrected != it },
                                    timestampStart = null, // Start Timestamp is not editable
                                    timestampEnd = timestampEnd?.takeIf { it != initialTimestampEnd && timestampEndError == null },
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
            // Name field (read-only)
            OutlinedTextField(
                value = initialName,
                onValueChange = { },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                readOnly = true,
                singleLine = true
            )
            
            // Description field (read-only)
            OutlinedTextField(
                value = initialDescription,
                onValueChange = { },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                readOnly = true,
                minLines = 3,
                maxLines = 6
            )

            HorizontalDivider()

            // Context Fields (read-only, display names instead of IDs)
            Text(
                text = "Context Fields",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContextPicker(
                    label = "Category",
                    selectedId = event.categoryId,
                    onSelectionChange = { },
                    entitiesFlow = categoriesFlow,
                    modifier = Modifier.weight(1f),
                    enabled = false
                )
                ContextPicker(
                    label = "Forma",
                    selectedId = event.formaId,
                    onSelectionChange = { },
                    entitiesFlow = formasFlow,
                    modifier = Modifier.weight(1f),
                    enabled = false
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContextPicker(
                    label = "Cause",
                    selectedId = event.causeId,
                    onSelectionChange = { },
                    entitiesFlow = causesFlow,
                    modifier = Modifier.weight(1f),
                    enabled = false
                )
                ContextPicker(
                    label = "Develop",
                    selectedId = event.developId,
                    onSelectionChange = { },
                    entitiesFlow = developsFlow,
                    modifier = Modifier.weight(1f),
                    enabled = false
                )
            }

            ContextPicker(
                label = "Effect",
                selectedId = event.effectId,
                onSelectionChange = { },
                entitiesFlow = effectsFlow,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            HorizontalDivider()

            // Timestamp fields (editable only if not already filled)
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            DatePickerField(
                label = "Start Timestamp",
                selectedDate = timestampStart,
                onDateSelected = { },
                modifier = Modifier.fillMaxWidth(),
                enabled = false // Start Timestamp is not available for editing
            )

            DatePickerField(
                label = "End Timestamp *",
                selectedDate = timestampEnd,
                onDateSelected = { newDate ->
                    // Validation: End Timestamp cannot be less than Start Timestamp, but can be equal
                    if (timestampStart != null) {
                        val normalizedStart = normalizeToStartOfDay(timestampStart)
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
                enabled = true, // End Timestamp is always available for editing
                isError = timestampEndError != null,
                errorMessage = timestampEndError
                // allowClear = false - no clear function
            )

            HorizontalDivider()

            // Direction (read-only)
            Text(
                text = "Direction",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = event.vector,
                    onClick = { },
                    enabled = false,
                    label = { Text(if (event.vector) "Outgoing" else "Incoming") }
                )
            }

            // Flags
            Text(
                text = "Flags",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = detected,
                    onClick = { detected = !detected },
                    label = { Text("Detected") }
                )
                FilterChip(
                    selected = corrected,
                    onClick = { }, // Corrected is not available for editing
                    enabled = false, // Corrected is set automatically
                    label = { Text("Corrected") }
                )
            }
        }
    }
}
