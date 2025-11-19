package com.truth.training.client.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.repository.DiscoveryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for periodic node discovery and sync.
 * 
 * Implements T044: NodeSyncWorker with:
 * - Periodic global registry polling (if configured)
 * - HTTP reachability checks for all nodes
 * - TTL-based cleanup of stale nodes
 * - Integration with DiscoveryRepository
 * 
 * Matches Desktop implementation in ui/desktop/src-tauri/src/discovery.rs::restart_worker()
 * 
 * Reference:
 * - Desktop: ui/desktop/src-tauri/src/discovery.rs
 * - Core: src/p2p/node.rs::poll_global_registries(), run_http_reachability_checks()
 * - Storage: core/src/storage.rs::prune_stale_nodes()
 */
class NodeSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(Companion.TAG, "Starting node sync worker")
            
            // Get database and repository from Application
            val app = applicationContext as? TruthTrainingApplication
                ?: return@withContext Result.failure()
            
            val database = app.database
            val repository = DiscoveryRepository(database, null)
            
            // Extract configuration from input data
            val registryUrls = inputData.getStringArray(Companion.KEY_REGISTRY_URLS)?.toList() ?: emptyList()
            val reachabilityTimeout = inputData.getLong(Companion.KEY_REACHABILITY_TIMEOUT, 5L)
            val reachabilityRetries = inputData.getInt(Companion.KEY_REACHABILITY_RETRIES, 2)
            
            var totalOperations = 0
            val errors = mutableListOf<String>()
            
            // 1. Poll global registries (if configured)
            if (registryUrls.isNotEmpty()) {
                Log.d(Companion.TAG, "Polling ${registryUrls.size} global registries")
                repository.pollGlobalRegistries(registryUrls).fold(
                    onSuccess = { count ->
                        totalOperations += count
                        Log.d(Companion.TAG, "Discovered $count nodes from global registries")
                    },
                    onFailure = { e ->
                        val error = "Global registry polling failed: ${e.message}"
                        Log.w(Companion.TAG, error, e)
                        errors.add(error)
                    }
                )
            }
            
            // 2. Run HTTP reachability checks for all nodes
            Log.d(Companion.TAG, "Running reachability checks")
            repository.runReachabilityChecks(
                timeoutSeconds = reachabilityTimeout,
                retries = reachabilityRetries
            ).fold(
                onSuccess = { count ->
                    totalOperations += count
                    Log.d(Companion.TAG, "Checked reachability for $count nodes")
                },
                onFailure = { e ->
                    val error = "Reachability checks failed: ${e.message}"
                    Log.w(Companion.TAG, error, e)
                    errors.add(error)
                }
            )
            
            // 3. Prune stale nodes (TTL expired or unreachable for > ttl/2)
            Log.d(Companion.TAG, "Pruning stale nodes")
            repository.pruneStaleNodes().fold(
                onSuccess = { count ->
                    if (count > 0) {
                        totalOperations += count
                        Log.d(Companion.TAG, "Pruned $count stale nodes")
                    }
                },
                onFailure = { e ->
                    val error = "Node pruning failed: ${e.message}"
                    Log.w(Companion.TAG, error, e)
                    errors.add(error)
                }
            )
            
            Log.d(Companion.TAG, "Node sync worker completed. Operations: $totalOperations")
            
            // Return success if at least some operations succeeded
            // or if no operations were needed (no registries configured)
            if (errors.isEmpty() || totalOperations > 0) {
                Result.success()
            } else {
                // If all operations failed, retry
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(Companion.TAG, "Node sync worker failed with exception", e)
            // Retry on transient errors, fail on permanent errors
            if (isTransientError(e)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Determine if an error is transient and should be retried.
     */
    private fun isTransientError(e: Exception): Boolean {
        // Network errors are typically transient
        return e.message?.contains("network", ignoreCase = true) == true
            || e.message?.contains("timeout", ignoreCase = true) == true
            || e.message?.contains("connection", ignoreCase = true) == true
    }
    
    companion object {
        private const val TAG = "NodeSyncWorker"
        
        /**
         * Work name for unique periodic work scheduling.
         */
        const val WORK_NAME = "node_sync_worker"
        
        /**
         * Input data keys for worker configuration.
         */
        private const val KEY_REGISTRY_URLS = "registry_urls"
        private const val KEY_REACHABILITY_TIMEOUT = "reachability_timeout_seconds"
        private const val KEY_REACHABILITY_RETRIES = "reachability_retries"
        /**
         * Create WorkRequest for periodic execution.
         * 
         * Matches Desktop timing:
         * - Global registry polling: hourly (or as configured)
         * - Reachability checks: every 5 minutes
         * - Cleanup: every minute
         * 
         * WorkManager minimum interval is 15 minutes, so we run all operations
         * every 15 minutes and let DiscoveryRepository handle internal timing.
         * 
         * @param registryUrls List of global registry URLs to poll
         * @param reachabilityTimeout HTTP timeout in seconds (default: 5)
         * @param reachabilityRetries Number of retry attempts (default: 2)
         */
        fun createPeriodicWorkRequest(
            registryUrls: List<String> = emptyList(),
            reachabilityTimeout: Long = 5L,
            reachabilityRetries: Int = 2
        ): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false) // Allow sync even when not charging
                .build()
            
            val inputData = Data.Builder()
                .putStringArray(Companion.KEY_REGISTRY_URLS, registryUrls.toTypedArray())
                .putLong(Companion.KEY_REACHABILITY_TIMEOUT, reachabilityTimeout)
                .putInt(Companion.KEY_REACHABILITY_RETRIES, reachabilityRetries)
                .build()
            
            return PeriodicWorkRequestBuilder<NodeSyncWorker>(
                15, // Minimum interval (minutes) - WorkManager requirement
                java.util.concurrent.TimeUnit.MINUTES,
                5, // Flex interval (minutes) - allows execution within [15, 20] minute window
                java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag("node_sync")
                .addTag("discovery")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
        }
        
        /**
         * Create one-time work request for immediate sync (e.g., manual trigger).
         */
        fun createOneTimeWorkRequest(
            registryUrls: List<String> = emptyList(),
            reachabilityTimeout: Long = 5L,
            reachabilityRetries: Int = 2
        ): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()
            
            val inputData = Data.Builder()
                .putStringArray(Companion.KEY_REGISTRY_URLS, registryUrls.toTypedArray())
                .putLong(Companion.KEY_REACHABILITY_TIMEOUT, reachabilityTimeout)
                .putInt(Companion.KEY_REACHABILITY_RETRIES, reachabilityRetries)
                .build()
            
            return OneTimeWorkRequestBuilder<NodeSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag("node_sync")
                .addTag("discovery")
                .addTag("one_time")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
        }
    }
}
