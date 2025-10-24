package com.truth.training.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.network.dto.InfoResponse
import com.truth.training.client.data.network.dto.StatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = TruthRepository(app)

    private val _info = MutableStateFlow<InfoResponse?>(null)
    val info: StateFlow<InfoResponse?> = _info

    private val _stats = MutableStateFlow<StatsResponse?>(null)
    val stats: StateFlow<StatsResponse?> = _stats

    val lastSync = repository.lastSync

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun refresh() {
        viewModelScope.launch {
            val i = repository.fetchInfo()
            i.onSuccess { _info.value = it }.onFailure { _error.value = it.message }

            val s = repository.fetchStats()
            s.onSuccess { _stats.value = it }.onFailure { _error.value = it.message }
        }
    }
}


