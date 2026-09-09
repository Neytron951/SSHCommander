package com.neytron.sshcommander.terminal

import com.neytron.sshcommander.data.DiscoveredDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

actual object AdbPlatform {
    actual suspend fun pair(host: String, port: Int, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val adb = AdbBinaryManager.findAdb() ?: throw Exception("ADB binary not found.")
            val process = ProcessBuilder(adb.absolutePath, "pair", "$host:$port", code)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0 || output.contains("Successfully paired")) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pairing failed: $output"))
            }
        } catch (e: Exception) {
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
        
        println("[ADB] Starting scan using: ${adb.absolutePath}")
        
        // Ensure server is running and mDNS is active
        try {
            val startProcess = ProcessBuilder(adb.absolutePath, "start-server").start()
            startProcess.waitFor()
            println("[ADB] Server started/checked")
        } catch (e: Exception) {
            println("[ADB] Failed to start server: ${e.message}")
        }

        while (true) {
            try {
                println("[ADB] Executing 'adb mdns services'...")
                val process = ProcessBuilder(adb.absolutePath, "mdns", "services")
                    .redirectErrorStream(true)
                    .start()
                
                val output = withContext(Dispatchers.IO) {
                    try {
                        val reader = process.inputStream.bufferedReader()
                        val sb = StringBuilder()
                        val startTime = System.currentTimeMillis()
                        // Read line by line with a deadline, as 'mdns services' might not exit
                        while (System.currentTimeMillis() - startTime < 3000) {
                            if (reader.ready()) {
                                val line = reader.readLine() ?: break
                                sb.append(line).append("\n")
                                println("[ADB] Discovery line: $line")
                            } else {
                                delay(100)
                            }
                        }
                        sb.toString()
                    } catch (e: Exception) {
                        println("[ADB] Error reading mdns output: ${e.message}")
                        ""
                    } finally {
                        val isAlive = try { process.exitValue(); false } catch (ex: IllegalThreadStateException) { true }
                        if (isAlive) process.destroy()
                    }
                }
                
                println("[ADB] Raw output size: ${output.length} chars")
                if (output.isNotEmpty()) {
                    println("[ADB] Raw output start: ${output.take(100).replace("\n", " ")}")
                }

                val deviceMap = mutableMapOf<String, DiscoveredDevice>()
                
                output.lines().map { it.trim() }.forEach { line ->
                    if (line.isEmpty() || line.startsWith("List of")) return@forEach
                    
                    val isPairing = line.contains("_adb-tls-pairing")
                    val isConnect = line.contains("_adb-tls-connect") || line.contains("_adb._tcp")
                    
                    if (isPairing || isConnect) {
                        val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                        if (parts.size >= 2) {
                            // The service name is usually the first part
                            val rawName = parts[0].substringBefore("._adb").trim('[', ']', '.')
                            
                            // The address part (IP:PORT) can be in parts[1] or parts[2] depending on adb version
                            val addressPart = parts.find { it.contains(":") && it.any { c -> c.isDigit() } }
                            
                            if (addressPart != null) {
                                val host = addressPart.substringBeforeLast(":")
                                val port = addressPart.substringAfterLast(":").toIntOrNull() ?: 5555
                                
                                println("[ADB] Found service: $rawName at $host:$port (pairing=$isPairing)")

                                val existing = deviceMap[host]
                                if (existing == null) {
                                    deviceMap[host] = DiscoveredDevice(
                                        name = rawName.ifEmpty { host },
                                        host = host,
                                        port = if (isConnect) port else 5555
                                    ).apply {
                                        if (isPairing) pairingPort = port
                                    }
                                } else {
                                    if (isConnect) deviceMap[host] = existing.copy(port = port)
                                    if (isPairing) existing.pairingPort = port
                                    // If we have a generic adb-ID name, try to replace it with a better one if found
                                    if (existing.name.startsWith("adb-") && !rawName.startsWith("adb-") && rawName.isNotEmpty()) {
                                        deviceMap[host] = deviceMap[host]!!.copy(name = rawName)
                                    }
                                }
                            }
                        }
                    }
                }
                
                val results = deviceMap.values.toList()
                println("[ADB] Emitting ${results.size} devices")
                emit(results)
            } catch (e: Exception) {
                println("[ADB] Critical error in scan loop: ${e.message}")
                e.printStackTrace()
                emit(emptyList())
            }
            delay(3000)
        }
    }.flowOn(Dispatchers.IO)
}
