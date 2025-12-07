package com.truth.training.client.ui.contexts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.database.entities.*
import com.truth.training.client.data.network.dto.CreateContextRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for ContextTemplateEditorScreen.
 * Manages context template creation/editing state and actions.
 */
class ContextTemplateEditorViewModel(
    app: Application,
    private val templateId: Int? = null
) : AndroidViewModel(app) {
    
    private val application = app as? TruthTrainingApplication
        ?: throw IllegalStateException("Application must be TruthTrainingApplication")
    
    private val repository = TruthRepository(app, application.database)
    
    val template: StateFlow<ContextTemplateEntity?> = if (templateId != null) {
        repository.contextTemplateRepository.getAllTemplatesFlow()
            .map { templates -> templates.find { it.id == templateId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    } else {
        MutableStateFlow(null).asStateFlow()
    }
    
    val categories: StateFlow<List<CategoryEntity>> = 
        repository.knowledgeBaseRepository.getAllCategoriesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    val formas: StateFlow<List<FormaEntity>> = 
        repository.knowledgeBaseRepository.getAllFormasFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    val causes: StateFlow<List<CauseEntity>> = 
        repository.knowledgeBaseRepository.getAllCausesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    val develops: StateFlow<List<DevelopEntity>> = 
        repository.knowledgeBaseRepository.getAllDevelopsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    val effects: StateFlow<List<EffectEntity>> = 
        repository.knowledgeBaseRepository.getAllEffectsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _savedTemplate = MutableStateFlow<ContextTemplateEntity?>(null)
    val savedTemplate: StateFlow<ContextTemplateEntity?> = _savedTemplate.asStateFlow()
    
    fun saveTemplate(request: CreateContextRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = if (templateId != null) {
                repository.contextTemplateRepository.updateTemplate(templateId, request)
            } else {
                repository.contextTemplateRepository.createTemplate(request)
            }
            result.fold(
                onSuccess = { template ->
                    _savedTemplate.value = template
                    onSuccess()
                },
                onFailure = { 
                    _error.value = it.message
                }
            )
            _isLoading.value = false
        }
    }
    
    fun deleteTemplate(onSuccess: () -> Unit) {
        if (templateId == null) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.contextTemplateRepository.deleteTemplate(templateId).fold(
                onSuccess = { onSuccess() },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }
}

