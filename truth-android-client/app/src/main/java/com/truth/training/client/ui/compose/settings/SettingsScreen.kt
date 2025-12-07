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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truth.training.client.data.SyncStatus
import com.truth.training.client.ui.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

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
    
    var showInitConfirmation by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        text = "Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Connection Mode Toggle
            Text(
                text = "Connection Mode",
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
                    Text("Mode")
                    Row {
                        FilterChip(
                            selected = connectionMode == "core",
                            onClick = { viewModel.setConnectionMode("core") },
                            label = { Text("Core (Local)") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = connectionMode == "http",
                            onClick = { viewModel.setConnectionMode("http") },
                            label = { Text("HTTP API") }
                        )
                    }
                }
            }
            
            // Server Configuration
            if (connectionMode == "http") {
                HorizontalDivider()
                
                Text(
                    text = "Server Configuration",
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
                    label = { Text("IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = ipText.isNotBlank() && !isValidIpAddress(ipText),
                    supportingText = {
                        if (ipText.isNotBlank() && !isValidIpAddress(ipText)) {
                            Text("Invalid IP address format")
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
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = portText.isNotBlank() && (portText.toIntOrNull() == null || portText.toIntOrNull() !in 1..65535),
                    supportingText = {
                        if (portText.isNotBlank() && (portText.toIntOrNull() == null || portText.toIntOrNull() !in 1..65535)) {
                            Text("Port must be between 1 and 65535")
                        }
                    }
                )
            }
            
            // Nearby Sync Toggle
            HorizontalDivider()
            
            Text(
                text = "Nearby Sync",
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
                        Text("Enable UDP Broadcast Discovery")
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
                            label = { Text("Interval (ms)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = intervalText.isNotBlank() && run {
                                val interval = intervalText.toLongOrNull()
                                interval == null || interval !in 500..60000
                            },
                            supportingText = {
                                val interval = intervalText.toLongOrNull()
                                if (intervalText.isNotBlank() && (interval == null || interval !in 500..60000)) {
                                    Text("Interval must be between 500 and 60000 ms")
                                }
                            }
                        )
                    }
                }
            }
            
            // Discovery Worker Settings
            HorizontalDivider()
            
            Text(
                text = "Discovery Worker Settings",
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
                        Text("Enable Background Discovery")
                        Switch(
                            checked = discoveryWorkerEnabled,
                            onCheckedChange = { viewModel.setDiscoveryWorkerEnabled(it) }
                        )
                    }
                    
                    if (discoveryWorkerEnabled) {
                        SettingNumberField(
                            label = "LAN Interval (ms)",
                            value = lanInterval,
                            onValueChange = { viewModel.setLanInterval(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = "Wi-Fi Interval (ms)",
                            value = wifiInterval,
                            onValueChange = { viewModel.setWifiInterval(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = "Global Interval (ms)",
                            value = globalInterval,
                            onValueChange = { viewModel.setGlobalInterval(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = "LAN TTL (seconds)",
                            value = lanTtl,
                            onValueChange = { viewModel.setLanTtl(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = "Wi-Fi TTL (seconds)",
                            value = wifiTtl,
                            onValueChange = { viewModel.setWifiTtl(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        SettingNumberField(
                            label = "Global TTL (seconds)",
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
                text = "Connection Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            ConnectionStatusCard(
                syncStatus = syncStatus,
                testResult = connectionTestResult,
                testTimestamp = connectionTestTimestamp,
                onTestConnection = { viewModel.testConnection() },
                isTesting = isTestingConnection
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
                    Text("Save Connection Settings")
                }
                
                Button(
                    onClick = { viewModel.saveDiscoveryWorkerSettings() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Discovery Settings")
                }
            }
            
            OutlinedButton(
                onClick = { showInitConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Initialize App (Reset Configuration)")
            }
            
            // Init Confirmation Dialog
            if (showInitConfirmation) {
                AlertDialog(
                    onDismissRequest = { showInitConfirmation = false },
                    title = { Text("Initialize App") },
                    text = { Text("This will reset all configuration and rebuild the database. Are you sure?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showInitConfirmation = false
                                viewModel.initializeApp { }
                            }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showInitConfirmation = false }
                        ) {
                            Text("No")
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
    isTesting: Boolean
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
                    text = if (syncStatus.isOnline) "Online" else "Offline",
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
                text = "Pending operations: ${syncStatus.pendingOperations}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            syncStatus.lastSyncTimestamp?.let {
                Text(
                    text = "Last sync: ${formatTimestamp(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            testResult?.let {
                Text(
                    text = "Test result: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (testTimestamp > 0) {
                    Text(
                        text = "Test time: ${formatTimestamp(testTimestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Button(
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
                    Text("Test Connection")
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

