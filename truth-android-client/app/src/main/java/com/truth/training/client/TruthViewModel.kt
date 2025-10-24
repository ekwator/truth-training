package com.truth.training.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class TruthViewModel : ViewModel() {
    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    private fun sendAction(json: JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = TruthCore.processJson(json.toString())
            _response.value = res
        }
    }

    fun syncPeers() {
        val req = JSONObject().apply { put("action", "sync_peers") }
        sendAction(req)
    }

    fun submitClaim(claim: String) {
        val req = JSONObject().apply {
            put("action", "submit_claim")
            put("claim", claim)
        }
        sendAction(req)
    }

    fun getClaims() {
        val req = JSONObject().apply { put("action", "get_claims") }
        sendAction(req)
    }

    fun analyzeText(text: String) {
        val req = JSONObject().apply {
            put("action", "analyze_text")
            put("text", text)
        }
        sendAction(req)
    }

    fun getStats() {
        val req = JSONObject().apply { put("action", "get_stats") }
        sendAction(req)
    }
}


