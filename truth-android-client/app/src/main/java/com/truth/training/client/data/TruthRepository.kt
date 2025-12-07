package com.truth.training.client.data

import android.content.Context
import com.truth.training.client.BuildConfig
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.network.NetworkModule
import com.truth.training.client.data.network.MockTruthApi
import com.truth.training.client.data.network.TokenStorage
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.AuthRequest
import com.truth.training.client.data.network.dto.InfoResponse
import com.truth.training.client.data.network.dto.StatsResponse
import com.truth.training.client.data.repository.*
import com.truth.training.client.data.repository.DiscoveryRepository
import com.truth.training.client.data.repository.KnowledgeBaseRepository
import com.truth.training.client.data.sync.SyncQueueManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main repository integrating Room database, API client, and sync queue.
 * Implements offline-first architecture with automatic background sync.
 */
class TruthRepository(context: Context, database: TruthDatabase? = null) {
    private val tokenStorage = TokenStorage(context)
    private val api: TruthApi = if (BuildConfig.FLAVOR == "mock") {
        MockTruthApi(context)
    } else {
        NetworkModule.provideApi(
            NetworkModule.provideRetrofit(
                NetworkModule.provideOkHttp(context, tokenStorage)
            )
        )
    }
    
    // Room database (lazy initialization if not provided)
    private val db: TruthDatabase = database ?: throw IllegalStateException(
        "TruthDatabase must be initialized in Application class before creating TruthRepository"
    )
    
    // Repositories for each entity (lazy initialization to improve startup performance)
    val eventRepository: EventRepository by lazy { EventRepository(db, api) }
    val contextTemplateRepository: ContextTemplateRepository by lazy { ContextTemplateRepository(db, api) }
    val judgmentRepository: JudgmentRepository by lazy { JudgmentRepository(db, api) }
    val impactRepository: ImpactRepository by lazy { ImpactRepository(db, api) }
    val summaryRepository: SummaryRepository by lazy { SummaryRepository(db) }
    val knowledgeBaseRepository: KnowledgeBaseRepository by lazy { KnowledgeBaseRepository(db) }
    
    // Discovery repository (no API dependency, uses HTTP client internally)
    val discoveryRepository: DiscoveryRepository by lazy { DiscoveryRepository(db, null) }
    
    // Sync queue manager (lazy to avoid blocking during repository creation)
    val syncQueueManager: SyncQueueManager by lazy { SyncQueueManager(db) }

    private val _lastSync = MutableStateFlow<Long?>(null)
    val lastSync: Flow<Long?> = _lastSync.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Unknown)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val resp = api.authenticate(AuthRequest(email, password))
        if (!resp.isSuccessful || resp.body() == null) error("Auth failed: ${resp.code()}")
        val body = resp.body()!!
        tokenStorage.saveTokens(body.accessToken, body.refreshToken)
    }

    suspend fun fetchInfo(): Result<InfoResponse> = runCatching {
        val resp = api.getInfo()
        if (!resp.isSuccessful || resp.body() == null) error("Info failed: ${resp.code()}")
        _lastSync.value = System.currentTimeMillis()
        resp.body()!!
    }

    suspend fun fetchStats(): Result<StatsResponse> = runCatching {
        val resp = api.getStats()
        if (!resp.isSuccessful || resp.body() == null) error("Stats failed: ${resp.code()}")
        _lastSync.value = System.currentTimeMillis()
        resp.body()!!
    }

    suspend fun fetchGraphJson(): Result<String> = runCatching {
        val resp = api.getGraphJson()
        if (!resp.isSuccessful || resp.body() == null) error("Graph failed: ${resp.code()}")
        _lastSync.value = System.currentTimeMillis()
        resp.body()!!.string()
    }
    
    /**
     * Get sync status with pending operation count.
     */
    suspend fun getSyncStatus(): SyncStatus {
        val pendingCount = syncQueueManager.getPendingCount()
        // Check if database is open and accessible
        val isDbAvailable = try {
            if (db.isOpen) {
                val cursor = db.query("SELECT 1", null)
                val result = cursor.moveToFirst()
                cursor.close()
                result
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
        return SyncStatus(
            isOnline = isDbAvailable, // Database connection status
            pendingOperations = pendingCount,
            lastSyncTimestamp = _lastSync.value
        )
    }
}

/**
 * Sync status information.
 */
data class SyncStatus(
    val isOnline: Boolean,
    val pendingOperations: Int,
    val lastSyncTimestamp: Long? = null
) {
    companion object {
        val Unknown = SyncStatus(isOnline = false, pendingOperations = 0, lastSyncTimestamp = null)
    }
}


