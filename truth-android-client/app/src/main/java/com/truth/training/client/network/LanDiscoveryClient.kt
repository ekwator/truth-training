package com.truth.training.client.network

import android.content.Context
import android.util.Log
import com.truth.training.client.core.crypto.Ed25519CryptoManager
import com.truth.training.client.data.repository.DiscoveryRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.*
import java.security.PublicKey
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UDP Multicast discovery client compatible with Rust implementation.
 * 
 * Implements T045: UDP multicast listener matching src/p2p/node.rs::run_lan_listener()
 * 
 * Requirements:
 * - Multicast address: 239.255.0.1:52525
 * - JSON payload format: LanAnnouncement { node_id, address, node_type, ttl, timestamp, signature }
 * - Signature verification using ed25519 (node_id as public key)
 * - Parse NodeType enum: "LAN", "WIFI", "GLOBAL", "RELAY", "CLIENT"
 * - Store discovered nodes via DiscoveryRepository
 * 
 * Matches Desktop/CLI implementation in src/p2p/node.rs::run_lan_listener()
 * 
 * Reference:
 * - Rust implementation: src/p2p/node.rs
 * - Format specification: docs/cross_platform_discovery_compatibility.md
 */
data class LanAnnouncement(
    val node_id: String,        // ed25519 public key (hex, 64 chars)
    val address: String,         // Full URL: "http://host:port/api/v1"
    val node_type: String,       // "LAN" | "WIFI" | "GLOBAL" | "RELAY" | "CLIENT"
    val ttl: Long,              // Time-to-live in seconds
    val timestamp: Long,        // Unix timestamp (seconds)
    val signature: String        // ed25519 signature (hex, 128 chars)
)

