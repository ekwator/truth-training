package com.truth.training.client.ui.compose.training

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
import com.truth.training.client.ui.training.TrainingResultsViewModel

/**
 * Training Results Screen - Displays training progress and results.
 * Matches Desktop UI "Training Results" screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingResultsScreen(
    viewModel: TrainingResultsViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.trainingSummary.collectAsState()
    val latestProgress by viewModel.latestProgress.collectAsState()
    val progressMetrics by viewModel.progressMetrics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Results") },
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
            
            // Progress Metrics
            Text(
                text = "Progress Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            ProgressMetricsCard(
                totalEvents = summary.totalEvents,
                totalPositiveImpact = summary.totalPositiveImpact,
                totalNegativeImpact = summary.totalNegativeImpact,
                averageScore = summary.averageScore,
                trend = summary.trend
            )
            
            // Impact Progress
            HorizontalDivider()
            
            Text(
                text = "Impact Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            val totalImpact = summary.totalPositiveImpact + summary.totalNegativeImpact
            val positivePercentage = if (totalImpact > 0) {
                (summary.totalPositiveImpact / totalImpact * 100).toInt()
            } else 0
            
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
                    Text(
                        text = "Positive Impact Progress",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { positivePercentage / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "$positivePercentage%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Results Table (simplified - showing latest progress metrics)
            if (progressMetrics.isNotEmpty()) {
                HorizontalDivider()
                
                Text(
                    text = "Historical Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                progressMetrics.take(10).forEach { metric ->
                    ProgressMetricRow(
                        timestamp = metric.timestamp,
                        totalEvents = metric.totalEvents,
                        trend = metric.trend
                    )
                }
            }
            
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
private fun ProgressMetricsCard(
    totalEvents: Int,
    totalPositiveImpact: Double,
    totalNegativeImpact: Double,
    averageScore: Double?,
    trend: Double
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
            MetricRow("Total Positive Impact", String.format("%.2f", totalPositiveImpact))
            MetricRow("Total Negative Impact", String.format("%.2f", totalNegativeImpact))
            
            averageScore?.let {
                MetricRow("Average Score", String.format("%.2f", it))
            }
            
            MetricRow("Trend", String.format("%.2f", trend))
        }
    }
}

@Composable
private fun ProgressMetricRow(
    timestamp: Long,
    totalEvents: Int,
    trend: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = formatTimestamp(timestamp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Events: $totalEvents",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Trend: ${String.format("%.2f", trend)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
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
    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestamp))
}


