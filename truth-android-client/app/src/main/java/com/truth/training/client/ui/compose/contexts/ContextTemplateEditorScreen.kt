package com.truth.training.client.ui.compose.contexts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.data.network.dto.CreateContextRequest
import com.truth.training.client.data.database.entities.*
import com.truth.training.client.ui.compose.components.ContextPicker
import com.truth.training.client.utils.EmojiMapping
import kotlinx.coroutines.flow.Flow

/**
 * Context Template Editor Screen (Compose) - Form for creating templates only.
 * Editing is not allowed, only adding new templates.
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
    val context = LocalContext.current
    
    // Collect knowledge base entities directly using collectAsState
    // Room flows automatically emit new values when database changes
    val categories by categoriesFlow.collectAsState(initial = emptyList())
    val formas by formasFlow.collectAsState(initial = emptyList())
    val causes by causesFlow.collectAsState(initial = emptyList())
    val develops by developsFlow.collectAsState(initial = emptyList())
    val effects by effectsFlow.collectAsState(initial = emptyList())
    
    // Check if knowledge base data is available
    val dataAvailable = categories.isNotEmpty() || formas.isNotEmpty() || 
                        causes.isNotEmpty() || develops.isNotEmpty() || effects.isNotEmpty()
    // Only allow creating new templates, not editing existing ones
    // If templateId is provided, it means we're trying to edit, which is not allowed
    if (templateId != null) {
        // Redirect to create new template instead
        LaunchedEffect(templateId) {
            onCancel()
        }
        return
    }
    
    var name by remember { mutableStateOf(initialName) }
    var categoryId by remember { mutableStateOf<Int?>(initialCategoryId) }
    var formaId by remember { mutableStateOf<Int?>(initialFormaId) }
    var causeId by remember { mutableStateOf<Int?>(initialCauseId) }
    var developId by remember { mutableStateOf<Int?>(initialDevelopId) }
    var effectId by remember { mutableStateOf<Int?>(initialEffectId) }
    var description by remember { mutableStateOf(initialDescription) }
    var duplicateError by remember { mutableStateOf<String?>(null) }
    
    // Validation errors for context fields
    var categoryError by remember { mutableStateOf<String?>(null) }
    var formaError by remember { mutableStateOf<String?>(null) }
    var causeError by remember { mutableStateOf<String?>(null) }
    var developError by remember { mutableStateOf<String?>(null) }
    var effectError by remember { mutableStateOf<String?>(null) }
    
    // Validate that all context fields are filled (none can be NULL)
    val allContextFieldsFilled = categoryId != null && formaId != null && 
                                  causeId != null && developId != null && effectId != null
    
    val canSave = name.isNotEmpty() && 
                  allContextFieldsFilled &&
                  categoryError == null && formaError == null && 
                  causeError == null && developError == null && effectError == null
    
    // Update fields when initial values change (e.g., when template is selected)
    // This ensures that when a template is selected from the list, all fields are populated
    LaunchedEffect(initialName, initialCategoryId, initialFormaId, initialCauseId, initialDevelopId, initialEffectId, initialDescription) {
        if (initialName.isNotEmpty() || initialCategoryId != null || initialFormaId != null || 
            initialCauseId != null || initialDevelopId != null || initialEffectId != null || 
            initialDescription.isNotEmpty()) {
            // Only update if there are actual values (template was selected)
            name = initialName
            categoryId = initialCategoryId
            formaId = initialFormaId
            causeId = initialCauseId
            developId = initialDevelopId
            effectId = initialEffectId
            description = initialDescription
            // Clear any validation errors when template is selected
            categoryError = null
            formaError = null
            causeError = null
            developError = null
            effectError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${EmojiMapping.getEmoji("screens", "contextEditor")} ${context.getString(R.string.new_template)}") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(R.string.cancel)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Validate that all context fields are filled before submission
                            var hasValidationError = false
                            
                            if (categoryId == null) {
                                categoryError = context.getString(R.string.field_required)
                                hasValidationError = true
                            }
                            if (formaId == null) {
                                formaError = context.getString(R.string.field_required)
                                hasValidationError = true
                            }
                            if (causeId == null) {
                                causeError = context.getString(R.string.field_required)
                                hasValidationError = true
                            }
                            if (developId == null) {
                                developError = context.getString(R.string.field_required)
                                hasValidationError = true
                            }
                            if (effectId == null) {
                                effectError = context.getString(R.string.field_required)
                                hasValidationError = true
                            }
                            
                            if (hasValidationError) {
                                return@TextButton
                            }
                            
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
                                    duplicateError = context.getString(R.string.template_duplicate_error)
                                } else {
                                    duplicateError = e.message ?: context.getString(R.string.error_saving_template)
                                }
                            }
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
                label = { Text("${EmojiMapping.getEmoji("fields", "name")} ${context.getString(R.string.template_name)}") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isEmpty()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("${EmojiMapping.getEmoji("fields", "description")} ${context.getString(R.string.template_description)}") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            HorizontalDivider()

            // Context Fields - same as in EventCreateScreen
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
                            text = "${EmojiMapping.getEmoji("status", "warning")} ${context.getString(R.string.knowledge_base_unavailable)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = context.getString(R.string.knowledge_base_unavailable_message),
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
                    label = "${EmojiMapping.getEmoji("fields", "category")} ${context.getString(R.string.category)}",
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
                    label = "${EmojiMapping.getEmoji("fields", "forma")} ${context.getString(R.string.forma)}",
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
                    label = "${EmojiMapping.getEmoji("fields", "cause")} ${context.getString(R.string.cause)}",
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
                    label = "${EmojiMapping.getEmoji("fields", "develop")} ${context.getString(R.string.develop)}",
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
                label = "${EmojiMapping.getEmoji("fields", "effect")} ${context.getString(R.string.effect)}",
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
        }
    }
}

