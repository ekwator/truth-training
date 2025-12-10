package com.truth.training.client.ui.compose.nodes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.data.database.entities.NodeEntity
import com.truth.training.client.utils.EmojiMapping
import java.text.SimpleDateFormat
import java.util.*

/**
 * Nodes Screen (Compose) - Displays discovered nodes with reachability status and TTL countdown.
 * 
 * Implements T046: Android UI for node discovery matching Desktop NodesPanel.
 * 
 * Features:
 * - Node list with filters (type, reachability)
 * - Manual refresh, discover, cleanup, health check actions
 * - TTL countdown display
 * - Reachability status badges
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodesScreen(
    viewModel: NodesViewModel,
    modifier: Modifier = Modifier
) {
    val nodes by viewModel.nodes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val nodeTypeFilter by viewModel.nodeTypeFilter.collectAsState()
    val reachableFilter by viewModel.reachableFilter.collectAsState()
    
    var showTypeFilterMenu by remember { mutableStateOf(false) }
    var showReachableFilterMenu by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val nodeTypes = listOf("ALL", "LAN", "WIFI", "GLOBAL", "RELAY", "CLIENT")
    val reachableOptions = listOf("All" to null, "Reachable" to 1, "Unreachable" to 0)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${EmojiMapping.getEmoji("navigation", "events")} ${context.getString(R.string.node_discovery)}") },
                actions = {
                    IconButton(onClick = { viewModel.refreshNodes() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
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
        ) {
            // Filters and Actions Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Type Filter
                        FilterChip(
                            selected = nodeTypeFilter != null,
                            onClick = { showTypeFilterMenu = true },
                            label = { Text(nodeTypeFilter ?: "All Types") }
                        )
                        
                        // Reachability Filter
                        FilterChip(
                            selected = reachableFilter != null,
                            onClick = { showReachableFilterMenu = true },
                            label = {
                                Text(
                                    when (reachableFilter) {
                                        1 -> "Reachable"
                                        0 -> "Unreachable"
                                        else -> "All"
                                    }
                                )
                            }
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Action Buttons
                        IconButton(
                            onClick = { viewModel.discoverNodes() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Discover"
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.cleanupNodes() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Cleanup"
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.runHealthCheck() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HealthAndSafety,
                                contentDescription = "Health Check"
                            )
                        }
                    }
                    
                    // Error Message
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Last Updated
                    if (lastUpdated != null) {
                        Text(
                            text = "Last updated: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUpdated!!))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Nodes List
            if (isLoading && nodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (nodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "No nodes discovered yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.discoverNodes() }) {
                            Text("Start Discovery")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nodes, key = { it.id }) { node ->
                        NodeCard(node = node, lastUpdated = lastUpdated)
                    }
                }
            }
        }
    }
    
    // Type Filter Dropdown
    if (showTypeFilterMenu) {
        DropdownMenu(
            expanded = showTypeFilterMenu,
            onDismissRequest = { showTypeFilterMenu = false }
        ) {
            nodeTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        viewModel.setNodeTypeFilter(if (type == "ALL") null else type)
                        showTypeFilterMenu = false
                    }
                )
            }
        }
    }
    
    // Reachability Filter Dropdown
    if (showReachableFilterMenu) {
        DropdownMenu(
            expanded = showReachableFilterMenu,
            onDismissRequest = { showReachableFilterMenu = false }
        ) {
            reachableOptions.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        viewModel.setReachableFilter(value)
                        showReachableFilterMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NodeCard(
    node: NodeEntity,
    lastUpdated: Long?,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis() / 1000
    val ageSeconds = lastUpdated?.let { (it / 1000) - node.lastSeen } ?: 0
    val expiresIn = (node.ttl - ageSeconds).coerceAtLeast(0)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Address and Type Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = node.address,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                
                AssistChip(
                    onClick = {},
                    label = { Text(node.type) }
                )
            }
            
            // Reachability and TTL Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reachability Badge
                Surface(
                    color = if (node.reachable == 1) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (node.reachable == 1) "Online" else "Offline",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (node.reachable == 1)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                // TTL and Expires In
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "TTL: ${node.ttl}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (expiresIn > 0) "Expires: ${formatDuration(expiresIn)}" else "Expired",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (expiresIn > 0) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Source and Last Seen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (node.source != null) {
                    Text(
                        text = "Source: ${node.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Last seen: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(node.lastSeen * 1000))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val secs = seconds.toInt()
    if (secs == 0) return "0s"
    if (secs < 60) return "${secs}s"
    val minutes = secs / 60
    val remainingSeconds = secs % 60
    if (minutes < 60) {
        return "${minutes}m ${remainingSeconds}s"
    }
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return "${hours}h ${remainingMinutes}m"
}

