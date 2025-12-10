package com.truth.training.client.ui.compose.judgments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truth.training.client.R
import com.truth.training.client.utils.EmojiMapping
import com.truth.training.client.data.database.entities.JudgmentEntity
import com.truth.training.client.data.network.dto.JudgmentStatsResponse

/**
 * Judgment List Screen (Compose) - Displays judgments for an event with consensus stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JudgmentListScreen(
    eventTitle: String,
    judgments: List<JudgmentEntity>,
    stats: JudgmentStatsResponse?,
    onNewJudgmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${EmojiMapping.getEmoji("screens", "judgments")} ${context.getString(R.string.judgments)}") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewJudgmentClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "${EmojiMapping.getEmoji("actions", "create")} ${context.getString(R.string.new_judgment)}"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Event title card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = eventTitle,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Consensus statistics
            if (stats != null) {
                ConsensusStatsCard(
                    stats = stats,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Judgments list
            if (judgments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.no_judgments_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(judgments, key = { it.id }) { judgment ->
                        JudgmentCard(
                            judgment = judgment,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsensusStatsCard(
    stats: JudgmentStatsResponse,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = context.getString(R.string.consensus_statistics),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(context.getString(R.string.assessment_true), stats.trueCount.toString())
                StatItem(context.getString(R.string.assessment_false), stats.falseCount.toString())
                StatItem(context.getString(R.string.uncertain), stats.uncertainCount.toString())
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = context.getString(R.string.average_confidence),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${(stats.avgConfidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = context.getString(R.string.total_judgments),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${stats.trueCount + stats.falseCount + stats.uncertainCount}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun JudgmentCard(
    judgment: JudgmentEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { 
                        Text(
                            text = judgment.assessment.uppercase(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (judgment.assessment) {
                            "true" -> MaterialTheme.colorScheme.primaryContainer
                            "false" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
                
                Text(
                    text = "${(judgment.confidenceLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (!judgment.reasoning.isNullOrEmpty()) {
                Text(
                    text = judgment.reasoning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = judgment.submittedAt.take(16), // Show date and time
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

