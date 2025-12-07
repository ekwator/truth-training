package com.truth.training.client.ui.judgments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.database.entities.JudgmentEntity
import com.truth.training.client.data.network.dto.JudgmentStatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for JudgmentListScreen.
 * Manages judgments list state and actions.
 */
class JudgmentListViewModel(
    app: Application,
    private val eventId: Long
) : AndroidViewModel(app) {
    
    private val application = app as? TruthTrainingApplication
        ?: throw IllegalStateException("Application must be TruthTrainingApplication")
    
    private val repository = TruthRepository(app, application.database)
    
    val event: StateFlow<EventEntity?> = repository.eventRepository.getEventByIdFlow(eventId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val judgments: StateFlow<List<JudgmentEntity>> = 
        repository.judgmentRepository.getJudgmentsForEventFlow(eventId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    private val _stats = MutableStateFlow<JudgmentStatsResponse?>(null)
    val stats: StateFlow<JudgmentStatsResponse?> = _stats.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.judgmentRepository.getJudgmentStats(eventId).fold(
                onSuccess = { _stats.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }
    
    fun refresh() {
        loadStats()
    }
}

