package com.neytron.sshcommander.terminal

import java.net.ServerSocket
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlinx.coroutines.*
import java.net.InetAddress
import java.util.concurrent.Executors

object AdbPairingServer {
    private var jmdns: JmDNS? = null
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    private var avahiProcess: Process? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(name: String, password: String, onDevicePaired: () -> Unit) {
        println("[ADB-Pair] start() called for $name")
        stop("Starting new session")
        
        System.setProperty("java.net.preferIPv4Stack", "true")
        
        job = scope.launch {
            try {
                // 1. Find LAN IP
                val allAddresses = java.net.NetworkInterface.getNetworkInterfaces().asSequence()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.asSequence() }
                    .filter { it is java.net.Inet4Address && !it.isLoopbackAddress }
                    .toList()
                
                val lanAddress = allAddresses.find { it.hostAddress.startsWith("192.168.") }
                    ?: allAddresses.find { it.hostAddress.startsWith("10.") && !it.hostAddress.startsWith("10.8.") }
                    ?: allAddresses.firstOrNull() ?: InetAddress.getLocalHost()

                println("[ADB-Pair] Selected interface: ${lanAddress.hostAddress}")

                // 2. Open port
                val socket = ServerSocket(0)
                serverSocket = socket
                val port = socket.localPort
                println("[ADB-Pair] Server socket opened on port $port")

                // 3. Register mDNS
                try {
                    val dns = JmDNS.create(lanAddress)
                    jmdns = dns
                    val serviceType = "_adb-tls-pairing._tcp.local."
                    val serviceInfo = ServiceInfo.create(serviceType, name, port, 0, 0, true, mapOf("txtvers" to "1"))
                    dns.registerService(serviceInfo)
                    println("[ADB-Pair] JmDNS service registered: $name")
                } catch (e: Exception) {
                    println("[ADB-Pair] JmDNS registration failed: ${e.message}")
                }

                // Linux Fallback: avahi-publish
                if (System.getProperty("os.name").lowercase().contains("linux")) {
                    try {
                        val process = ProcessBuilder("avahi-publish", "-s", name, "_adb-tls-pairing._tcp", port.toString(), "txtvers=1").start()
                        avahiProcess = process
                        println("[ADB-Pair] Avahi-publish started (PID: ${try { process.pid() } catch(e: Exception) { "unknown" }})")
                        scope.launch {
                            val exit = process.waitFor()
                            if (isActive) println("[ADB-Pair] Avahi-publish terminated with exit code $exit")
                        }
                    } catch (e: Exception) {
                        println("[ADB-Pair] Avahi-publish not available")
                    }
                }

                // 4. Wait for connection
                socket.soTimeout = 1000
                while (isActive) {
                    val client = try { 
                        socket.accept() 
                    } catch (e: java.net.SocketTimeoutException) {
                        continue
                    } catch (e: Exception) {
                        if (isActive) println("[ADB-Pair] Socket accept error: ${e.message}")
                        break
                    }
                    
                    println("[ADB-Pair] !!! CONNECTION RECEIVED FROM ${client.inetAddress.hostAddress} !!!")
                    withContext(Dispatchers.Main) { onDevicePaired() }
                    delay(2000)
                    client.close()
                }
            } catch (e: Exception) {
                if (isActive) println("[ADB-Pair] Error in pairing loop: ${e.message}")
            } finally {
                println("[ADB-Pair] Pairing loop exited")
            }
        }
    }

    fun stop(reason: String = "External request") {
        val currentJob = job
        val currentDns = jmdns
        val currentSocket = serverSocket
        val currentAvahi = avahiProcess
        
        if (currentJob == null && currentDns == null && currentSocket == null && currentAvahi == null) return

        println("[ADB-Pair] stop() called. Reason: $reason")
        
        job = null
        jmdns = null
        serverSocket = null
        avahiProcess = null

        scope.launch(Dispatchers.IO) {
            currentJob?.cancelAndJoin()
            currentAvahi?.destroy()
            try {
                currentDns?.unregisterAllServices()
                currentDns?.close()
            } catch (e: Exception) {}
            try {
                currentSocket?.close()
            } catch (e: Exception) {}
            println("[ADB-Pair] Server stopped and cleaned up ($reason)")
        }
    }
}
