package com.truth.training.client.core.crypto

import android.content.Context
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object Ed25519CryptoManager {
    private const val KEY_PREFS = "ed25519_keys"
    private const val PUB = "pub"
    private const val PRIV = "priv"
    private var inited = false

    private fun ensureProvider() {
        if (inited) return
        // Try to add BouncyCastle if available; otherwise rely on default JDK provider (SunEC on JDK17+)
        runCatching {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
        inited = true
    }

    fun generateKeyPair(): KeyPair {
        ensureProvider()
        val kpg = KeyPairGenerator.getInstance("Ed25519")
        return kpg.generateKeyPair()
    }

    @Volatile private var cachedKeys: KeyPair? = null

    fun init(context: Context) {
        if (cachedKeys == null) synchronized(this) {
            if (cachedKeys == null) cachedKeys = loadOrCreateKeys(context)
        }
    }

    // For unit tests without Android Context
    fun initForTests() {
        if (cachedKeys == null) synchronized(this) {
            if (cachedKeys == null) cachedKeys = generateKeyPair()
        }
    }

    fun loadOrCreateKeys(context: Context): KeyPair {
        val prefs = context.getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)
        val pubB64 = prefs.getString(PUB, null)
        val privB64 = prefs.getString(PRIV, null)
        return if (pubB64 != null && privB64 != null) {
            val keyFactory = KeyFactory.getInstance("Ed25519")
            val pub = keyFactory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(pubB64)))
            val priv = java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(privB64)).let {
                keyFactory.generatePrivate(it)
            }
            KeyPair(pub, priv)
        } else {
            val kp = generateKeyPair()
            prefs.edit()
                .putString(PUB, base64EncodeNoPad(kp.public.encoded))
                .putString(PRIV, base64EncodeNoPad(kp.private.encoded))
                .apply()
            kp
        }
    }

    private fun base64EncodeNoPad(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)

    fun signMessage(privateKey: PrivateKey, message: String): String {
        ensureProvider()
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(privateKey)
        sig.update(message.toByteArray(Charsets.UTF_8))
        return base64EncodeNoPad(sig.sign())
    }

    fun verifySignature(publicKey: PublicKey, message: String, signatureB64: String): Boolean {
        return try {
            ensureProvider()
            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(publicKey)
            sig.update(message.toByteArray(Charsets.UTF_8))
            sig.verify(Base64.getDecoder().decode(signatureB64))
        } catch (_: Exception) { false }
    }

    fun getPublicKeyBase64(context: Context? = null): String {
        val kp = synchronized(this) {
            cachedKeys ?: if (context != null) loadOrCreateKeys(context) else generateKeyPair().also { cachedKeys = it }
        }
        return base64EncodeNoPad(kp.public.encoded)
    }

    fun decodePublicKeyFromBase64(b64: String): PublicKey {
        ensureProvider()
        val bytes = Base64.getDecoder().decode(b64)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("Ed25519").generatePublic(spec)
    }

    fun signJsonPayload(payload: org.json.JSONObject, context: Context? = null): String {
        val msg = payload.toString()
        val kp = synchronized(this) {
            cachedKeys ?: if (context != null) loadOrCreateKeys(context) else generateKeyPair().also { cachedKeys = it }
        }
        return signMessage(kp.private, msg)
    }
}


