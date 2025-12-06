package com.truth.training.client.data.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Application configuration manager using SharedPreferences.
 * Stores connection settings, discovery worker settings, and other app preferences.
 */
class AppConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "truth_app_config",
        Context.MODE_PRIVATE
    )
    
    // Connection mode: "core" (local) or "http" (API)
    var connectionMode: String
        get() = prefs.getString(KEY_CONNECTION_MODE, "core") ?: "core"
        set(value) = prefs.edit().putString(KEY_CONNECTION_MODE, value).apply()
    
    // Server configuration
    var serverIp: String?
        get() = prefs.getString(KEY_SERVER_IP, null)
        set(value) = prefs.edit().putString(KEY_SERVER_IP, value).apply()
    
    var serverPort: Int
        get() = prefs.getInt(KEY_SERVER_PORT, 8080)
        set(value) = prefs.edit().putInt(KEY_SERVER_PORT, value).apply()
    
    // Nearby sync (UDP broadcast discovery)
    var nearbySyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_NEARBY_SYNC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NEARBY_SYNC_ENABLED, value).apply()
    
    var nearbySyncInterval: Long
        get() = prefs.getLong(KEY_NEARBY_SYNC_INTERVAL, 5000L)
        set(value) = prefs.edit().putLong(KEY_NEARBY_SYNC_INTERVAL, value).apply()
    
    // Discovery worker settings
    var discoveryWorkerEnabled: Boolean
        get() = prefs.getBoolean(KEY_DISCOVERY_WORKER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DISCOVERY_WORKER_ENABLED, value).apply()
    
    var lanInterval: Long
        get() = prefs.getLong(KEY_LAN_INTERVAL, 60000L)
        set(value) = prefs.edit().putLong(KEY_LAN_INTERVAL, value).apply()
    
    var wifiInterval: Long
        get() = prefs.getLong(KEY_WIFI_INTERVAL, 60000L)
        set(value) = prefs.edit().putLong(KEY_WIFI_INTERVAL, value).apply()
    
    var globalInterval: Long
        get() = prefs.getLong(KEY_GLOBAL_INTERVAL, 300000L)
        set(value) = prefs.edit().putLong(KEY_GLOBAL_INTERVAL, value).apply()
    
    var lanTtl: Long
        get() = prefs.getLong(KEY_LAN_TTL, 120L)
        set(value) = prefs.edit().putLong(KEY_LAN_TTL, value).apply()
    
    var wifiTtl: Long
        get() = prefs.getLong(KEY_WIFI_TTL, 120L)
        set(value) = prefs.edit().putLong(KEY_WIFI_TTL, value).apply()
    
    var globalTtl: Long
        get() = prefs.getLong(KEY_GLOBAL_TTL, 300L)
        set(value) = prefs.edit().putLong(KEY_GLOBAL_TTL, value).apply()
    
    // Connection test result
    var lastConnectionTestResult: String?
        get() = prefs.getString(KEY_LAST_CONNECTION_TEST_RESULT, null)
        set(value) = prefs.edit().putString(KEY_LAST_CONNECTION_TEST_RESULT, value).apply()
    
    var lastConnectionTestTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CONNECTION_TEST_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CONNECTION_TEST_TIMESTAMP, value).apply()
    
    /**
     * Reset all configuration to defaults.
     */
    fun reset() {
        prefs.edit().clear().apply()
    }
    
    companion object {
        private const val KEY_CONNECTION_MODE = "connection_mode"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_NEARBY_SYNC_ENABLED = "nearby_sync_enabled"
        private const val KEY_NEARBY_SYNC_INTERVAL = "nearby_sync_interval"
        private const val KEY_DISCOVERY_WORKER_ENABLED = "discovery_worker_enabled"
        private const val KEY_LAN_INTERVAL = "lan_interval"
        private const val KEY_WIFI_INTERVAL = "wifi_interval"
        private const val KEY_GLOBAL_INTERVAL = "global_interval"
        private const val KEY_LAN_TTL = "lan_ttl"
        private const val KEY_WIFI_TTL = "wifi_ttl"
        private const val KEY_GLOBAL_TTL = "global_ttl"
        private const val KEY_LAST_CONNECTION_TEST_RESULT = "last_connection_test_result"
        private const val KEY_LAST_CONNECTION_TEST_TIMESTAMP = "last_connection_test_timestamp"
    }
}


