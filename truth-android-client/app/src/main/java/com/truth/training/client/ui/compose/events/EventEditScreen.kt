package com.truth.training.client.ui.compose.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.network.dto.UpdateEventRequest
import com.truth.training.client.ui.compose.components.DatePickerField
import com.truth.training.client.utils.EmojiMapping
import kotlinx.coroutines.flow.Flow
import com.truth.training.client.data.database.entities.*
import java.util.Calendar

/**
 * Helper function to get entity name by ID from a list
 */
private fun <T> getEntityNameById(
    id: Int?,
    entities: List<T>,
    getId: (T) -> Int,
    getName: (T) -> String
): String? {
    if (id == null) return null
    return entities.find { getId(it) == id }?.let { getName(it) }
}

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val context = LocalContext.current
    
    // Collect knowledge base entities directly using collectAsState
    // Room flows automatically emit new values when database changes
    val categories by categoriesFlow.collectAsState(initial = emptyList())
    val formas by formasFlow.collectAsState(initial = emptyList())
    val causes by causesFlow.collectAsState(initial = emptyList())
    val develops by developsFlow.collectAsState(initial = emptyList())
    val effects by effectsFlow.collectAsState(initial = emptyList())
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
                title = { Text("${EmojiMapping.getEmoji("screens", "newEvent")} ${context.getString(R.string.edit_event)}") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.cancel))
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
                        Text("${EmojiMapping.getEmoji("actions", "save")} ${context.getString(R.string.save)}")
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
                label = { Text("${EmojiMapping.getEmoji("fields", "name")} ${context.getString(R.string.name)}") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                readOnly = true,
                singleLine = true
            )
            
            // Description field (read-only)
            OutlinedTextField(
                value = initialDescription,
                onValueChange = { },
                label = { Text("${EmojiMapping.getEmoji("fields", "description")} ${context.getString(R.string.description)}") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                readOnly = true,
                minLines = 3,
                maxLines = 6
            )

            HorizontalDivider()

            // Context Fields (read-only, display names instead of IDs)
            // Use the same approach as EventDetailScreen: AssistChip in FlowRow
            // Get entity names by ID, fallback to ID if name not found
            // Always show field if ID exists, even if entity not found in list yet
            // CRITICAL: Use remember with keys to force recomputation when lists change
            // This ensures context fields update immediately after knowledge base re-seeding
            val categoryDisplay = remember(event.categoryId, categories.size, categories) {
                event.categoryId?.let { id ->
                    val name = getEntityNameById(id, categories, { it.id }, { it.name })
                    if (name != null) name else id.toString()
                }
            }
            val formaDisplay = remember(event.formaId, formas.size, formas) {
                event.formaId?.let { id ->
                    val name = getEntityNameById(id, formas, { it.id }, { it.name })
                    if (name != null) name else id.toString()
                }
            }
            val causeDisplay = remember(event.causeId, causes.size, causes) {
                event.causeId?.let { id ->
                    val name = getEntityNameById(id, causes, { it.id }, { it.name })
                    if (name != null) name else id.toString()
                }
            }
            val developDisplay = remember(event.developId, develops.size, develops) {
                event.developId?.let { id ->
                    val name = getEntityNameById(id, develops, { it.id }, { it.name })
                    if (name != null) name else id.toString()
                }
            }
            val effectDisplay = remember(event.effectId, effects.size, effects) {
                event.effectId?.let { id ->
                    val name = getEntityNameById(id, effects, { it.id }, { it.name })
                    if (name != null) name else id.toString()
                }
            }
            
            val contextFields = listOfNotNull(
                categoryDisplay?.let { "${context.getString(R.string.category)}: $it" },
                formaDisplay?.let { "${context.getString(R.string.forma)}: $it" },
                causeDisplay?.let { "${context.getString(R.string.cause)}: $it" },
                developDisplay?.let { "${context.getString(R.string.develop)}: $it" },
                effectDisplay?.let { "${context.getString(R.string.effect)}: $it" }
            )
            if (contextFields.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.context_fields),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contextFields.forEach { field ->
                        AssistChip(onClick = {}, label = { Text(field) })
                    }
                }
            }

            HorizontalDivider()

            // Timestamp fields (editable only if not already filled)
            Text(
                text = context.getString(R.string.timeline),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            DatePickerField(
                label = "${EmojiMapping.getEmoji("fields", "startDate")} ${context.getString(R.string.start_timestamp)}",
                selectedDate = timestampStart,
                onDateSelected = { },
                modifier = Modifier.fillMaxWidth(),
                enabled = false // Start Timestamp is not available for editing
            )

            DatePickerField(
                label = "${EmojiMapping.getEmoji("fields", "endDate")} ${context.getString(R.string.end_timestamp)} *",
                selectedDate = timestampEnd,
                onDateSelected = { newDate ->
                    // Validation: End Timestamp cannot be less than Start Timestamp, but can be equal
                    if (timestampStart != null) {
                        val normalizedStart = normalizeToStartOfDay(timestampStart)
                        val normalizedEnd = normalizeToStartOfDay(newDate)
                        if (normalizedEnd < normalizedStart) {
                            timestampEndError = context.getString(R.string.end_timestamp_cannot_be_less_than_start)
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
                text = context.getString(R.string.direction),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = event.vector,
                    onClick = { },
                    enabled = false,
                    label = { Text(if (event.vector) context.getString(R.string.outgoing) else context.getString(R.string.incoming)) }
                )
            }

            // Flags
            Text(
                text = context.getString(R.string.flags),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = detected,
                    onClick = { detected = !detected },
                    label = { Text(context.getString(R.string.detected)) }
                )
                FilterChip(
                    selected = corrected,
                    onClick = { }, // Corrected is not available for editing
                    enabled = false, // Corrected is set automatically
                    label = { Text(context.getString(R.string.corrected)) }
                )
            }
        }
    }
}
