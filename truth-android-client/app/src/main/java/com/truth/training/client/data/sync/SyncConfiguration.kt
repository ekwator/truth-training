package com.truth.training.client.data.sync

import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Configuration for WorkManager periodic synchronization.
 * 
 * Requirements:
 * - Periodic sync every 15 minutes
 * - Network constraint (CONNECTED required)
 * - Optional charging constraint (disabled by default)
 * - Exponential backoff retry (3 max retries)
 */
object SyncConfiguration {
    /**
     * Sync interval in minutes.
     */
    const val SYNC_INTERVAL_MINUTES = 15L
    
    /**
     * Flex interval in minutes (window for periodic work execution).
     * Work can execute any time within [interval, interval + flex] window.
     */
    const val SYNC_FLEX_MINUTES = 5L
    
    /**
     * Maximum retry count for failed sync operations.
     */
    const val MAX_RETRY_COUNT = 3

    /**
     * Create periodic sync work request (every 15 minutes when online).
     * 
     * Constraints:
     * - NetworkType.CONNECTED (requires active network connection)
     * - Charging: optional (disabled by default for better sync frequency)
     * 
     * Retry policy:
     * - Exponential backoff starting at MIN_BACKOFF_MILLIS
     * - Maximum 3 retries before marking as FAILED
     */
    fun createPeriodicSyncRequest(): PeriodicWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false) // Optional - set to true if you want sync only when charging
            .build()
        
        return PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
            SYNC_FLEX_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("sync_worker")
            .addTag("periodic_sync")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
    }

    /**
     * Create one-time sync work request (for immediate sync on user action).
     * 
     * Use this when user explicitly triggers sync (e.g., pull-to-refresh).
     */
    fun createOneTimeSyncRequest(): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .build()
        
        return OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag("sync_worker")
            .addTag("one_time_sync")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
    }
}

