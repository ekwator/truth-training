package com.truth.training.client.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "truth_node_key"

    private fun ensureKey() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) return
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()
        kpg.initialize(spec)
        kpg.generateKeyPair()
    }

    private fun getPrivateKey() = KeyStore.getInstance(ANDROID_KEYSTORE).let { ks ->
        ks.load(null)
        (ks.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry).privateKey
    }

    private fun getPublicKey(): PublicKey = KeyStore.getInstance(ANDROID_KEYSTORE).let { ks ->
        ks.load(null)
        ks.getCertificate(KEY_ALIAS).publicKey
    }

    fun getPublicKeyBase64(): String {
        ensureKey()
        val pub = getPublicKey()
        return Base64.encodeToString(pub.encoded, Base64.NO_WRAP)
    }

    fun signMessage(message: String): String {
        ensureKey()
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(getPrivateKey())
        sig.update(message.toByteArray(Charsets.UTF_8))
        val signature = sig.sign()
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }

    fun verifySignature(message: String, signatureBase64: String, publicKey: PublicKey): Boolean {
        return try {
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update(message.toByteArray(Charsets.UTF_8))
            val bytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
            sig.verify(bytes)
        } catch (_: Exception) { false }
    }

    fun decodePublicKeyFromBase64(b64: String): PublicKey {
        val bytes = Base64.decode(b64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}


