package com.truth.training.client.ui.compose.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truth.training.client.ui.summary.OverallSummaryViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Overall Summary Screen - Displays aggregated statistics across all events.
 * Matches Desktop UI "Overall Summary" screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverallSummaryScreen(
    viewModel: OverallSummaryViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.aggregatedMetrics.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overall Summary") },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isLoading
                    ) {
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
            
            // Metrics Display
            Text(
                text = "Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            MetricsCard(
                totalEvents = metrics.totalEvents,
                detectedEvents = metrics.detectedEvents,
                eventsWithConsensus = metrics.eventsWithConsensus,
                averageScore = metrics.averageCollectiveScore,
                lastUpdated = metrics.lastUpdated
            )
            
            // Network Stats (from API)
            stats?.let {
                HorizontalDivider()
                
                Text(
                    text = "Network Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                NetworkStatsCard(
                    peers = it.peers,
                    edges = it.edges,
                    avgTrust = it.avgTrust,
                    updatedAt = it.updatedAt
                )
            }
            
            // Event Rows Table (simplified - showing summary)
            HorizontalDivider()
            
            Text(
                text = "Event Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Total events: ${metrics.totalEvents}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Text(
                text = "Detected events: ${metrics.detectedEvents}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Text(
                text = "Events with consensus: ${metrics.eventsWithConsensus}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricsCard(
    totalEvents: Int,
    detectedEvents: Int,
    eventsWithConsensus: Int,
    averageScore: Float?,
    lastUpdated: Long
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
            MetricRow("Total Events", totalEvents.toString())
            MetricRow("Detected Events", detectedEvents.toString())
            MetricRow("Events with Consensus", eventsWithConsensus.toString())
            
            averageScore?.let {
                MetricRow("Average Collective Score", String.format("%.2f", it))
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Last updated: ${formatTimestamp(lastUpdated)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NetworkStatsCard(
    peers: Int?,
    edges: Int?,
    avgTrust: Double?,
    updatedAt: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            peers?.let { MetricRow("Peers", it.toString()) }
            edges?.let { MetricRow("Edges", it.toString()) }
            avgTrust?.let { MetricRow("Average Trust", String.format("%.2f", it)) }
            
            updatedAt?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Updated: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}


