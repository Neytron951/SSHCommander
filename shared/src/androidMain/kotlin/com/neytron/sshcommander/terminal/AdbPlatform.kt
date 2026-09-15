package com.neytron.sshcommander.terminal

import com.neytron.sshcommander.data.DiscoveredDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import com.flyfishxu.kadb.Kadb

actual object AdbPlatform {
    actual suspend fun pair(host: String, port: Int, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        println("[ADB-Android] Starting pure Kotlin pairing with $host:$port using Kadb")
        try {
            Kadb.pair(host, port, code)
            println("[ADB-Android] Pairing successful via Kadb!")
            Result.success(Unit)
        } catch (e: Exception) {
            println("[ADB-Android] Pairing error: ${e.message}")
            Result.failure(e)
        }
    }

    actual fun startPairingServer(name: String, password: String, onDevicePaired: () -> Unit) {
        AdbPairingServer.start(name, password, onDevicePaired)
    }

    actual fun stopPairingServer() {
        AdbPairingServer.stop()
    }

    actual fun isAdbAvailable(): Boolean = true

    actual fun startDownload() {}

    actual fun getDownloadProgress(): StateFlow<Float?> = MutableStateFlow<Float?>(null)

    actual fun scanDevices(): Flow<List<DiscoveredDevice>> = flow {
        println("[ADB-Android] --- DISCOVERY STARTED (Kadb) ---")
        val jmdnsDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
        val jmdnsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        val localIpStrings = mutableSetOf("127.0.0.1", "localhost", "0.0.0.0")
        val allLanAddresses = mutableListOf<InetAddress>()
        try {
            java.net.NetworkInterface.getNetworkInterfaces().asSequence().forEach { ni ->
                if (ni.isUp && !ni.isLoopback) {
                    ni.inetAddresses.asSequence().forEach { addr ->
                        if (addr is java.net.Inet4Address) {
                            localIpStrings.add(addr.hostAddress ?: "")
                            allLanAddresses.add(addr)
                        }
                    }
                }
            }
        } catch (e: Exception) {}

        jmdnsScope.launch {
            allLanAddresses.forEach { lanAddress ->
                try {
                    val jmdns = JmDNS.create(lanAddress)
                    val listener = object : ServiceListener {
                        override fun serviceAdded(event: ServiceEvent) {
                            event.dns.requestServiceInfo(event.type, event.name)
                        }
                        override fun serviceRemoved(event: ServiceEvent) {
                            val host = event.info?.hostAddresses?.firstOrNull() ?: return
                            jmdnsDevices.update { it - host }
                        }
                        override fun serviceResolved(event: ServiceEvent) {
                            val info = event.info
                            val host = info.hostAddresses.firstOrNull() ?: info.inetAddresses.firstOrNull()?.hostAddress ?: return
                            if (localIpStrings.contains(host)) return

                            val port = info.port
                            val rawName = info.name ?: host
                            val friendlyName = rawName.substringBefore(".").substringBefore("_adb")
                            val isPairing = event.type.contains("pairing")
                            
                            jmdnsDevices.update { current ->
                                val existing = current[host]
                                val updated = if (existing == null) {
                                    DiscoveredDevice(name = friendlyName, host = host, port = if (isPairing) 0 else port).apply {
                                        if (isPairing) pairingPort = port
                                    }
                                } else {
                                    if (isPairing) { existing.pairingPort = port; existing }
                                    else { existing.copy(port = port, name = friendlyName) }
                                }
                                current + (host to updated)
                            }
                        }
                    }
                    
                    listOf("_adb-tls-connect._tcp.local.", "_adb-tls-pairing._tcp.local.", "_adb._tcp.local.").forEach {
                        jmdns.addServiceListener(it, listener)
                    }
                    
                    suspendCancellableCoroutine<Unit> { cont ->
                        cont.invokeOnCancellation {
                            try { jmdns.close() } catch (e: Exception) {}
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        try {
            while (currentCoroutineContext().isActive) {
                emit(jmdnsDevices.value.values.toList())
                delay(5000)
            }
        } finally {
            jmdnsScope.cancel()
        }
    }.flowOn(Dispatchers.IO)
}
