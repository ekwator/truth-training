package com.truth.training.client

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.truth.training.client.p2p.P2PClient
import com.truth.training.client.p2p.P2PDiscoveryService
import com.truth.training.client.p2p.P2PServer
import com.truth.training.client.crypto.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class P2PActivity : AppCompatActivity() {
    private lateinit var discovery: P2PDiscoveryService
    private lateinit var server: P2PServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p2p)

        server = P2PServer(lifecycleScope)
        server.start()

        discovery = P2PDiscoveryService(this, lifecycleScope)
        discovery.registerService(server.port, "truthnode-${System.currentTimeMillis()}")
        discovery.startDiscovery()

        val peersList = findViewById<RecyclerView>(R.id.peersList)
        peersList.layoutManager = LinearLayoutManager(this)
        val adapter = PeersAdapter { host, port -> ping(host, port) }
        peersList.adapter = adapter

        lifecycleScope.launch {
            discovery.peersFlow.collectLatest { adapter.submit(it.map { p -> p.host to p.port }) }
        }

        val input = findViewById<EditText>(R.id.inputJson)
        val sendBtn = findViewById<Button>(R.id.btnSendJson)
        val output = findViewById<TextView>(R.id.output)
        val pubKeySuffix = findViewById<TextView>(R.id.pubKeySuffix)
        pubKeySuffix.text = com.truth.training.client.core.crypto.Ed25519CryptoManager.getPublicKeyBase64(this).takeLast(8)

        sendBtn.setOnClickListener {
            val list = adapter.items
            if (list.isNotEmpty()) {
                val (host, port) = list.first()
                lifecycleScope.launch(Dispatchers.IO) {
                    val resp = P2PClient.send(this@P2PActivity, host, port, input.text.toString())
                    launch(Dispatchers.Main) { output.text = resp }
                }
            }
        }
    }

    private fun ping(host: String, port: Int) {
        val payload = JSONObject().apply {
            put("action", "ping")
            put("node_id", "android")
        }.toString()
        val output = findViewById<TextView>(R.id.output)
        lifecycleScope.launch(Dispatchers.IO) {
            val resp = P2PClient.send(this@P2PActivity, host, port, payload)
            launch(Dispatchers.Main) { output.text = resp }
        }
    }
}

private class PeersAdapter(private val onClick: (String, Int) -> Unit) : RecyclerView.Adapter<PeerVH>() {
    var items: List<Pair<String, Int>> = emptyList()
        private set
    fun submit(newItems: List<Pair<String, Int>>) { items = newItems; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PeerVH {
        val tv = android.widget.TextView(parent.context)
        tv.setPadding(24, 24, 24, 24)
        return PeerVH(tv, onClick)
    }
    override fun onBindViewHolder(holder: PeerVH, position: Int) { holder.bind(items[position]) }
    override fun getItemCount(): Int = items.size
}

private class PeerVH(private val tv: android.widget.TextView, private val onClick: (String, Int) -> Unit) : RecyclerView.ViewHolder(tv) {
    private var host: String = ""
    private var port: Int = -1
    init {
        tv.setOnClickListener { if (host.isNotBlank() && port > 0) onClick(host, port) }
    }
    fun bind(item: Pair<String, Int>) {
        host = item.first; port = item.second
        tv.text = "$host:$port"
    }
}


