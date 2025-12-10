package com.truth.training.client.ui.nodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.database.entities.NodeEntity
import com.truth.training.client.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for NodeDetailScreen.
 * Manages node detail state and actions.
 */
class NodeDetailViewModel(
    app: Application,
    private val nodeId: Long
) : AndroidViewModel(app) {
    
    private val application = app as? TruthTrainingApplication
        ?: throw IllegalStateException("Application must be TruthTrainingApplication")
    
    private val repository = DiscoveryRepository(application.database, null)
    
    private val _node = MutableStateFlow<NodeEntity?>(null)
    val node: StateFlow<NodeEntity?> = _node.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadNode()
    }
    
    /**
     * Load node by ID.
     */
    private fun loadNode() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val node = repository.getNodeById(nodeId)
                if (node != null) {
                    _node.value = node
                } else {
                    _error.value = "Node not found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load node: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh node data.
     */
    fun refresh() {
        loadNode()
    }
}


