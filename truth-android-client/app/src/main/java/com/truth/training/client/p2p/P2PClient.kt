package com.truth.training.client.p2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import android.content.Context
import com.truth.training.client.core.crypto.Ed25519CryptoManager
import org.json.JSONObject

object P2PClient {
    suspend fun send(context: Context, host: String, port: Int, json: String, timeoutMs: Int = 5000): String = withContext(Dispatchers.IO) {
        Ed25519CryptoManager.init(context)
        val payload = JSONObject(json)
        val signature = Ed25519CryptoManager.signJsonPayload(payload)
        val envelope = JSONObject().apply {
            put("payload", payload)
            put("signature", signature)
            put("public_key", Ed25519CryptoManager.getPublicKeyBase64())
        }
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer.println(envelope.toString())
            writer.flush()
            reader.readLine() ?: ""
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }
}


