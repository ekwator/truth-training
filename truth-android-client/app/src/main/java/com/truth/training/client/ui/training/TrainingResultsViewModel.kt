package com.truth.training.client.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.database.entities.ProgressMetricsEntity
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.database.entities.JudgmentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for TrainingResultsScreen.
 * Provides training progress and results data.
 */
class TrainingResultsViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as? TruthTrainingApplication
        ?: throw IllegalStateException("Application must be TruthTrainingApplication")
    
    private val repository = TruthRepository(app, application.database)
    private val progressMetricsDao = application.database.progressMetricsDao()
    
    // Progress metrics from database
    val progressMetrics: StateFlow<List<ProgressMetricsEntity>> = progressMetricsDao.listProgressMetricsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Latest progress metrics
    val latestProgress: StateFlow<ProgressMetricsEntity?> = progressMetricsDao.getLatestProgressMetricsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // All events for training data
    val events: StateFlow<List<EventEntity>> = repository.eventRepository.getAllEventsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Training summary data
    data class TrainingSummary(
        val totalEvents: Int,
        val totalPositiveImpact: Double,
        val totalNegativeImpact: Double,
        val averageScore: Double?,
        val trend: Double
    )
    
    val trainingSummary: StateFlow<TrainingSummary> = combine(
        latestProgress,
        events
    ) { latest, eventList ->
        if (latest != null) {
            TrainingSummary(
                totalEvents = latest.totalEvents,
                totalPositiveImpact = latest.totalPositiveImpact,
                totalNegativeImpact = latest.totalNegativeImpact,
                averageScore = if (latest.totalEvents > 0) {
                    (latest.totalPositiveImpact + latest.totalNegativeImpact) / latest.totalEvents
                } else null,
                trend = latest.trend
            )
        } else {
            // Calculate from events if no progress metrics
            val total = eventList.size
            val positive = eventList.sumOf { it.collectiveScore?.toDouble()?.coerceAtLeast(0.0) ?: 0.0 }
            val negative = eventList.sumOf { 
                val score = it.collectiveScore?.toDouble() ?: 0.0
                if (score < 0) -score else 0.0
            }
            val avg = if (total > 0) (positive + negative) / total else null
            
            TrainingSummary(
                totalEvents = total,
                totalPositiveImpact = positive,
                totalNegativeImpact = negative,
                averageScore = avg,
                trend = 0.0 // TODO: Calculate trend from historical data
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrainingSummary(0, 0.0, 0.0, null, 0.0)
        )
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // Progress metrics are updated by background workers
            // Just trigger a refresh of the flow
            _isLoading.value = false
        }
    }
}


