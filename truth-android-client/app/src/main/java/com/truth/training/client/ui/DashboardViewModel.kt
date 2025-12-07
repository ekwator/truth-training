package com.truth.training.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.data.SyncStatus
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.network.dto.InfoResponse
import com.truth.training.client.data.network.dto.StatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(app: Application, database: TruthDatabase) : AndroidViewModel(app) {
    private val repository = TruthRepository(app, database)

    private val _info = MutableStateFlow<InfoResponse?>(null)
    val info: StateFlow<InfoResponse?> = _info

    private val _stats = MutableStateFlow<StatsResponse?>(null)
    val stats: StateFlow<StatsResponse?> = _stats

    val lastSync = repository.lastSync

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncStatus.Unknown
    )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Get event count from EventRepository
    val eventCount: StateFlow<Int> = repository.eventRepository.getAllEventsFlow()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun refresh() {
        viewModelScope.launch {
            val i = repository.fetchInfo()
            i.onSuccess { _info.value = it }.onFailure { _error.value = it.message }

            val s = repository.fetchStats()
            s.onSuccess { _stats.value = it }.onFailure { _error.value = it.message }
        }
    }
}


