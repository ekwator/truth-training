package com.truth.training.client.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class P2PPeer(val serviceName: String, val host: String, val port: Int)

class P2PDiscoveryService(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val SERVICE_TYPE = "_truthnode._tcp."
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registration: NsdServiceInfo? = null
    private val peers = ConcurrentHashMap<String, P2PPeer>()
    private val _peersFlow = MutableStateFlow<List<P2PPeer>>(emptyList())
    val peersFlow: StateFlow<List<P2PPeer>> = _peersFlow

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun registerService(port: Int, instanceName: String) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = instanceName
            serviceType = SERVICE_TYPE
            this.port = port
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) { registration = NsdServiceInfo }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) { registration = null }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        })
    }

    fun startDiscovery() {
        stopDiscovery()
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: return
                        val peer = P2PPeer(resolved.serviceName, host, resolved.port)
                        peers[resolved.serviceName] = peer
                        emitPeers()
                    }
                })
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                peers.remove(serviceInfo.serviceName)
                emitPeers()
            }
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
    }

    fun unregister() {
        registration?.let { runCatching { nsdManager.unregisterService(object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }) } }
        registration = null
    }

    private fun emitPeers() {
        scope.launch(Dispatchers.Main) { _peersFlow.value = peers.values.toList() }
    }
}