class LanDiscoveryClient(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: DiscoveryRepository? = null,
    private val selfNodeId: String? = null
) {
    companion object {
        private const val TAG = "LanDiscoveryClient"
        const val MULTICAST_ADDRESS = "239.255.0.1"
        const val MULTICAST_PORT = 52525
        private const val BUFFER_SIZE = 2048
    }
    
    private val isRunning = AtomicBoolean(false)
    private var socket: MulticastSocket? = null
    private var listenerJob: Job? = null
    
    private val discoveredNodes = MutableStateFlow<List<LanAnnouncement>>(emptyList())
    val nodesFlow: StateFlow<List<LanAnnouncement>> = discoveredNodes
    
    /**
     * Start UDP multicast listener.
     * 
     * Implementation:
     * 1. Create MulticastSocket bound to MULTICAST_PORT
     * 2. Join multicast group (MULTICAST_ADDRESS)
     * 3. Receive UDP packets in background coroutine
     * 4. Parse JSON payload to LanAnnouncement
     * 5. Verify signature
     * 6. Filter self-announcements (compare node_id)
     * 7. Emit to nodesFlow and store via DiscoveryRepository
     */
    fun startDiscovery() {
        if (isRunning.getAndSet(true)) {
            Log.w(TAG, "Discovery already running")
            return
        }
        
        listenerJob = scope.launch(Dispatchers.IO) {
            try {
                // Initialize crypto manager if needed
                Ed25519CryptoManager.init(context)
                
                // Create and bind multicast socket
                val multicastGroup = InetAddress.getByName(MULTICAST_ADDRESS)
                socket = MulticastSocket(MULTICAST_PORT).apply {
                    reuseAddress = true
                    timeToLive = 1 // Local network only
                    joinGroup(multicastGroup)
                }
                
                Log.d(TAG, "Started UDP multicast listener on $MULTICAST_ADDRESS:$MULTICAST_PORT")
                
                val buffer = ByteArray(BUFFER_SIZE)
                
                while (isRunning.get()) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        
                        val data = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val announcement = parseAnnouncement(data) ?: continue
                        
                        // Filter self-announcements
                        if (selfNodeId != null && announcement.node_id == selfNodeId) {
                            Log.d(TAG, "Ignoring self-announcement from ${announcement.node_id}")
                            continue
                        }
                        
                        // Verify signature
                        if (!verifyAnnouncementSignature(announcement)) {
                            Log.w(TAG, "Invalid signature for announcement from ${announcement.address}")
                            continue
                        }
                        
                        // Update discovered nodes list
                        val current = discoveredNodes.value.toMutableList()
                        val existingIndex = current.indexOfFirst { it.node_id == announcement.node_id }
                        if (existingIndex >= 0) {
                            current[existingIndex] = announcement
                        } else {
                            current.add(announcement)
                        }
                        discoveredNodes.value = current
                        
                        // Store via repository if available
                        repository?.let { repo ->
                            scope.launch(Dispatchers.IO) {
                                repo.processLanAnnouncement(announcement).fold(
                                    onSuccess = {
                                        Log.d(TAG, "Discovered LAN node ${announcement.node_id} at ${announcement.address}")
                                    },
                                    onFailure = { e ->
                                        Log.w(TAG, "Failed to store LAN node ${announcement.node_id}", e)
                                    }
                                )
                            }
                        }
                        
                    } catch (e: SocketTimeoutException) {
                        // Timeout is expected, continue listening
                        continue
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            Log.w(TAG, "Error receiving multicast packet", e)
                        }
                        // Continue listening on errors
                        delay(100) // Brief delay before retrying
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start multicast listener", e)
                isRunning.set(false)
            } finally {
                socket?.close()
                socket = null
            }
        }
    }
    
    /**
     * Stop UDP multicast listener.
     */
    fun stopDiscovery() {
        if (!isRunning.getAndSet(false)) {
            return
        }
        
        listenerJob?.cancel()
        listenerJob = null
        
        try {
            socket?.let { sock ->
                val multicastGroup = InetAddress.getByName(MULTICAST_ADDRESS)
                sock.leaveGroup(multicastGroup)
                sock.close()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error closing multicast socket", e)
        }
        
        socket = null
        Log.d(TAG, "Stopped UDP multicast listener")
    }
    
    /**
     * Parse JSON payload to LanAnnouncement.
     */
    private fun parseAnnouncement(jsonString: String): LanAnnouncement? {
        return try {
            val json = JSONObject(jsonString)
            LanAnnouncement(
                node_id = json.getString("node_id"),
                address = json.getString("address"),
                node_type = json.getString("node_type"),
                ttl = json.getLong("ttl"),
                timestamp = json.getLong("timestamp"),
                signature = json.getString("signature")
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse announcement JSON: $jsonString", e)
            null
        }
    }
    
    /**
     * Verify announcement signature.
     * 
     * Signature payload format: "{node_id}|{address}|{node_type}|{ttl}|{timestamp}"
     * 
     * Steps:
     * 1. Decode node_id from hex to ed25519 public key
     * 2. Decode signature from hex
     * 3. Construct payload string
     * 4. Verify signature using ed25519
     * 
     * Reference: src/p2p/node.rs::verify_announcement_signature()
     */
    private fun verifyAnnouncementSignature(announcement: LanAnnouncement): Boolean {
        return try {
            // Construct payload string: "{node_id}|{address}|{node_type}|{ttl}|{timestamp}"
            val payload = "${announcement.node_id}|${announcement.address}|${announcement.node_type}|${announcement.ttl}|${announcement.timestamp}"
            
            // Decode node_id (hex string) to PublicKey
            val publicKey = Ed25519CryptoManager.decodePublicKeyFromHex(announcement.node_id)
                ?: return false
            
            // Decode signature (hex string) to ByteArray
            val signatureBytes = hexStringToByteArray(announcement.signature)
            
            // Verify signature
            Ed25519CryptoManager.verifySignature(publicKey, payload, signatureBytes)
        } catch (e: Exception) {
            Log.w(TAG, "Signature verification failed for ${announcement.node_id}", e)
            false
        }
    }
    
    /**
     * Convert hex string to ByteArray.
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
