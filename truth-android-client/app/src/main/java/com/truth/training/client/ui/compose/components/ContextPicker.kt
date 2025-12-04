package com.truth.training.client.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * ContextPicker - Dropdown component for selecting context IDs with human-readable labels.
 * 
 * Matches Desktop ContextPicker behavior:
 * - Displays human-readable labels instead of numeric IDs
 * - Validates selected IDs against lookup data
 * - Shows error state for invalid selections
 * - Supports manual entry with validation
 * 
 * @param label Label text for the picker
 * @param selectedId Currently selected context ID (nullable)
 * @param onSelectionChange Callback when selection changes
 * @param contextsFlow Flow of available contexts from repository
 * @param modifier Modifier for the component
 * @param enabled Whether the picker is enabled
 * @param isError Whether to show error state
 * @param errorMessage Error message to display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextPicker(
    label: String,
    selectedId: Int?,
    onSelectionChange: (Int?) -> Unit,
    contextsFlow: Flow<List<ContextTemplateEntity>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val contexts by contextsFlow.collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    
    // Find selected context for display
    val selectedContext = contexts.find { it.id == selectedId }
    val displayText = selectedContext?.name ?: (selectedId?.toString() ?: "")
    
    // Filter contexts based on search text
    val filteredContexts = if (searchText.isBlank()) {
        contexts
    } else {
        contexts.filter {
            it.name.contains(searchText, ignoreCase = true) ||
            it.id.toString().contains(searchText, ignoreCase = true)
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
                        // Validate against available contexts
                        val isValid = contexts.any { ctx -> ctx.id == manualId }
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
                if (filteredContexts.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No contexts available") },
                        onClick = { expanded = false }
                    )
                } else {
                    filteredContexts.forEach { context ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(
                                        text = context.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (context.description != null) {
                                        Text(
                                            text = context.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSelectionChange(context.id)
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

