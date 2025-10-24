package com.truth.training.client

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

class TruthCoreIntegrationTest {
    @Test
    fun builds_json_for_actions() {
        val actions = listOf("sync_peers", "submit_claim", "get_claims", "analyze_text", "get_stats")
        actions.forEach { action ->
            val obj = JSONObject().apply { put("action", action) }
            if (action == "submit_claim") obj.put("claim", "demo")
            if (action == "analyze_text") obj.put("text", "demo")
            val payload = obj.toString()
            // Smoke-check: JSON contains the action string
            assertTrue(payload.indexOf(action) >= 0)
        }
    }
}


