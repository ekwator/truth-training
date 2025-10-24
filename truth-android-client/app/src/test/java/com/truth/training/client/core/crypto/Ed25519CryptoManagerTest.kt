package com.truth.training.client.core.crypto

import org.junit.Assert.assertTrue
import org.junit.Test

class Ed25519CryptoManagerTest {
    @Test
    fun sign_verify() {
        val kp = Ed25519CryptoManager.generateKeyPair()
        val msg = "{\"event\":\"truth_claim\",\"value\":1}"
        val sig = Ed25519CryptoManager.signMessage(kp.private, msg)
        assertTrue(Ed25519CryptoManager.verifySignature(kp.public, msg, sig))
    }

    @Test
    fun sign_json_payload() {
        Ed25519CryptoManager.initForTests()
        val payloadStr = "{\"event\":\"truth_claim\",\"value\":1}"
        val payload = org.json.JSONObject(payloadStr)
        val sig = Ed25519CryptoManager.signJsonPayload(payload)
        val pubB64 = Ed25519CryptoManager.getPublicKeyBase64()
        val pub = Ed25519CryptoManager.decodePublicKeyFromBase64(pubB64)
        assertTrue(Ed25519CryptoManager.verifySignature(pub, payloadStr, sig))
    }
}


