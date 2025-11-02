package com.truth.training.client

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.sync.SyncConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application class for Truth Training Android v1.0.0.
 * Initializes Room database, WorkManager, and dependency injection.
 */
class TruthTrainingApplication : Application() {
    // Application scope for coroutines
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Room database instance (singleton)
    val database: TruthDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            TruthDatabase::class.java,
            TruthDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Truth Core
        TruthCore.initNode()
        
        // Initialize WorkManager with custom configuration
        val workManagerConfiguration = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
        WorkManager.initialize(this, workManagerConfiguration)
        
        // Start periodic sync worker (if enabled)
        // Note: In production, this should be controlled by user settings
        startPeriodicSync()
    }

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
}

