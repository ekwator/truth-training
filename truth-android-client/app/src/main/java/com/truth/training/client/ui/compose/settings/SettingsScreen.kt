package com.truth.training.client.ui.compose.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.SyncStatus
import com.truth.training.client.ui.settings.SettingsViewModel
import com.truth.training.client.R
import com.truth.training.client.utils.EmojiMapping
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity

/**
 * Settings Screen - Application configuration and connection settings.
 * Matches Desktop UI "Settings" screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionMode by viewModel.connectionMode.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val nearbySyncEnabled by viewModel.nearbySyncEnabled.collectAsState()
    val nearbySyncInterval by viewModel.nearbySyncInterval.collectAsState()
    val discoveryWorkerEnabled by viewModel.discoveryWorkerEnabled.collectAsState()
    val lanInterval by viewModel.lanInterval.collectAsState()
    val wifiInterval by viewModel.wifiInterval.collectAsState()
    val globalInterval by viewModel.globalInterval.collectAsState()
    val lanTtl by viewModel.lanTtl.collectAsState()
    val wifiTtl by viewModel.wifiTtl.collectAsState()
    val globalTtl by viewModel.globalTtl.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val connectionTestResult by viewModel.connectionTestResult.collectAsState()
    val connectionTestTimestamp by viewModel.connectionTestTimestamp.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentLocale by viewModel.currentLocale.collectAsState()
    val context = LocalContext.current
    
    var showClearEventsConfirmation by remember { mutableStateOf(false) }
    var showLanguageChangeConfirmation by remember { mutableStateOf(false) }
    var pendingLocale by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${EmojiMapping.getEmoji("screens", "settings")} ${context.getString(R.string.settings)}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "${EmojiMapping.getEmoji("actions", "back")} ${context.getString(R.string.back)}"
                        )
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
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${EmojiMapping.getEmoji("status", "error")} ${context.getString(R.string.error_prefix, error)}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Language Selection
            Text(
                text = context.getString(R.string.language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(context.getString(R.string.language))
                    Row {
                        FilterChip(
                            selected = currentLocale == "en",
                            onClick = { 
                                if (currentLocale != "en") {
                                    pendingLocale = "en"
                                    showLanguageChangeConfirmation = true
                                }
                            },
                            label = { Text(context.getString(R.string.english)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = currentLocale == "ru",
                            onClick = { 
                                if (currentLocale != "ru") {
                                    pendingLocale = "ru"
                                    showLanguageChangeConfirmation = true
                                }
                            },
                            label = { Text(context.getString(R.string.russian)) }
                        )
                    }
                }
            }
            
            HorizontalDivider()
            
            // Connection Mode Toggle
            Text(
                text = context.getString(R.string.connection_mode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(context.getString(R.string.mode))
                    Row {
                        FilterChip(
                            selected = connectionMode == "core",
                            onClick = { viewModel.setConnectionMode("core") },
                            label = { Text(context.getString(R.string.core_local)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = connectionMode == "http",
                            onClick = { viewModel.setConnectionMode("http") },
                            label = { Text(context.getString(R.string.http_api)) }
                        )
                    }
                }
            }
            
            // Server Configuration
            if (connectionMode == "http") {
                HorizontalDivider()
                
                Text(
                    text = context.getString(R.string.server_configuration),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                var ipText by remember { mutableStateOf(serverIp) }
                var portText by remember { mutableStateOf(serverPort.toString()) }
                
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { 
                        ipText = it
                        if (isValidIpAddress(it)) {
                            viewModel.setServerIp(it)
                        }
                    },
                    label = { Text(context.getString(R.string.ip_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = ipText.isNotBlank() && !isValidIpAddress(ipText),
                    supportingText = {
                        if (ipText.isNotBlank() && !isValidIpAddress(ipText)) {
                            Text(context.getString(R.string.invalid_ip_format))
                        }
                    }
                )
                
                OutlinedTextField(
                    value = portText,
                    onValueChange = { 
                        portText = it
                        val port = it.toIntOrNull()
                        if (port != null && port in 1..65535) {
                            viewModel.setServerPort(port)
                        }
                    },
                    label = { Text(context.getString(R.string.port)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = portText.isNotBlank() && (portText.toIntOrNull() == null || portText.toIntOrNull() !in 1..65535),
                    supportingText = {
                        if (portText.isNotBlank() && (portText.toIntOrNull() == null || portText.toIntOrNull() !in 1..65535)) {
                            Text(context.getString(R.string.port_range_error))
                        }
                    }
                )
            }
            
            // Nearby Sync Toggle
            HorizontalDivider()
            
            Text(
                text = context.getString(R.string.nearby_sync),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(context.getString(R.string.enable_udp_broadcast))
                        Switch(
                            checked = nearbySyncEnabled,
                            onCheckedChange = { viewModel.setNearbySyncEnabled(it) }
                        )
                    }
                    
                    if (nearbySyncEnabled) {
                        var intervalText by remember { mutableStateOf(nearbySyncInterval.toString()) }
                        
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { 
                                intervalText = it
                                val interval = it.toLongOrNull()
                                if (interval != null && interval in 500..60000) {
                                    viewModel.setNearbySyncInterval(interval)
                                }
                            },
                            label = { Text(context.getString(R.string.interval_ms)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = intervalText.isNotBlank() && run {
                                val interval = intervalText.toLongOrNull()
                                interval == null || interval !in 500..60000
                            },
                            supportingText = {
                                val interval = intervalText.toLongOrNull()
                                if (intervalText.isNotBlank() && (interval == null || interval !in 500..60000)) {
                                    Text(context.getString(R.string.interval_range_error))
                                }
                            }
                        )
                    }
                }
            }
            
            // Discovery Worker Settings
            HorizontalDivider()
            
            Text(
                text = context.getString(R.string.discovery_worker_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(context.getString(R.string.enable_background_discovery))
                        Switch(
                            checked = discoveryWorkerEnabled,
                            onCheckedChange = { viewModel.setDiscoveryWorkerEnabled(it) }
                        )
                    }
                    
                    if (discoveryWorkerEnabled) {
                        SettingNumberField(
                            label = context.getString(R.string.lan_interval_ms),
                            value = lanInterval,
                            onValueChange = { viewModel.setLanInterval(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = context.getString(R.string.wifi_interval_ms),
                            value = wifiInterval,
                            onValueChange = { viewModel.setWifiInterval(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = context.getString(R.string.global_interval_ms),
                            value = globalInterval,
                            onValueChange = { viewModel.setGlobalInterval(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = context.getString(R.string.lan_ttl_seconds),
                            value = lanTtl,
                            onValueChange = { viewModel.setLanTtl(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = context.getString(R.string.wifi_ttl_seconds),
                            value = wifiTtl,
                            onValueChange = { viewModel.setWifiTtl(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = context.getString(R.string.global_ttl_seconds),
                            value = globalTtl,
                            onValueChange = { viewModel.setGlobalTtl(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Connection Status Panel
            HorizontalDivider()
            
            Text(
                text = context.getString(R.string.connection_status),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            ConnectionStatusCard(
                syncStatus = syncStatus,
                testResult = connectionTestResult,
                testTimestamp = connectionTestTimestamp,
                onTestConnection = { viewModel.testConnection() },
                isTesting = isTestingConnection,
                context = context
            )
            
            // Action Buttons
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.saveConnectionSettings() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${EmojiMapping.getEmoji("actions", "save")} ${context.getString(R.string.save_connection_settings)}")
                }
                
                Button(
                    onClick = { viewModel.saveDiscoveryWorkerSettings() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${EmojiMapping.getEmoji("actions", "save")} ${context.getString(R.string.save_discovery_settings)}")
                }
            }
            
            OutlinedButton(
                onClick = { showClearEventsConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(context.getString(R.string.clear_events))
            }
            
            // Language Change Confirmation Dialog
            if (showLanguageChangeConfirmation && pendingLocale != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showLanguageChangeConfirmation = false
                        pendingLocale = null
                    },
                    title = { Text(context.getString(R.string.change_language)) },
                    text = { 
                        Text(context.getString(R.string.change_language_message)) 
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val locale = pendingLocale ?: return@TextButton
                                showLanguageChangeConfirmation = false
                                pendingLocale = null
                                viewModel.changeLanguage(locale) {
                                    // Restart activity to apply new locale
                                    if (context is Activity) {
                                        context.recreate()
                                    }
                                }
                            }
                        ) {
                            Text(context.getString(R.string.yes))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showLanguageChangeConfirmation = false
                                pendingLocale = null
                            }
                        ) {
                            Text(context.getString(R.string.no))
                        }
                    }
                )
            }
            
            // Clear Events Confirmation Dialog
            if (showClearEventsConfirmation) {
                AlertDialog(
                    onDismissRequest = { showClearEventsConfirmation = false },
                    title = { Text(context.getString(R.string.clear_events_title)) },
                    text = { Text(context.getString(R.string.clear_events_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showClearEventsConfirmation = false
                                viewModel.clearEvents { }
                            }
                        ) {
                            Text(context.getString(R.string.yes))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearEventsConfirmation = false }
                        ) {
                            Text(context.getString(R.string.no))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingNumberField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    
    // Sync with external value changes
    LaunchedEffect(value) {
        text = value.toString()
    }
    
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            val num = newText.toLongOrNull()
            if (num != null && num > 0) {
                onValueChange(num)
            }
        },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun ConnectionStatusCard(
    syncStatus: SyncStatus,
    testResult: String?,
    testTimestamp: Long,
    onTestConnection: () -> Unit,
    isTesting: Boolean,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (syncStatus.isOnline) 
                        "${EmojiMapping.getEmoji("status", "online")} ${context.getString(R.string.online)}" 
                    else 
                        "${EmojiMapping.getEmoji("status", "offline")} ${context.getString(R.string.offline)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (syncStatus.isOnline) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (syncStatus.isOnline) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            
            Text(
                text = if (syncStatus.pendingOperations > 0)
                    "${EmojiMapping.getEmoji("status", "syncing")} ${context.getString(R.string.pending_operations, syncStatus.pendingOperations)}"
                else
                    context.getString(R.string.pending_operations, syncStatus.pendingOperations),
                style = MaterialTheme.typography.bodyMedium
            )
            
            syncStatus.lastSyncTimestamp?.let {
                Text(
                    text = context.getString(R.string.last_sync_time, formatTimestamp(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            testResult?.let {
                val emoji = if (it.contains("Success", ignoreCase = true)) {
                    EmojiMapping.getEmoji("status", "success")
                } else if (it.contains("Failed", ignoreCase = true)) {
                    EmojiMapping.getEmoji("status", "error")
                } else {
                    ""
                }
                Text(
                    text = if (emoji.isNotEmpty()) "$emoji ${context.getString(R.string.test_result, it)}" else context.getString(R.string.test_result, it),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (testTimestamp > 0) {
                    Text(
                        text = context.getString(R.string.test_time, formatTimestamp(testTimestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            OutlinedButton(
                onClick = onTestConnection,
                enabled = !isTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("${EmojiMapping.getEmoji("actions", "refresh")} ${context.getString(R.string.test_connection)}")
                }
            }
        }
    }
}

private fun isValidIpAddress(ip: String): Boolean {
    val pattern = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
    if (!pattern.matches(ip)) return false
    
    return ip.split(".").all { segment ->
        val num = segment.toIntOrNull()
        num != null && num in 0..255
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

