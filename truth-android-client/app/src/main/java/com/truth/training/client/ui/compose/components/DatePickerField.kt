package com.truth.training.client.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date picker field component for Jetpack Compose.
 * Displays a date in dd.MM.yyyy format and opens a date picker dialog on click.
 * 
 * @param allowClear If true, shows a clear button to set the date to null (for End Timestamp in New Event)
 * @param onDateCleared Callback when date is cleared (only used if allowClear is true)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    allowClear: Boolean = false,
    onDateCleared: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    
    val dateString = remember(selectedDate) {
        selectedDate?.let { dateFormat.format(Date(it)) } ?: ""
    }
    
    OutlinedTextField(
        value = dateString,
        onValueChange = { }, // Read-only, opens picker on click
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDatePicker = true },
        enabled = false, // Make it read-only, clickable via modifier
        readOnly = true,
        isError = isError,
        supportingText = if (errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        trailingIcon = {
            Row {
                if (allowClear && selectedDate != null && enabled) {
                    IconButton(onClick = { onDateCleared?.invoke() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear date"
                        )
                    }
                }
                IconButton(onClick = { if (enabled) showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select date"
                    )
                }
            }
        }
    )
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate ?: System.currentTimeMillis()
        )
        
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                DatePicker(state = datePickerState)
            }
        )
    }
}

