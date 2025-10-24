package com.truth.training.client

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class JsonTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_json_test)

        val inputField = findViewById<EditText>(R.id.inputJson)
        val outputView = findViewById<TextView>(R.id.outputJson)
        val sendButton = findViewById<Button>(R.id.sendJson)

        sendButton.setOnClickListener {
            val request = inputField.text.toString()
            val response = TruthCore.processJsonRequest(request)
            outputView.text = response
        }
    }
}
