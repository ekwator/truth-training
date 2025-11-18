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

/**
 * Application class for Truth Training Android v1.0.0.
 * Initializes Room database, WorkManager, and dependency injection.
 */
class TruthTrainingApplication : Application(), Configuration.Provider {
    // Application scope for coroutines
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Room database instance (singleton)
    val database: TruthDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            TruthDatabase::class.java,
            TruthDatabase.DATABASE_NAME
        )
            .addMigrations(
                TruthDatabaseMigrations.MIGRATION_1_2,
                TruthDatabaseMigrations.MIGRATION_2_3
            )
            .fallbackToDestructiveMigration() // Fallback for development/testing - will be removed in production
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Truth Core
        TruthCore.initNode()
        
        // Start periodic sync worker (if enabled)
        // Note: In production, this should be controlled by user settings
        startPeriodicSync()
        
        // Start periodic node discovery worker
        // Note: In production, this should be controlled by user settings
        startNodeDiscovery()
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

