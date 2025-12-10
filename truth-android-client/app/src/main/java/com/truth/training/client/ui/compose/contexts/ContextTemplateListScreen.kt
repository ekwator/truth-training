package com.truth.training.client.ui.compose.contexts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

/**
 * Context Template List Screen (Compose) - Displays list of context templates.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContextTemplateListScreen(
    templates: List<ContextTemplateEntity>,
    onTemplateClick: (Int) -> Unit,
    onNewTemplateClick: () -> Unit,
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${EmojiMapping.getEmoji("screens", "contextEditor")} ${context.getString(R.string.context_templates)}") },
                actions = {
                    IconButton(onClick = onNewTemplateClick) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = context.getString(R.string.new_template)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTemplateClick) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Add,
                    contentDescription = context.getString(R.string.new_template)
                )
            }
        }
    ) { padding ->
        if (templates.isEmpty()) {
            EmptyTemplatesView(
                onNewTemplateClick = onNewTemplateClick,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { onTemplateClick(template.id) },
                        categories = categories,
                        formas = formas,
                        causes = causes,
                        develops = develops,
                        effects = effects,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateCard(
    template: ContextTemplateEntity,
    onClick: () -> Unit,
    categories: List<CategoryEntity>,
    formas: List<FormaEntity>,
    causes: List<CauseEntity>,
    develops: List<DevelopEntity>,
    effects: List<EffectEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Get entity names by ID, fallback to ID if name not found
    // Always show field if ID exists, even if entity not found in list yet
    // CRITICAL: Use remember with keys to force recomputation when lists change
    // This ensures context fields update immediately after knowledge base re-seeding
    val categoryDisplay = remember(template.categoryId, categories.size, categories) {
        template.categoryId?.let { id ->
            val name = getEntityNameById(id, categories, { it.id }, { it.name })
            if (name != null) name else id.toString()
        }
    }
    val formaDisplay = remember(template.formaId, formas.size, formas) {
        template.formaId?.let { id ->
            val name = getEntityNameById(id, formas, { it.id }, { it.name })
            if (name != null) name else id.toString()
        }
    }
    val causeDisplay = remember(template.causeId, causes.size, causes) {
        template.causeId?.let { id ->
            val name = getEntityNameById(id, causes, { it.id }, { it.name })
            if (name != null) name else id.toString()
        }
    }
    val developDisplay = remember(template.developId, develops.size, develops) {
        template.developId?.let { id ->
            val name = getEntityNameById(id, develops, { it.id }, { it.name })
            if (name != null) name else id.toString()
        }
    }
    val effectDisplay = remember(template.effectId, effects.size, effects) {
        template.effectId?.let { id ->
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
    
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!template.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            
            if (contextFields.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contextFields.forEach { field ->
                        AssistChip(onClick = {}, label = { Text(field) })
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = context.getString(R.string.no_context_fields),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyTemplatesView(
    onNewTemplateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = context.getString(R.string.no_templates_yet),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNewTemplateClick) {
            Text("${EmojiMapping.getEmoji("actions", "create")} ${context.getString(R.string.create_first_template)}")
        }
    }
}

