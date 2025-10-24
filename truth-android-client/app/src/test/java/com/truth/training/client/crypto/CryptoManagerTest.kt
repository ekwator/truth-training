package com.truth.training.client.crypto

import android.content.Context
import com.truth.training.client.core.crypto.Ed25519CryptoManager
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoManagerTest {
    @Test
    fun sign_and_verify_ed25519() {
        Ed25519CryptoManager.initForTests()
        val kp = Ed25519CryptoManager.generateKeyPair()
        val message = "hello"
        val sig = Ed25519CryptoManager.signMessage(kp.private, message)
        assertTrue(Ed25519CryptoManager.verifySignature(kp.public, message, sig))
    }
}


