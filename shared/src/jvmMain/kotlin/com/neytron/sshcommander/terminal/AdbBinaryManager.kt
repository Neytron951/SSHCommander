package com.neytron.sshcommander.terminal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipInputStream

object AdbBinaryManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = HttpClient()

    private val _downloadProgress = MutableStateFlow<Float?>(null)

    private val dataDir = File(System.getProperty("user.home"), ".sshcommander/bin")

    fun findAdb(): File? {
        // 1. Check local download
        val adbName = if (isWindows()) "adb.exe" else "adb"
        val localAdb = dataDir.resolve("platform-tools").resolve(adbName)
        if (localAdb.exists()) {
            println("[ADB] Found local binary: ${localAdb.absolutePath}")
            return localAdb
        }

        // 2. Try PATH
        val which = if (isWindows()) "where" else "which"
        val pathProcess = try { ProcessBuilder(which, "adb").start() } catch (e: Exception) { null }
        if (pathProcess?.waitFor() == 0) {
            val path = pathProcess.inputStream.bufferedReader().readLine()?.trim()
            if (path != null && File(path).exists()) {
                println("[ADB] Found system binary via PATH: $path")
                return File(path)
            }
        }

        // 3. Try common Android Home locations
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val file = File(androidHome).resolve("platform-tools").resolve(adbName)
            if (file.exists()) {
                println("[ADB] Found binary in Android Home: ${file.absolutePath}")
                return file
            }
        }

        println("[ADB] ADB binary not found in any location")
        return null
    }

    fun isAdbAvailable(): Boolean = findAdb() != null

    fun startDownload() {
        if (_downloadProgress.value != null) return
        
        scope.launch {
            try {
                _downloadProgress.value = 0f
                val url = getDownloadUrl()
                val response = client.get(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength > 0) {
                            _downloadProgress.value = bytesSentTotal.toFloat() / contentLength
                        }
                    }
                }

                if (!dataDir.exists()) dataDir.mkdirs()
                val zipFile = dataDir.resolve("platform-tools.zip")
                
                val channel = response.bodyAsChannel()
                FileOutputStream(zipFile).use { out ->
                    channel.toInputStream().copyTo(out)
                }

                _downloadProgress.value = 0.95f // Extraction starts
                unzip(zipFile, dataDir)
                zipFile.delete()
                
                // Set executable permission on Unix
                if (!isWindows()) {
                    dataDir.resolve("platform-tools/adb").setExecutable(true)
                }

                _downloadProgress.value = 1f
                delay(1000)
                _downloadProgress.value = null // Reset for next time
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadProgress.value = null
            }
        }
    }

    fun getDownloadProgress(): StateFlow<Float?> = _downloadProgress

    private fun getDownloadUrl(): String {
        val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
        return when {
            os.contains("win") -> "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
            os.contains("mac") -> "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
            else -> "https://dl.google.com/android/repository/platform-tools-latest-linux.zip"
        }
    }

    private fun unzip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val newFile = destDir.resolve(entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { out ->
                        zip.copyTo(out)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name")?.lowercase(Locale.ENGLISH)?.contains("win") == true
    }
}
