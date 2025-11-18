package com.truth.training.client.ui.compose.nodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.database.entities.NodeEntity
import com.truth.training.client.data.repository.DiscoveryRepository
import com.truth.training.client.worker.NodeSyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for NodesScreen.
 * Manages node discovery state and actions.
 */
class NodesViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: DiscoveryRepository = 
        DiscoveryRepository((application as TruthTrainingApplication).database, null)
    
    private val _nodeTypeFilter = MutableStateFlow<String?>(null)
    private val _reachableFilter = MutableStateFlow<Int?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _lastUpdated = MutableStateFlow<Long?>(null)
    
    val nodeTypeFilter: StateFlow<String?> = _nodeTypeFilter.asStateFlow()
    val reachableFilter: StateFlow<Int?> = _reachableFilter.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()
    val lastUpdated: StateFlow<Long?> = _lastUpdated.asStateFlow()
    
    // Combined flow of nodes based on filters
    val nodes: StateFlow<List<NodeEntity>> = combine(
        _nodeTypeFilter,
        _reachableFilter
    ) { typeFilter, reachableFilter ->
        repository.listNodes(
            nodeType = typeFilter,
            reachable = reachableFilter,
            limit = 0
        )
    }.flatMapLatest { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * Set node type filter (null = all types).
     */
    fun setNodeTypeFilter(type: String?) {
        _nodeTypeFilter.value = type
    }
    
    /**
     * Set reachability filter (null = all, 1 = reachable, 0 = unreachable).
     */
    fun setReachableFilter(reachable: Int?) {
        _reachableFilter.value = reachable
    }
    
    /**
     * Refresh nodes list.
     */
    fun refreshNodes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Force refresh by updating filter (triggers new query)
                val currentType = _nodeTypeFilter.value
                val currentReachable = _reachableFilter.value
                _nodeTypeFilter.value = null
                _reachableFilter.value = null
                _nodeTypeFilter.value = currentType
                _reachableFilter.value = currentReachable
                _lastUpdated.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _error.value = "Failed to refresh nodes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Trigger manual discovery.
     */
    fun discoverNodes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Trigger one-time sync worker
                val app = getApplication<TruthTrainingApplication>()
                androidx.work.WorkManager.getInstance(app)
                    .enqueue(NodeSyncWorker.createOneTimeWorkRequest())
                _error.value = null
                
                // Wait a bit then refresh
                kotlinx.coroutines.delay(2000)
                refreshNodes()
            } catch (e: Exception) {
                _error.value = "Discovery failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Cleanup stale nodes.
     */
    fun cleanupNodes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.pruneStaleNodes().fold(
                    onSuccess = { _ ->
                        _error.value = null
                        refreshNodes()
                    },
                    onFailure = { e ->
                        _error.value = "Cleanup failed: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Cleanup failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Run health check on all nodes.
     */
    fun runHealthCheck() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.runReachabilityChecks(
                    timeoutSeconds = 5L,
                    retries = 2
                ).fold(
                    onSuccess = { _ ->
                        _error.value = null
                        refreshNodes()
                    },
                    onFailure = { e ->
                        _error.value = "Health check failed: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Health check failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

