package com.truth.training.client.p2p

import android.content.Context
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.repository.EventRepository
import com.truth.training.client.core.crypto.Ed25519CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Handles encrypted P2P messages for event synchronization.
 * Verifies Ed25519 signatures and processes event sync messages.
 */
class P2PMessageHandler(
    private val context: Context,
    private val database: TruthDatabase
) {
    private val eventRepository = EventRepository(database, null)

    /**
     * Handle incoming P2P message.
     * Verifies signature and processes based on message type.
     */
    suspend fun handleMessage(envelope: JSONObject, peer: P2PPeer): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = envelope.getJSONObject("payload")
            val signature = envelope.getString("signature")
            val publicKey = envelope.getString("public_key")
            
            // Verify Ed25519 signature
            Ed25519CryptoManager.init(context)
            val publicKeyObj = Ed25519CryptoManager.decodePublicKeyFromBase64(publicKey)
            val payloadStr = payload.toString()
            if (!Ed25519CryptoManager.verifySignature(publicKeyObj, payloadStr, signature)) {
                return@withContext Result.failure(SecurityException("Invalid message signature"))
            }
            
            // Process message based on type
            val messageType = payload.optString("type", "")
            when (messageType) {
                "EVENT_SYNC" -> handleEventSync(payload, peer)
                else -> {
                    // Unknown message type - log but don't fail
                    android.util.Log.w("P2PMessageHandler", "Unknown message type: $messageType")
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Handle EVENT_SYNC message - save event from peer.
     */
    private suspend fun handleEventSync(payload: JSONObject, peer: P2PPeer): Result<Unit> {
        return try {
            val eventId = payload.getString("event_id")
            
            // Check if event already exists (local-wins conflict resolution)
            val existing = eventRepository.getEventById(eventId)
            if (existing != null) {
                // Event already exists locally - skip (local-wins)
                return Result.success(Unit)
            }
            
            // Create event entity from P2P message
            val entity = EventEntity(
                id = eventId,
                title = payload.getString("title"),
                description = payload.optString("description").takeIf { !it.isNullOrEmpty() },
                categoryId = payload.optInt("category_id").takeIf { it > 0 },
                formaId = payload.optInt("forma_id").takeIf { it > 0 },
                causeId = payload.optInt("cause_id").takeIf { it > 0 },
                developId = payload.optInt("develop_id").takeIf { it > 0 },
                effectId = payload.optInt("effect_id").takeIf { it > 0 },
                startDate = payload.optString("start_date").takeIf { !it.isNullOrEmpty() },
                endDate = payload.optString("end_date").takeIf { !it.isNullOrEmpty() },
                createdAt = payload.getString("created_at"),
                updatedAt = null,
                status = payload.optString("status", "active")
            )
            
            // Save to local database
            val dao = database.eventDao()
            dao.insertEvent(entity)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

