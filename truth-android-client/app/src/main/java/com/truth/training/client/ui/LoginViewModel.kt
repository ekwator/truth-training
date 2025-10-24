package com.truth.training.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.data.TruthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = TruthRepository(app)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    fun login(email: String, password: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = repository.login(email, password)
            _loading.value = false
            result.onSuccess { _success.value = true }
                .onFailure { _error.value = it.message }
        }
    }
}


