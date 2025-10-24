package com.truth.training.client

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.truth.training.client.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Truth Core
        TruthCore.initNode()

        // Get runtime info from Rust core
        val info = TruthCore.getInfo()

        findViewById<TextView>(R.id.coreInfoView).text = info
    }
}
