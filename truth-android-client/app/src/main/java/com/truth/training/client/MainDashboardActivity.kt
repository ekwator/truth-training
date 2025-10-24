package com.truth.training.client

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_dashboard)

        val vm by viewModels<TruthViewModel>()

        val claimInput = findViewById<EditText>(R.id.inputClaim)
        val textInput = findViewById<EditText>(R.id.inputText)
        val output = findViewById<TextView>(R.id.jsonOutput)

        findViewById<Button>(R.id.btnSyncPeers).setOnClickListener { vm.syncPeers() }
        findViewById<Button>(R.id.btnSubmitClaim).setOnClickListener { vm.submitClaim(claimInput.text.toString()) }
        findViewById<Button>(R.id.btnGetClaims).setOnClickListener { vm.getClaims() }
        findViewById<Button>(R.id.btnAnalyzeText).setOnClickListener { vm.analyzeText(textInput.text.toString()) }
        findViewById<Button>(R.id.btnGetStats).setOnClickListener { vm.getStats() }

        lifecycleScope.launch {
            vm.response.collectLatest { output.text = it }
        }
    }
}


