package com.truth.training.client.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmName

/**
 * ContextPicker - Dropdown component for selecting context IDs with human-readable labels.
 * 
 * Matches Desktop ContextPicker behavior:
 * - Displays human-readable labels instead of numeric IDs
 * - Validates selected IDs against lookup data
 * - Shows error state for invalid selections
 * - Supports knowledge base entities (category, forma, cause, develop, effect)
 */

/**
 * ContextPicker for CategoryEntity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@JvmName("ContextPickerCategory")
fun ContextPicker(
    label: String,
    selectedId: Int?,
    onSelectionChange: (Int?) -> Unit,
    entitiesFlow: Flow<List<CategoryEntity>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    ContextPickerInternal(
        label, selectedId, onSelectionChange, entitiesFlow, modifier, enabled, isError, errorMessage,
        getId = { it.id },
        getName = { it.name },
        getDescription = { it.description }
    )
}

/**
 * ContextPicker for FormaEntity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@JvmName("ContextPickerForma")
fun ContextPicker(
    label: String,
    selectedId: Int?,
    onSelectionChange: (Int?) -> Unit,
    entitiesFlow: Flow<List<FormaEntity>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    ContextPickerInternal(
        label, selectedId, onSelectionChange, entitiesFlow, modifier, enabled, isError, errorMessage,
        getId = { it.id },
        getName = { it.name },
        getDescription = { it.description }
    )
}

/**
 * ContextPicker for CauseEntity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@JvmName("ContextPickerCause")
fun ContextPicker(
    label: String,
    selectedId: Int?,
    onSelectionChange: (Int?) -> Unit,
    entitiesFlow: Flow<List<CauseEntity>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    ContextPickerInternal(
        label, selectedId, onSelectionChange, entitiesFlow, modifier, enabled, isError, errorMessage,
        getId = { it.id },
        getName = { it.name },
        getDescription = { it.description }
    )
}

/**
 * ContextPicker for DevelopEntity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@JvmName("ContextPickerDevelop")
fun ContextPicker(
    label: String,
    selectedId: Int?,
    onSelectionChange: (Int?) -> Unit,
    entitiesFlow: Flow<List<DevelopEntity>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    ContextPickerInternal(
        label, selectedId, onSelectionChange, entitiesFlow, modifier, enabled, isError, errorMessage,
        getId = { it.id },
        getName = { it.name },
        getDescription = { it.description }
    )
}

/**
 * ContextPicker for EffectEntity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@JvmName("ContextPickerEffect")
fun ContextPicker(
    label: String,
    selectedId: Int?,
    onSelectionChange: (Int?) -> Unit,
    entitiesFlow: Flow<List<EffectEntity>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    ContextPickerInternal(
        label, selectedId, onSelectionChange, entitiesFlow, modifier, enabled, isError, errorMessage,
        getId = { it.id },
        getName = { it.name },
        getDescription = { it.description }
    )
}

/**
 * Internal implementation of ContextPicker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ContextPickerInternal(
    label: String,
    selectedId: Int?,
    onSelectionChange: (Int?) -> Unit,
    entitiesFlow: Flow<List<T>>,
    modifier: Modifier,
    enabled: Boolean,
    isError: Boolean,
    errorMessage: String?,
    getId: (T) -> Int,
    getName: (T) -> String,
    getDescription: (T) -> String?
) {
    val entities by entitiesFlow.collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    
    // Find selected entity for display
    val selectedEntity = entities.find { getId(it) == selectedId }
    val displayText = selectedEntity?.let { getName(it) } ?: (selectedId?.toString() ?: "")
    
    // Filter entities based on search text
    val filteredEntities = if (searchText.isBlank()) {
        entities
    } else {
        entities.filter {
            getName(it).contains(searchText, ignoreCase = true) ||
            getId(it).toString().contains(searchText, ignoreCase = true)
        }
    }
    
    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = { 
                    searchText = it
                    // Allow manual entry - validate on blur/submit
                    val manualId = it.toIntOrNull()
                    if (manualId != null) {
                        // Validate against available entities
                        val isValid = entities.any { entity -> getId(entity) == manualId }
                        if (isValid) {
                            onSelectionChange(manualId)
                        }
                    }
                },
                readOnly = false, // Allow manual entry
                label = { Text(label) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = enabled,
                isError = isError,
                supportingText = if (isError && errorMessage != null) {
                    { Text(errorMessage) }
                } else null,
                singleLine = true
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (filteredEntities.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No options available") },
                        onClick = { expanded = false }
                    )
                } else {
                    filteredEntities.forEach { entity ->
                        val entityId = getId(entity)
                        val entityName = getName(entity)
                        val description = getDescription(entity)
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(
                                        text = entityName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (description != null) {
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSelectionChange(entityId)
                                expanded = false
                                searchText = ""
                            }
                        )
                    }
                }
            }
        }
    }
}
