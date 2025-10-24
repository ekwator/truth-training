package com.truth.training.client.p2p

import com.truth.training.client.TruthCore
import com.truth.training.client.core.crypto.Ed25519CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class P2PServer(private val scope: CoroutineScope) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    val port: Int get() = serverSocket?.localPort ?: -1

    fun start() {
        if (serverJob != null) return
        serverJob = scope.launch(Dispatchers.IO) {
            serverSocket = ServerSocket(0)
            while (!Thread.currentThread().isInterrupted) {
                val client = try { serverSocket!!.accept() } catch (_: Exception) { break }
                handleClient(client)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val writer = PrintWriter(s.getOutputStream(), true)
                val line = reader.readLine() ?: return@use
                val requestJson = line.trim()
                val response = try {
                    val req = JSONObject(requestJson)
                    val payload = req.optJSONObject("payload") ?: JSONObject()
                    val signature = req.optString("signature", "")
                    val pubKeyB64 = req.optString("public_key", "")
                    if (signature.isBlank() || pubKeyB64.isBlank()) {
                        JSONObject(mapOf("status" to "error", "reason" to "missing_signature")).toString()
                    } else {
                        // Проверяем подпись на payload как чистом JSON
                        val canonical = payload.toString()
                        val pub = Ed25519CryptoManager.decodePublicKeyFromBase64(pubKeyB64)
                        val ok = Ed25519CryptoManager.verifySignature(pub, canonical, signature)
                        if (!ok) {
                            JSONObject(mapOf("status" to "error", "reason" to "invalid_signature")).toString()
                        } else {
                            val result = TruthCore.processJson(payload.toString())
                            val base = if (result.isBlank()) JSONObject(mapOf("status" to "ok")) else JSONObject(result)
                            base.put("verified", true)
                            base.toString()
                        }
                    }
                } catch (e: Exception) {
                    JSONObject(mapOf("status" to "error", "reply" to (e.message ?: "exception"))).toString()
                }
                writer.println(response)
                writer.flush()
            }
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverJob?.cancel()
        serverJob = null
        serverSocket = null
    }
}


