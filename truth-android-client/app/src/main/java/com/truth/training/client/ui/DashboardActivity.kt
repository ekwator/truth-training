package com.truth.training.client.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.truth.training.client.R
import android.widget.TextView

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        val vm by viewModels<DashboardViewModel>()
        val connectionState = findViewById<TextView>(R.id.connectionState)
        val lastSync = findViewById<TextView>(R.id.lastSync)
        val infoView = findViewById<TextView>(R.id.infoView)
        val statsView = findViewById<TextView>(R.id.statsView)
        val errorView = findViewById<TextView>(R.id.errorText)
        val progress = findViewById<TextView>(R.id.progress)

        lifecycleScope.launch {
            vm.info.collectLatest { info ->
                infoView.text = info?.let { "version=${it.version} node=${it.nodeId}" } ?: ""
            }
        }
        lifecycleScope.launch {
            vm.stats.collectLatest { stats ->
                statsView.text = stats?.let { "peers=${it.peers} edges=${it.edges} avgTrust=${it.avgTrust}" } ?: ""
            }
        }
        lifecycleScope.launch {
            vm.lastSync.collectLatest { ts ->
                lastSync.text = ts?.let { getString(R.string.last_sync, it.toString()) } ?: ""
            }
        }
        lifecycleScope.launch {
            vm.error.collectLatest { err ->
                errorView.visibility = if (err.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                errorView.text = err.orEmpty()
            }
        }

        connectionState.text = getString(R.string.dashboard)
        vm.refresh()
    }
}


