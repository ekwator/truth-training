package com.truth.training.client.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.network.dto.StatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for OverallSummaryScreen.
 * Provides aggregated statistics across all events.
 */
class OverallSummaryViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as? TruthTrainingApplication
        ?: throw IllegalStateException("Application must be TruthTrainingApplication")
    
    private val repository = TruthRepository(app, application.database)
    
    // All events for summary calculation
    val events: StateFlow<List<EventEntity>> = repository.eventRepository.getAllEventsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Stats from API
    private val _stats = MutableStateFlow<StatsResponse?>(null)
    val stats: StateFlow<StatsResponse?> = _stats.asStateFlow()
    
    // Aggregated metrics calculated from events
    data class AggregatedMetrics(
        val totalEvents: Int,
        val detectedEvents: Int,
        val eventsWithConsensus: Int,
        val averageCollectiveScore: Float?,
        val lastUpdated: Long
    )
    
    val aggregatedMetrics: StateFlow<AggregatedMetrics> = events
        .map { eventList ->
            val total = eventList.size
            val detected = eventList.count { it.detected == true }
            val withConsensus = eventList.count { it.collectiveScore != null }
            val avgScore = eventList
                .mapNotNull { it.collectiveScore }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toFloat()
            
            AggregatedMetrics(
                totalEvents = total,
                detectedEvents = detected,
                eventsWithConsensus = withConsensus,
                averageCollectiveScore = avgScore,
                lastUpdated = System.currentTimeMillis()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AggregatedMetrics(0, 0, 0, null, System.currentTimeMillis())
        )
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.fetchStats().fold(
                onSuccess = { _stats.value = it },
                onFailure = { _error.value = it.message }
            )
            
            _isLoading.value = false
        }
    }
    
    init {
        refresh()
    }
}

