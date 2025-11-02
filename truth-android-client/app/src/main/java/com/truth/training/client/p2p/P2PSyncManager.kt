package com.truth.training.client.p2p

import android.content.Context
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.repository.EventRepository
import com.truth.training.client.core.crypto.Ed25519CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Manages P2P synchronization of events between Android clients.
 * Propagates events to discovered peers via encrypted messages.
 */
class P2PSyncManager(
    private val context: Context,
    private val database: TruthDatabase,
    private val discoveryService: P2PDiscoveryService,
    private val messageHandler: P2PMessageHandler
) {
    private val eventRepository = EventRepository(database, null) // API not needed for local sync

    /**
     * Propagate event to all discovered peers.
     */
    suspend fun propagateEvent(eventId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val event = eventRepository.getEventById(eventId)
                ?: return@withContext Result.failure(IllegalArgumentException("Event not found: $eventId"))
            
            val peers = discoveryService.peersFlow.value
            if (peers.isEmpty()) {
                return@withContext Result.success(0)
            }
            
            Ed25519CryptoManager.init(context)
            
            // Create event message
            val eventPayload = JSONObject().apply {
                put("type", "EVENT_SYNC")
                put("event_id", event.id)
                put("title", event.title)
                put("description", event.description)
                put("category_id", event.categoryId)
                put("forma_id", event.formaId)
                put("cause_id", event.causeId)
                put("develop_id", event.developId)
                put("effect_id", event.effectId)
                put("start_date", event.startDate)
                put("end_date", event.endDate)
                put("created_at", event.createdAt)
                put("status", event.status)
            }
            
            val signature = Ed25519CryptoManager.signJsonPayload(eventPayload)
            val envelope = JSONObject().apply {
                put("payload", eventPayload)
                put("signature", signature)
                put("public_key", Ed25519CryptoManager.getPublicKeyBase64())
            }
            
            var propagatedCount = 0
            peers.forEach { peer ->
                try {
                    val response = P2PClient.send(context, peer.host, peer.port, envelope.toString())
                    if (response.isNotEmpty()) {
                        messageHandler.handleMessage(JSONObject(response), peer)
                        propagatedCount++
                    }
                } catch (e: Exception) {
                    // Log error but continue with other peers
                    android.util.Log.e("P2PSyncManager", "Failed to propagate to ${peer.host}:${peer.port}", e)
                }
            }
            
            Result.success(propagatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start listening for P2P event sync messages.
     */
    fun startListening() {
        // P2P server already handles incoming messages via P2PMessageHandler
        // This method can be extended for additional setup if needed
    }

    /**
     * Stop P2P synchronization.
     */
    fun stopListening() {
        // Cleanup if needed
    }
}

