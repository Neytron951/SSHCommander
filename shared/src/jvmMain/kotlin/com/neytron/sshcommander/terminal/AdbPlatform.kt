package com.neytron.sshcommander.terminal

import com.neytron.sshcommander.data.DiscoveredDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.net.InetAddress
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import org.conscrypt.Conscrypt

actual object AdbPlatform {
    init {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val vm = System.getProperty("java.vm.name")?.lowercase() ?: ""
        val runtimeName = System.getProperty("java.runtime.name")?.lowercase() ?: ""
        if (os.contains("android") || vm.contains("dalvik") || runtimeName.contains("android")) {
            println("[ADB] Running on Android — skipping Conscrypt initialization")
        } else {
            try {
                java.security.Security.insertProviderAt(Conscrypt.newProvider(), 1)
                println("[ADB] Conscrypt provider initialized")
            } catch (e: Exception) {
                println("[ADB] Conscrypt initialization failed: ${e.message}")
            }
        }
    }
    actual suspend fun pair(host: String, port: Int, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        println("[ADB] Starting manual pairing with $host:$port using code $code")
        try {
            val adb = AdbBinaryManager.findAdb() ?: throw Exception("ADB binary not found.")
            val output = executeAdbCommand(adb, listOf("pair", "$host:$port", code), 10000)
            
            if (output.contains("Successfully paired")) {
                println("[ADB] Pairing successful!")
                Result.success(Unit)
            } else {
                println("[ADB] Pairing failed: $output")
                Result.failure(Exception("Pairing failed: $output"))
            }
        } catch (e: Exception) {
            println("[ADB] Pairing error: ${e.message}")
            Result.failure(e)
        }
    }

    actual fun startPairingServer(name: String, password: String, onDevicePaired: () -> Unit) {
        AdbPairingServer.start(name, password, onDevicePaired)
    }

    actual fun stopPairingServer() {
        AdbPairingServer.stop()
    }

    actual fun isAdbAvailable(): Boolean = AdbBinaryManager.isAdbAvailable()

    actual fun startDownload() = AdbBinaryManager.startDownload()

    actual fun getDownloadProgress(): StateFlow<Float?> = AdbBinaryManager.getDownloadProgress()

    actual fun scanDevices(): Flow<List<DiscoveredDevice>> = flow {
        val adb = AdbBinaryManager.findAdb() ?: run {
            println("[ADB] Scan failed: binary not found")
            return@flow
        }
        
        println("[ADB] --- DISCOVERY STARTED ---")
        
        try {
            executeAdbCommand(adb, listOf("start-server"), 5000)
        } catch (e: Exception) {}

        val jmdnsDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
        val jmdnsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        val localIpStrings = mutableSetOf<String>("127.0.0.1", "localhost", "0.0.0.0")
        val allLanAddresses = mutableListOf<InetAddress>()
        try {
            java.net.NetworkInterface.getNetworkInterfaces().asSequence().forEach { ni ->
                if (ni.isUp) {
                    ni.inetAddresses.asSequence().forEach { addr ->
                        if (addr is java.net.Inet4Address) {
                            localIpStrings.add(addr.hostAddress)
                            if (!ni.isLoopback) allLanAddresses.add(addr)
                        }
                    }
                }
            }
        } catch (e: Exception) {}

        jmdnsScope.launch {
            allLanAddresses.forEach { lanAddress ->
                try {
                    println("[ADB] JmDNS starting on: ${lanAddress.hostAddress}")
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
                            
                            println("[ADB] JmDNS RESOLVED: $friendlyName at $host:$port (pairing=$isPairing)")
                            
                            jmdnsDevices.update { current ->
                                val existing = current[host]
                                val updated = if (existing == null) {
                                    DiscoveredDevice(
                                        name = friendlyName, 
                                        host = host, 
                                        port = if (isPairing) 0 else port
                                    ).apply {
                                        if (isPairing) pairingPort = port
                                    }
                                } else {
                                    if (isPairing) {
                                        existing.pairingPort = port
                                        existing
                                    } else {
                                        existing.copy(port = port, name = friendlyName)
                                    }
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
                } catch (e: Exception) {
                    println("[ADB] JmDNS error on ${lanAddress.hostAddress}: ${e.message}")
                }
            }
        }

        try {
            while (currentCoroutineContext().isActive) {
                val deviceMap = mutableMapOf<String, DiscoveredDevice>()
                deviceMap.putAll(jmdnsDevices.value)

                // 1. adb devices -l (USB or manually connected)
                val devicesOutput = executeAdbCommand(adb, listOf("devices", "-l"), 2000)
                devicesOutput.lines().forEach { line ->
                    if (line.startsWith("List of") || line.isBlank()) return@forEach
                    val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (parts.size >= 2) {
                        val serial = parts[0]
                        val model = parts.find { it.startsWith("model:") }?.substringAfter(":") ?: serial
                        
                        if (serial.contains(".")) { // TCP Device
                            val host = serial.substringBefore(":")
                            if (localIpStrings.contains(host)) return@forEach

                            val port = serial.substringAfter(":", "5555").toIntOrNull() ?: 5555
                            val existing = deviceMap[host]
                            if (existing == null) {
                                deviceMap[host] = DiscoveredDevice(name = model, host = host, port = port)
                            } else {
                                deviceMap[host] = existing.copy(name = model, port = port)
                            }
                        } else { // USB Device
                            deviceMap[serial] = DiscoveredDevice(name = "$model (USB)", host = serial, port = 5555)
                        }
                    }
                }

                // 2. adb mdns services (Smart Parser for random ports)
                val mdnsOutput = executeAdbCommand(adb, listOf("mdns", "services"), 3000)
                if (mdnsOutput.isNotBlank()) {
                    mdnsOutput.lines().forEach { line ->
                        if (!line.contains("_adb")) return@forEach
                        val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                        
                        val addressPart = parts.find { it.contains(".") && it.contains(":") }
                        val host = addressPart?.substringBefore(":") 
                            ?: parts.find { it.count { c -> c == '.' } == 3 }
                        
                        val port = addressPart?.substringAfterLast(":")?.toIntOrNull()
                            ?: parts.find { it.toIntOrNull() != null && it.length >= 4 }?.toIntOrNull()

                        if (host != null && port != null && !localIpStrings.contains(host)) {
                            val isPairing = line.contains("pairing")
                            val existing = deviceMap[host]
                            if (existing == null) {
                                deviceMap[host] = DiscoveredDevice(
                                    name = "Android Device", 
                                    host = host, 
                                    port = if (isPairing) 0 else port
                                ).apply {
                                    if (isPairing) pairingPort = port
                                }
                            } else {
                                if (isPairing) existing.pairingPort = port
                                else if (existing.port == 0 || existing.port == 5555) {
                                    deviceMap[host] = existing.copy(port = port)
                                }
                            }
                        }
                    }
                }

                // Final cleanup: if a device has port 0 (only pairing found), set it to 5555 for display 
                val resultList = deviceMap.values.map { 
                    if (it.port == 0) it.copy(port = 5555) else it 
                }.toList()

                emit(resultList)
                delay(5000)
            }
        } finally {
            jmdnsScope.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun executeAdbCommand(adb: File, args: List<String>, timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val cmd = args.joinToString(" ")
        try {
            val pb = ProcessBuilder(listOf(adb.absolutePath) + args).redirectErrorStream(true)
            pb.environment()["ADB_MDNS_OPENSCREEN"] = "1"
            val process = pb.start()
            
            val output = StringBuilder()
            val readerJob = launch(Dispatchers.IO) {
                try {
                    val stream = process.inputStream
                    val buffer = ByteArray(1024)
                    var count: Int
                    while (isActive) {
                        count = stream.read(buffer)
                        if (count == -1) break
                        val text = String(buffer, 0, count)
                        output.append(text)
                    }
                } catch (e: Exception) {}
            }
            
            val exitCode = withTimeoutOrNull(timeoutMs) {
                while (true) {
                    try {
                        return@withTimeoutOrNull process.exitValue()
                    } catch (e: IllegalThreadStateException) {
                        delay(100)
                    }
                }
            }
            
            if (exitCode == null) {
                println("[ADB] 'adb $cmd' TIMED OUT")
                process.destroy()
            }
            
            readerJob.cancelAndJoin()
            val result = output.toString().trim()
            if (result.isNotEmpty()) {
                println("[ADB] 'adb $cmd' (Exit: $exitCode) output:\n$result")
            }
            result
        } catch (e: Exception) {
            println("[ADB] Command 'adb $cmd' failed: ${e.message}")
            ""
        }
    }
}
