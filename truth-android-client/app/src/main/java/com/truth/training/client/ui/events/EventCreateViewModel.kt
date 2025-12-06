package com.truth.training.client.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.network.dto.CreateEventRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for EventCreateScreen.
 * Manages event creation state and actions.
 */
class EventCreateViewModel(
    app: Application
) : AndroidViewModel(app) {
    
    private val application = app as? TruthTrainingApplication
        ?: throw IllegalStateException("Application must be TruthTrainingApplication")
    
    private val repository = TruthRepository(app, application.database)
    
    val templates: StateFlow<List<ContextTemplateEntity>> = 
        repository.contextTemplateRepository.getAllTemplatesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _createdEvent = MutableStateFlow<EventEntity?>(null)
    val createdEvent: StateFlow<EventEntity?> = _createdEvent.asStateFlow()
    
    fun createEvent(request: CreateEventRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.eventRepository.createEvent(request).fold(
                onSuccess = { event ->
                    _createdEvent.value = event
                    onSuccess()
                },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }
}

