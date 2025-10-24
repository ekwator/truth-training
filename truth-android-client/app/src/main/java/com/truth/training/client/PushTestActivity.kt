package com.truth.training.client

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.truth.training.client.core.crypto.Ed25519CryptoManager
import com.truth.training.client.core.network.PushEnvelope
import com.truth.training.client.core.network.TruthPushClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class PushTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push_test)

        val input = findViewById<EditText>(R.id.inputPayload)
        val send = findViewById<Button>(R.id.btnSend)
        val output = findViewById<TextView>(R.id.output)

        send.setOnClickListener {
            val json = input.text.toString().ifBlank { JSONObject(mapOf("event" to "truth_claim", "value" to 1)).toString() }
            Ed25519CryptoManager.init(this)
            val payloadObj = JSONObject(json)
            val sig = Ed25519CryptoManager.signJsonPayload(payloadObj)
            val pub = Ed25519CryptoManager.getPublicKeyBase64()
            val body = PushEnvelope(payloadObj.toMap(), sig, pub)
            lifecycleScope.launch(Dispatchers.IO) {
                val bearer = "Bearer " + getSharedPreferences("truth_tokens", MODE_PRIVATE).getString("access", "")
                val resp = TruthPushClient.api.sendEvent(bearer, body)
                launch(Dispatchers.Main) {
                    output.text = "sent=${JSONObject().apply{put("payload",payloadObj);put("signature",sig);put("public_key",pub)}}\ncode=${resp.code()} success=${resp.isSuccessful}"
                }
            }
        }
    }
}

private fun JSONObject.toMap(): Map<String, Any> = keys().asSequence().associateWith { get(it) }


