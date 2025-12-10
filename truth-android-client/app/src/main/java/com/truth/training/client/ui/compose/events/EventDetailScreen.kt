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
import com.truth.training.client.utils.EmojiMapping
import kotlinx.coroutines.flow.Flow

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
    
    if (event == null) {
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
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = context.getString(R.string.edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = context.getString(R.string.delete))
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

            Button(onClick = onNavigateToJudgments, modifier = Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.view_judgments))
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.edit_event))
            }
        }
    }
}

