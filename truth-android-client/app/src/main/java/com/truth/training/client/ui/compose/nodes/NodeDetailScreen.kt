package com.truth.training.client.ui.compose.nodes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.data.database.entities.NodeEntity
import com.truth.training.client.ui.nodes.NodeDetailViewModel
import com.truth.training.client.utils.EmojiMapping
import com.truth.training.client.utils.NodeTypeMapper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Node Detail Screen - Displays detailed information about a network node.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    viewModel: NodeDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val node by viewModel.node.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${EmojiMapping.getEmoji("navigation", "events")} ${node?.address ?: context.getString(R.string.node_detail_title)}",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "${EmojiMapping.getEmoji("actions", "back")} ${context.getString(R.string.back)}"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "${EmojiMapping.getEmoji("actions", "refresh")} ${context.getString(R.string.node_refresh)}"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${EmojiMapping.getEmoji("status", "error")} ${error}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            node == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${EmojiMapping.getEmoji("status", "error")} ${context.getString(R.string.node_na)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                NodeDetailContent(
                    node = node!!,
                    modifier = modifier
                        .padding(padding)
                        .fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NodeDetailContent(
    node: NodeEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis() / 1000
    val ageSeconds = now - node.lastSeen
    val expiresInSeconds = node.ttl - ageSeconds
    val expiresIn = expiresInSeconds.coerceAtLeast(0)
    
    val (userFriendlyType, technicalType) = NodeTypeMapper.getBothTypes(node.type)
    
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Address
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "name")} ${context.getString(R.string.node_address)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = node.address,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        // Type (Hub/Leaf and Technical)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "category")} ${context.getString(R.string.node_type)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (userFriendlyType == "Hub") context.getString(R.string.node_type_hub)
                                else if (userFriendlyType == "Leaf") context.getString(R.string.node_type_leaf)
                                else context.getString(R.string.node_unknown_type)
                            )
                        }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(technicalType) }
                    )
                }
            }
        }
        
        // Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${EmojiMapping.getEmoji("status", if (node.reachable == 1) "online" else "offline")} ${context.getString(R.string.node_status)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = if (node.reachable == 1)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (node.reachable == 1)
                            context.getString(R.string.node_status_reachable)
                        else
                            context.getString(R.string.node_status_unreachable),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Timestamps and TTL
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "startDate")} ${context.getString(R.string.node_last_seen)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTimestamp(node.lastSeen),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "description")} ${context.getString(R.string.node_ttl)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDuration(node.ttl),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "${EmojiMapping.getEmoji("status", if (expiresIn > 0) "warning" else "error")} ${context.getString(R.string.node_expires_in)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (expiresIn > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (expiresIn > 0) formatDuration(expiresIn) else context.getString(R.string.node_expired),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (expiresIn > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "startDate")} ${context.getString(R.string.node_age)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDuration(ageSeconds),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Additional Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "description")} ${context.getString(R.string.node_source)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = node.source ?: context.getString(R.string.node_na),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "name")} ${context.getString(R.string.node_id)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = node.nodeId ?: context.getString(R.string.node_na),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "startDate")} ${context.getString(R.string.node_created_at)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTimestamp(node.createdAt),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "${EmojiMapping.getEmoji("fields", "startDate")} ${context.getString(R.string.node_updated_at)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTimestamp(node.updatedAt),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Format Unix timestamp (seconds) to human-readable string.
 */
private fun formatTimestamp(timestampSeconds: Long): String {
    val date = Date(timestampSeconds * 1000)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(date)
}

/**
 * Format duration (seconds) to human-readable string (e.g., "2h 30m", "5d 3h").
 */
private fun formatDuration(seconds: Long): String {
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    if (hours < 24) {
        val remainingMinutes = minutes % 60
        return if (remainingMinutes > 0) "${hours}h ${remainingMinutes}m" else "${hours}h"
    }
    val days = hours / 24
    val remainingHours = hours % 24
    return if (remainingHours > 0) "${days}d ${remainingHours}h" else "${days}d"
}


