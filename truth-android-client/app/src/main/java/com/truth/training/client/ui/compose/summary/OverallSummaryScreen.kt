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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
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
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.overall_summary)) },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = context.getString(R.string.refresh)
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
                        text = context.getString(R.string.error_prefix, error),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Metrics Display
            Text(
                text = context.getString(R.string.metrics),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            MetricsCard(
                totalEvents = metrics.totalEvents,
                detectedEvents = metrics.detectedEvents,
                eventsWithConsensus = metrics.eventsWithConsensus,
                averageScore = metrics.averageCollectiveScore,
                lastUpdated = metrics.lastUpdated,
                context = context
            )
            
            // Network Stats (from API)
            stats?.let {
                HorizontalDivider()
                
                Text(
                    text = context.getString(R.string.network_statistics),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                NetworkStatsCard(
                    peers = it.peers,
                    edges = it.edges,
                    avgTrust = it.avgTrust,
                    updatedAt = it.updatedAt,
                    context = context
                )
            }
            
            // Event Rows Table (simplified - showing summary)
            HorizontalDivider()
            
            Text(
                text = context.getString(R.string.event_summary),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = context.getString(R.string.total_events_count, metrics.totalEvents),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Text(
                text = context.getString(R.string.detected_events_count, metrics.detectedEvents),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Text(
                text = context.getString(R.string.events_with_consensus_count, metrics.eventsWithConsensus),
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
    lastUpdated: Long,
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
            MetricRow(context.getString(R.string.total_events), totalEvents.toString(), context)
            MetricRow(context.getString(R.string.detected_events), detectedEvents.toString(), context)
            MetricRow(context.getString(R.string.events_with_consensus), eventsWithConsensus.toString(), context)
            
            averageScore?.let {
                MetricRow(context.getString(R.string.average_collective_score), String.format("%.2f", it), context)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = context.getString(R.string.last_updated, formatTimestamp(lastUpdated)),
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
    updatedAt: String?,
    context: android.content.Context
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
            peers?.let { MetricRow(context.getString(R.string.peers), it.toString(), context) }
            edges?.let { MetricRow(context.getString(R.string.edges), it.toString(), context) }
            avgTrust?.let { MetricRow(context.getString(R.string.average_trust), String.format("%.2f", it), context) }
            
            updatedAt?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = context.getString(R.string.updated, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, context: android.content.Context) {
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


