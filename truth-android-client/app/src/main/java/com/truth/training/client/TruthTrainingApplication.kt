package com.truth.training.client

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.TruthDatabaseMigrations
import com.truth.training.client.data.sync.SyncConfiguration
import com.truth.training.client.worker.NodeSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for Truth Training Android v1.0.0.
 * Initializes Room database, WorkManager, and dependency injection.
 */
class TruthTrainingApplication : Application(), Configuration.Provider {
    // Application scope for coroutines
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Room database instance (singleton)
    val database: TruthDatabase by lazy {
        try {
            TruthDatabase.getInstance(this)
        } catch (e: Exception) {
            android.util.Log.e("TruthTrainingApplication", "Failed to initialize database", e)
            // If database initialization fails, try to delete and recreate
            // This handles cases where database schema is corrupted
            try {
                android.util.Log.w("TruthTrainingApplication", "Attempting to recover from database error by deleting database file")
                deleteDatabase(com.truth.training.client.data.database.TruthDatabase.DATABASE_NAME)
                // Retry initialization after deletion
                TruthDatabase.getInstance(this)
            } catch (recoveryException: Exception) {
                android.util.Log.e("TruthTrainingApplication", "Database recovery failed", recoveryException)
                // Re-throw to prevent app from continuing with invalid database
                throw IllegalStateException("Database initialization and recovery failed. App cannot continue.", recoveryException)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        val appStartTime = System.currentTimeMillis()
        android.util.Log.d("TruthTrainingApplication", "Application.onCreate() started")
        
        // Pre-warm database in background to avoid blocking UI thread on first access
        applicationScope.launch {
            val dbStartTime = System.currentTimeMillis()
            try {
                // Access database to trigger lazy initialization in background
                database
                android.util.Log.d("TruthTrainingApplication", "Database pre-warmed in ${System.currentTimeMillis() - dbStartTime}ms")
            } catch (e: Exception) {
                android.util.Log.e("TruthTrainingApplication", "Database pre-warm failed", e)
            }
        }
        
        // Initialize Truth Core in background (non-blocking)
        applicationScope.launch {
            val coreStartTime = System.currentTimeMillis()
            try {
                TruthCore.initNode()
                android.util.Log.d("TruthTrainingApplication", "TruthCore initialized in ${System.currentTimeMillis() - coreStartTime}ms")
            } catch (e: Exception) {
                android.util.Log.w("TruthTrainingApplication", "TruthCore initialization failed", e)
            }
        }
        
        // Defer WorkManager workers until after UI is visible to improve launch time
        // Workers will be started after a short delay to allow UI to render first
        applicationScope.launch {
            kotlinx.coroutines.delay(2000) // Wait 2 seconds for UI to be visible
            android.util.Log.d("TruthTrainingApplication", "Starting background workers after UI delay")
            startPeriodicSync()
            startNodeDiscovery()
        }
        
        android.util.Log.d("TruthTrainingApplication", "Application.onCreate() completed in ${System.currentTimeMillis() - appStartTime}ms")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /**
     * Start periodic background sync worker.
     * Uses SyncConfiguration for consistent sync interval and constraints.
     */
    private fun startPeriodicSync() {
        val workRequest = SyncConfiguration.createPeriodicSyncRequest()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "sync_worker",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }
    
    /**
     * Start periodic node discovery worker.
     * Polls global registries, checks reachability, and prunes stale nodes.
     * 
     * TODO: Load registry URLs from user settings/preferences
     */
    private fun startNodeDiscovery() {
        // Default configuration - should be loaded from user settings
        val registryUrls = emptyList<String>() // TODO: Load from SharedPreferences or Settings
        val workRequest = NodeSyncWorker.createPeriodicWorkRequest(
            registryUrls = registryUrls,
            reachabilityTimeout = 5L,
            reachabilityRetries = 2
        )
        
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                NodeSyncWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
}

