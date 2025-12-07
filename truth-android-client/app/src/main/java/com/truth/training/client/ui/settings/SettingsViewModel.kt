package com.truth.training.client.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.SyncStatus
import com.truth.training.client.data.config.AppConfig
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.KnowledgeBaseSeeder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * ViewModel for SettingsScreen.
 * Manages application configuration and connection settings.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as? TruthTrainingApplication
        ?: throw IllegalStateException("Application must be TruthTrainingApplication")
    
    private val repository = TruthRepository(app, application.database)
    private val appConfig = AppConfig(app)
    
    // Connection mode: "core" (local) or "http" (API)
    private val _connectionMode = MutableStateFlow(appConfig.connectionMode)
    val connectionMode: StateFlow<String> = _connectionMode.asStateFlow()
    
    // Server configuration
    private val _serverIp = MutableStateFlow(appConfig.serverIp ?: "")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()
    
    private val _serverPort = MutableStateFlow(appConfig.serverPort)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()
    
    // Nearby sync
    private val _nearbySyncEnabled = MutableStateFlow(appConfig.nearbySyncEnabled)
    val nearbySyncEnabled: StateFlow<Boolean> = _nearbySyncEnabled.asStateFlow()
    
    private val _nearbySyncInterval = MutableStateFlow(appConfig.nearbySyncInterval)
    val nearbySyncInterval: StateFlow<Long> = _nearbySyncInterval.asStateFlow()
    
    // Discovery worker settings
    private val _discoveryWorkerEnabled = MutableStateFlow(appConfig.discoveryWorkerEnabled)
    val discoveryWorkerEnabled: StateFlow<Boolean> = _discoveryWorkerEnabled.asStateFlow()
    
    private val _lanInterval = MutableStateFlow(appConfig.lanInterval)
    val lanInterval: StateFlow<Long> = _lanInterval.asStateFlow()
    
    private val _wifiInterval = MutableStateFlow(appConfig.wifiInterval)
    val wifiInterval: StateFlow<Long> = _wifiInterval.asStateFlow()
    
    private val _globalInterval = MutableStateFlow(appConfig.globalInterval)
    val globalInterval: StateFlow<Long> = _globalInterval.asStateFlow()
    
    private val _lanTtl = MutableStateFlow(appConfig.lanTtl)
    val lanTtl: StateFlow<Long> = _lanTtl.asStateFlow()
    
    private val _wifiTtl = MutableStateFlow(appConfig.wifiTtl)
    val wifiTtl: StateFlow<Long> = _wifiTtl.asStateFlow()
    
    private val _globalTtl = MutableStateFlow(appConfig.globalTtl)
    val globalTtl: StateFlow<Long> = _globalTtl.asStateFlow()
    
    // Connection status
    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncStatus.Unknown
    )
    
    private val _connectionTestResult = MutableStateFlow<String?>(appConfig.lastConnectionTestResult)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()
    
    private val _connectionTestTimestamp = MutableStateFlow(appConfig.lastConnectionTestTimestamp)
    val connectionTestTimestamp: StateFlow<Long> = _connectionTestTimestamp.asStateFlow()
    
    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Current locale
    private val _currentLocale = MutableStateFlow(appConfig.locale)
    val currentLocale: StateFlow<String> = _currentLocale.asStateFlow()
    
    fun setConnectionMode(mode: String) {
        _connectionMode.value = mode
    }
    
    fun setServerIp(ip: String) {
        _serverIp.value = ip
    }
    
    fun setServerPort(port: Int) {
        _serverPort.value = port
    }
    
    fun setNearbySyncEnabled(enabled: Boolean) {
        _nearbySyncEnabled.value = enabled
    }
    
    fun setNearbySyncInterval(interval: Long) {
        _nearbySyncInterval.value = interval
    }
    
    fun setDiscoveryWorkerEnabled(enabled: Boolean) {
        _discoveryWorkerEnabled.value = enabled
    }
    
    fun setLanInterval(interval: Long) {
        _lanInterval.value = interval
    }
    
    fun setWifiInterval(interval: Long) {
        _wifiInterval.value = interval
    }
    
    fun setGlobalInterval(interval: Long) {
        _globalInterval.value = interval
    }
    
    fun setLanTtl(ttl: Long) {
        _lanTtl.value = ttl
    }
    
    fun setWifiTtl(ttl: Long) {
        _wifiTtl.value = ttl
    }
    
    fun setGlobalTtl(ttl: Long) {
        _globalTtl.value = ttl
    }
    
    fun testConnection() {
        viewModelScope.launch {
            try {
                _isTestingConnection.value = true
                _error.value = null
                
                val result = if (_connectionMode.value == "core") {
                    // Test local Core connection
                    repository.fetchInfo().fold(
                        onSuccess = { "Success: Core connection working (version: ${it.version})" },
                        onFailure = { "Failed: ${it.message}" }
                    )
                } else {
                    // Test HTTP API connection
                    repository.fetchInfo().fold(
                        onSuccess = { "Success: HTTP API connection working (version: ${it.version})" },
                        onFailure = { "Failed: ${it.message}" }
                    )
                }
                
                // Use NonCancellable to ensure state is updated even if coroutine is cancelled
                withContext(NonCancellable) {
                    _connectionTestResult.value = result
                    _connectionTestTimestamp.value = System.currentTimeMillis()
                    appConfig.lastConnectionTestResult = result
                    appConfig.lastConnectionTestTimestamp = _connectionTestTimestamp.value
                }
            } catch (e: Exception) {
                val errorMessage = "Failed: ${e.message ?: "Unknown error"}"
                withContext(NonCancellable) {
                    _connectionTestResult.value = errorMessage
                    _connectionTestTimestamp.value = System.currentTimeMillis()
                    appConfig.lastConnectionTestResult = errorMessage
                    appConfig.lastConnectionTestTimestamp = _connectionTestTimestamp.value
                }
            } finally {
                withContext(NonCancellable) {
                    _isTestingConnection.value = false
                }
            }
        }
    }
    
    fun saveConnectionSettings() {
        appConfig.connectionMode = _connectionMode.value
        appConfig.serverIp = _serverIp.value.takeIf { it.isNotBlank() }
        appConfig.serverPort = _serverPort.value
        appConfig.nearbySyncEnabled = _nearbySyncEnabled.value
        appConfig.nearbySyncInterval = _nearbySyncInterval.value
    }
    
    fun saveDiscoveryWorkerSettings() {
        appConfig.discoveryWorkerEnabled = _discoveryWorkerEnabled.value
        appConfig.lanInterval = _lanInterval.value
        appConfig.wifiInterval = _wifiInterval.value
        appConfig.globalInterval = _globalInterval.value
        appConfig.lanTtl = _lanTtl.value
        appConfig.wifiTtl = _wifiTtl.value
        appConfig.globalTtl = _globalTtl.value
    }
    
    /**
     * Clears all events from the database.
     * Does not affect knowledge base, templates, or configuration.
     */
    fun clearEvents(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            try {
                val eventDao = application.database.eventDao()
                eventDao.clearAllEvents()
                android.util.Log.d("SettingsViewModel", "All events cleared successfully")
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to clear events: ${e.message}"
                android.util.Log.e("SettingsViewModel", "Failed to clear events", e)
            }
        }
    }
    
    /**
     * Changes the application language and re-seeds knowledge base.
     * 
     * @param locale Language code ("en" or "ru")
     * @param onLanguageChanged Callback invoked after language change is complete (should restart activity)
     */
    fun changeLanguage(locale: String, onLanguageChanged: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            try {
                val previousLocale = appConfig.locale
                
                // Only proceed if locale actually changed
                if (locale == previousLocale) {
                    return@launch
                }
                
                // Save new locale to AppConfig
                appConfig.locale = locale
                _currentLocale.value = locale
                
                // Clear context templates (as per requirement)
                val contextTemplateDao = application.database.contextTemplateDao()
                contextTemplateDao.clearAllTemplates()
                
                // Clear and re-seed knowledge base with new locale
                KnowledgeBaseSeeder.seedKnowledgeBase(
                    database = application.database,
                    locale = locale,
                    forceReseed = true
                )
                
                // Invoke callback to restart activity with new locale
                // This should be called on the main thread
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onLanguageChanged()
                }
            } catch (e: Exception) {
                _error.value = "Failed to change language: ${e.message}"
                android.util.Log.e("SettingsViewModel", "Failed to change language", e)
            }
        }
    }
    
    /**
     * Sets the language selection (updates state only, does not trigger change).
     * Use changeLanguage() to actually apply the change.
     */
    fun setLanguage(locale: String) {
        _currentLocale.value = locale
    }
}

