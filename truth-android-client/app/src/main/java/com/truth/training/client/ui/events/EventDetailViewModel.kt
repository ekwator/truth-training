package com.truth.training.client.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.database.entities.ImpactEntity
import com.truth.training.client.data.database.entities.JudgmentEntity
import com.truth.training.client.data.network.dto.UpdateEventRequest
import com.truth.training.client.data.network.dto.CreateImpactRequest
import com.truth.training.client.data.network.dto.CreateJudgmentRequest
import com.truth.training.client.utils.ImpactLevelMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for EventDetailScreen.
 * Manages event detail state and actions.
 */
class EventDetailViewModel(
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
    
    // Use SharingStarted.Lazily to ensure flows restart when subscribers appear
    // This is critical for context fields display after language change, as Room flows
    // need to re-collect data after database transactions (clear + reseed knowledge base)
    // Lazily starts collection when first subscriber appears and keeps it active while
    // there are subscribers, ensuring immediate updates after database changes
    val categories: StateFlow<List<com.truth.training.client.data.database.entities.CategoryEntity>> = 
        repository.knowledgeBaseRepository.getAllCategoriesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )
    
    val formas: StateFlow<List<com.truth.training.client.data.database.entities.FormaEntity>> = 
        repository.knowledgeBaseRepository.getAllFormasFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )
    
    val causes: StateFlow<List<com.truth.training.client.data.database.entities.CauseEntity>> = 
        repository.knowledgeBaseRepository.getAllCausesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )
    
    val develops: StateFlow<List<com.truth.training.client.data.database.entities.DevelopEntity>> = 
        repository.knowledgeBaseRepository.getAllDevelopsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )
    
    val effects: StateFlow<List<com.truth.training.client.data.database.entities.EffectEntity>> = 
        repository.knowledgeBaseRepository.getAllEffectsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )
    
    // Impacts and Judgments flows - reactive updates for EventDetailScreen
    val impacts: StateFlow<List<ImpactEntity>> = 
        repository.impactRepository.getImpactsForEventFlow(eventId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    val judgments: StateFlow<List<JudgmentEntity>> = 
        repository.judgmentRepository.getJudgmentsForEventFlow(eventId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _impactLoading = MutableStateFlow(false)
    val impactLoading: StateFlow<Boolean> = _impactLoading.asStateFlow()
    
    private val _impactError = MutableStateFlow<String?>(null)
    val impactError: StateFlow<String?> = _impactError.asStateFlow()
    
    private val _judgmentLoading = MutableStateFlow(false)
    val judgmentLoading: StateFlow<Boolean> = _judgmentLoading.asStateFlow()
    
    private val _judgmentError = MutableStateFlow<String?>(null)
    val judgmentError: StateFlow<String?> = _judgmentError.asStateFlow()
    
    fun deleteEvent(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.eventRepository.deleteEvent(eventId).fold(
                onSuccess = { onSuccess() },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }
    
    fun updateEvent(id: Long, request: UpdateEventRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.eventRepository.updateEvent(id, request).fold(
                onSuccess = { onSuccess() },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.eventRepository.syncFromServer().onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }
    
    /**
     * Add an impact to the event.
     * Maps impact level (1-5) to boolean value using ImpactLevelMapper.
     * 
     * @param impactLevel Impact level in range 1-5
     * @param notes Optional notes for the impact
     */
    fun addImpact(impactLevel: Int, notes: String?) {
        viewModelScope.launch {
            _impactLoading.value = true
            _impactError.value = null
            
            // Validate impact level
            if (!ImpactLevelMapper.isValid(impactLevel)) {
                _impactError.value = "Impact level must be in range 1-5"
                _impactLoading.value = false
                return@launch
            }
            
            // Map impact level to boolean value
            val value = ImpactLevelMapper.mapToBoolean(impactLevel)
            
            // Create request
            val request = CreateImpactRequest(
                eventId = eventId,
                value = value,
                notes = notes?.takeIf { it.isNotBlank() }
            )
            
            // Submit impact
            repository.impactRepository.addImpact(request).fold(
                onSuccess = {
                    _impactLoading.value = false
                },
                onFailure = {
                    _impactError.value = it.message ?: "Failed to add impact"
                    _impactLoading.value = false
                }
            )
        }
    }
    
    /**
     * Submit a judgment for the event.
     * 
     * @param assessment Assessment value: "true", "false", or "uncertain"
     * @param confidenceLevel Confidence level between 0.0 and 1.0
     * @param reasoning Optional reasoning text
     */
    fun submitJudgment(assessment: String, confidenceLevel: Double, reasoning: String?) {
        viewModelScope.launch {
            _judgmentLoading.value = true
            _judgmentError.value = null
            
            // Validate assessment
            if (assessment !in listOf("true", "false", "uncertain")) {
                _judgmentError.value = "Assessment must be 'true', 'false', or 'uncertain'"
                _judgmentLoading.value = false
                return@launch
            }
            
            // Validate confidence level
            if (confidenceLevel < 0.0 || confidenceLevel > 1.0) {
                _judgmentError.value = "Confidence level must be between 0.0 and 1.0"
                _judgmentLoading.value = false
                return@launch
            }
            
            // Create request
            val request = CreateJudgmentRequest(
                eventId = eventId,
                assessment = assessment,
                confidenceLevel = confidenceLevel,
                reasoning = reasoning?.takeIf { it.isNotBlank() }
            )
            
            // Submit judgment
            repository.judgmentRepository.submitJudgment(request).fold(
                onSuccess = {
                    _judgmentLoading.value = false
                },
                onFailure = {
                    _judgmentError.value = it.message ?: "Failed to submit judgment"
                    _judgmentLoading.value = false
                }
            )
        }
    }
}

