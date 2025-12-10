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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.data.database.entities.*
import com.truth.training.client.ui.compose.impacts.AddImpactDialog
import com.truth.training.client.ui.compose.judgments.SubmitJudgmentDialog
import com.truth.training.client.ui.events.EventDetailViewModel
import com.truth.training.client.utils.EmojiMapping
import com.truth.training.client.utils.ImpactLevelMapper
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    event: EventEntity?,
    viewModel: EventDetailViewModel?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToJudgments: () -> Unit,
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
    
    // Collect impacts and judgments from ViewModel
    val impacts by viewModel?.impacts?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList<ImpactEntity>()) }
    val judgments by viewModel?.judgments?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList<JudgmentEntity>()) }
    
    // Dialog state
    var showAddImpactDialog by remember { mutableStateOf(false) }
    var showSubmitJudgmentDialog by remember { mutableStateOf(false) }
    
    if (event == null || viewModel == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${EmojiMapping.getEmoji("screens", "events")} ${context.getString(R.string.event_details)}") },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "${EmojiMapping.getEmoji("actions", "edit")} ${context.getString(R.string.edit)}")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "${EmojiMapping.getEmoji("actions", "delete")} ${context.getString(R.string.delete)}")
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
                label = { Text(if (event.vector) context.getString(R.string.outgoing) else context.getString(R.string.incoming)) }
            )

            // Get entity names by ID, fallback to ID if name not found
            // Always show field if ID exists, even if entity not found in list yet
            // CRITICAL: Use remember with keys to force recomputation when lists change
            // This ensures context fields update immediately after knowledge base re-seeding
            // The keys include list sizes to detect when data is cleared and re-populated
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

            Text(
                text = context.getString(R.string.timestamps),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${context.getString(R.string.start)}: ${event.timestampStart}",
                style = MaterialTheme.typography.bodyMedium
            )
            event.timestampEnd?.let {
                Text(
                    text = "${context.getString(R.string.end)}: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val stateChips = listOfNotNull(
                event.detected?.let { if (it) context.getString(R.string.detected) else context.getString(R.string.not_detected) },
                if (event.corrected) context.getString(R.string.corrected) else null
            )
            if (stateChips.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stateChips.forEach { label ->
                        AssistChip(onClick = {}, label = { Text(label) })
                    }
                }
            }

            Divider()

            // Impacts Section
            Text(
                text = "${EmojiMapping.getEmoji("screens", "events")} ${context.getString(R.string.impacts)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Button(
                onClick = { showAddImpactDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${EmojiMapping.getEmoji("actions", "create")} ${context.getString(R.string.add_impact)}")
            }
            
            if (impacts.isEmpty()) {
                Text(
                    text = "${EmojiMapping.getEmoji("status", "warning")} ${context.getString(R.string.no_impacts_yet)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                impacts.forEach { impact ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${EmojiMapping.getEmoji("fields", "assessment")} ${if (impact.value) "Positive (Level 4-5)" else "Negative (Level 1-3)"}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = formatTimestamp(impact.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            impact.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Divider()
            
            // Judgments Section
            Text(
                text = "${EmojiMapping.getEmoji("screens", "judgments")} ${context.getString(R.string.judgments)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Button(
                onClick = { showSubmitJudgmentDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${EmojiMapping.getEmoji("actions", "submit")} ${context.getString(R.string.submit_judgment)}")
            }
            
            if (judgments.isEmpty()) {
                Text(
                    text = "${EmojiMapping.getEmoji("status", "warning")} ${context.getString(R.string.no_judgments_yet)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                judgments.forEach { judgment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${EmojiMapping.getEmoji("fields", "assessment")} ${judgment.assessment}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = formatJudgmentTimestamp(judgment.submittedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${EmojiMapping.getEmoji("fields", "confidence")} ${context.getString(R.string.confidence_percent, (judgment.confidenceLevel * 100).toInt())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            judgment.reasoning?.takeIf { it.isNotBlank() }?.let { reasoning ->
                                Text(
                                    text = reasoning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Divider()

            Button(onClick = onNavigateToJudgments, modifier = Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.view_judgments))
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.edit_event))
            }
        }
    }
    
    // Add Impact Dialog
    if (showAddImpactDialog) {
        AddImpactDialog(
            eventId = event.id,
            onSave = { impactLevel, notes ->
                viewModel.addImpact(impactLevel, notes)
                showAddImpactDialog = false
            },
            onDismiss = { showAddImpactDialog = false }
        )
    }
    
    // Submit Judgment Dialog
    if (showSubmitJudgmentDialog) {
        SubmitJudgmentDialog(
            eventId = event.id,
            onSubmit = { assessment, confidenceLevel, reasoning ->
                viewModel.submitJudgment(assessment, confidenceLevel, reasoning)
                showSubmitJudgmentDialog = false
            },
            onDismiss = { showSubmitJudgmentDialog = false }
        )
    }
}

/**
 * Format timestamp (milliseconds) to human-readable string.
 */
private fun formatTimestamp(timestampMs: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestampMs))
}

/**
 * Format judgment timestamp (ISO string) to human-readable string.
 */
private fun formatJudgmentTimestamp(timestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(timestamp)
        val date = Date.from(instant)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateFormat.format(date)
    } catch (e: Exception) {
        timestamp // Return original if parsing fails
    }
}

