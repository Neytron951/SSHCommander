package com.neytron.sshcommander.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpATTRS
import com.jcraft.jsch.SftpProgressMonitor
import com.neytron.sshcommander.data.ConnectionProfile
import com.neytron.sshcommander.data.HostKeyStore
import com.neytron.sshcommander.data.RemoteFile
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.SshConnectionManager
import com.neytron.sshcommander.ui.PlatformInputStream
import com.neytron.sshcommander.ui.PlatformOutputStream
import com.neytron.sshcommander.ui.PlatformTransferFile
import com.neytron.sshcommander.ui.PlatformTransferDir
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Vector

/**
 * SFTP channel backed by JSch. Reuses the shared SSH session from
 * [SshConnectionManager] so the terminal and SFTP share one connection.
 */
class SftpSession(
    private val server: Server,
    private val profile: ConnectionProfile,
    private val hostKeyStore: HostKeyStore? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : SftpController {

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error

    private val _currentPath = MutableStateFlow("/")
    override val currentPath: StateFlow<String> = _currentPath

    private val _files = MutableStateFlow<List<RemoteFile>>(emptyList())
    override val files: StateFlow<List<RemoteFile>> = _files

    private val _showHiddenFiles = MutableStateFlow(false)
    override val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    override val selectedFiles: StateFlow<Set<String>> = _selectedFiles

    private val _transferProgress = MutableStateFlow(0f)
    override val transferProgress: StateFlow<Float> = _transferProgress

    private val _isTransferring = MutableStateFlow(false)
    override val isTransferring: StateFlow<Boolean> = _isTransferring

    private val connectionManager = SshConnectionManager(hostKeyStore)
    private var currentChannel: ChannelSftp? = null

    override fun connect() {
        scope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                val session: Session = connectionManager.getOrCreateSession(server, profile)
                if (currentChannel?.isConnected == true) currentChannel?.disconnect()
                val channel = session.openChannel("sftp") as ChannelSftp
                channel.connect()
                currentChannel = channel
                _isConnected.value = true
                val startPath = server.sftpStartPath?.takeIf { it.isNotBlank() } ?: "/"
                listDirectory(startPath)
            } catch (e: Exception) {
                _error.value = e.message ?: "SFTP connection failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override suspend fun listDirectory(path: String) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _error.value = null
        try {
            val channel = currentChannel ?: throw IllegalStateException("Not connected")
            val norm = normalize(path)
            val entries = channel.ls(norm)
            val list = mutableListOf<RemoteFile>()
            val dirs = mutableListOf<RemoteFile>()
            val filesOut = mutableListOf<RemoteFile>()
            val e = entries as Vector<*>
            for (item in e) {
                val ce = item as? com.jcraft.jsch.ChannelSftp.LsEntry ?: continue
                val name = ce.filename
                if (name == "." || name == "..") continue
                if (!_showHiddenFiles.value && name.startsWith(".")) continue
                val attrs: SftpATTRS = ce.attrs
                val rf = RemoteFile(
                    name = name,
                    path = if (norm.endsWith("/")) "$norm$name" else "$norm/$name",
                    isDirectory = attrs.isDir,
                    size = attrs.size,
                    permissions = attrs.permissionsString,
                    modifiedTime = attrs.mTime * 1000L
                )
                if (rf.isDirectory) dirs.add(rf) else filesOut.add(rf)
            }
            dirs.sortBy { it.name.lowercase() }
            filesOut.sortBy { it.name.lowercase() }
            list.addAll(dirs)
            list.addAll(filesOut)
            _currentPath.value = norm
            _files.value = list
            _selectedFiles.value = emptySet()
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to list directory"
        } finally {
            _isLoading.value = false
        }
    }

    override fun toggleHiddenFiles() {
        _showHiddenFiles.value = !_showHiddenFiles.value
        scope.launch { listDirectory(_currentPath.value) }
    }

    override fun toggleSelection(path: String) {
        val current = _selectedFiles.value
        _selectedFiles.value = if (path in current) current - path else current + path
    }

    override fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    override suspend fun listFiles(path: String): List<RemoteFile> = withContext(Dispatchers.IO) {
        val channel = currentChannel ?: throw IllegalStateException("Not connected")
        val norm = normalize(path)
        val entries = channel.ls(norm)
        val list = mutableListOf<RemoteFile>()
        val e = entries as Vector<*>
        for (item in e) {
            val ce = item as? com.jcraft.jsch.ChannelSftp.LsEntry ?: continue
            val name = ce.filename
            if (name == "." || name == "..") continue
            val attrs: SftpATTRS = ce.attrs
            list.add(RemoteFile(
                name = name,
                path = if (norm.endsWith("/")) "$norm$name" else "$norm/$name",
                isDirectory = attrs.isDir,
                size = attrs.size,
                permissions = attrs.permissionsString,
                modifiedTime = attrs.mTime * 1000L
            ))
        }
        list
    }

    override fun goUp() {
        val current = _currentPath.value
        if (current == "/") return
        val parent = current.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { "/" }
        scope.launch { listDirectory(parent) }
    }

    override fun goTo(path: String) {
        scope.launch { listDirectory(path) }
    }

    override suspend fun download(remoteDir: String, remoteFile: RemoteFile, target: PlatformTransferFile): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val channel = currentChannel ?: return@withContext false
                if (remoteFile.isDirectory) return@withContext false
                _isTransferring.value = true
                _transferProgress.value = 0f
                target.openOutput()?.let { output ->
                    val out = JavaOutputStreamAdapter(output)
                    try {
                        channel.get("$remoteDir/${remoteFile.name}", out, createProgressMonitor(remoteFile.size))
                        true
                    } finally { 
                        out.close()
                        _isTransferring.value = false
                    }
                } ?: false
            } catch (e: Exception) {
                _error.value = e.message ?: "Download failed"
                _isTransferring.value = false
                false
            }
        }

    override suspend fun downloadToDir(remoteDir: String, remoteFile: RemoteFile, targetDir: PlatformTransferDir): Boolean =
        withContext(Dispatchers.IO) {
            val targetFile = targetDir.createFile(remoteFile.name) ?: return@withContext false
            download(remoteDir, remoteFile, targetFile)
        }

    override suspend fun readRemoteFile(remotePath: String, maxBytes: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val channel = currentChannel ?: return@withContext null
                val input = channel.get(normalize(remotePath))
                val buffer = ByteArray(maxBytes)
                val total = input.read(buffer)
                input.close()
                if (total <= 0) null else buffer.copyOf(total)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to read file"
                null
            }
        }

    override suspend fun upload(files: List<PlatformTransferFile>, remoteDir: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val channel = currentChannel ?: return@withContext false
            _isTransferring.value = true
            var overallSuccess = true
            files.forEach { file ->
                val input = file.openInput() ?: run { overallSuccess = false; return@forEach }
                val inStream = JavaInputStreamAdapter(input)
                try {
                    _transferProgress.value = 0f
                    channel.put(inStream, "$remoteDir/${file.name}", createProgressMonitor(file.size))
                } catch (e: Exception) {
                    overallSuccess = false
                } finally { inStream.close() }
            }
            listDirectory(remoteDir)
            _isTransferring.value = false
            overallSuccess
        } catch (e: Exception) {
            _error.value = e.message ?: "Upload failed"
            _isTransferring.value = false
            false
        }
    }

    override suspend fun upload(localFilePath: String, remoteDir: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val channel = currentChannel ?: return@withContext false
            val localFile = File(localFilePath)
            channel.put(localFile.absolutePath, "$remoteDir/${localFile.name}")
            listDirectory(remoteDir)
            true
        } catch (e: Exception) {
            _error.value = e.message ?: "Upload failed"
            false
        }
    }

    override suspend fun makeDirectory(parentDir: String, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val channel = currentChannel ?: return@withContext false
            channel.mkdir("$parentDir/$name")
            listDirectory(parentDir)
            true
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to create directory"
            false
        }
    }

    override suspend fun rename(remotePath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val channel = currentChannel ?: return@withContext false
            val parent = remotePath.substringBeforeLast('/', "").ifEmpty { "/" }
            val newPath = if (parent.endsWith("/")) "$parent$newName" else "$parent/$newName"
            channel.rename(remotePath, newPath)
            listDirectory(parent)
            true
        } catch (e: Exception) {
            _error.value = e.message ?: "Rename failed"
            false
        }
    }

    override suspend fun delete(remoteDir: String, remoteFile: RemoteFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val channel = currentChannel ?: return@withContext false
            val path = if (remoteDir.endsWith("/")) "$remoteDir${remoteFile.name}" else "$remoteDir/${remoteFile.name}"
            if (remoteFile.isDirectory) {
                channel.rmdir(path)
            } else {
                channel.rm(path)
            }
            listDirectory(remoteDir)
            true
        } catch (e: Exception) {
            _error.value = e.message ?: "Delete failed"
            false
        }
    }

    override suspend fun readRemoteText(remotePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val channel = currentChannel ?: return@withContext null
            val input = channel.get(normalize(remotePath))
            val text = input.bufferedReader().readText()
            input.close()
            text
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun writeRemoteText(remotePath: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val channel = currentChannel ?: return@withContext false
            val output = channel.put(normalize(remotePath))
            output.write(text.toByteArray())
            output.flush()
            output.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun close() {
        currentChannel?.disconnect()
        currentChannel = null
        _isConnected.value = false
        scope.cancel()
    }

    private fun normalize(path: String): String {
        val cleaned = path.trim().replace('\\', '/')
        val single = cleaned.replace(Regex("/+"), "/")
        return if (single.startsWith('/')) single else "/$single"
    }

    private fun createProgressMonitor(max: Long): SftpProgressMonitor = object : SftpProgressMonitor {
        private var current: Long = 0
        override fun init(op: Int, src: String?, dest: String?, max: Long) {}
        override fun count(count: Long): Boolean {
            current += count
            if (max > 0) _transferProgress.value = current.toFloat() / max
            return true
        }
        override fun end() { _transferProgress.value = 1f }
    }
}

private class JavaInputStreamAdapter(private val platform: PlatformInputStream) : InputStream() {
    override fun read(): Int {
        val buffer = ByteArray(1)
        val n = platform.read(buffer, 0, 1)
        return if (n <= 0) -1 else buffer[0].toInt() and 0xFF
    }
    override fun read(b: ByteArray, off: Int, len: Int): Int = platform.read(b, off, len)
    override fun close() = platform.close()
}

private class JavaOutputStreamAdapter(private val platform: PlatformOutputStream) : OutputStream() {
    override fun write(b: Int) {
        val buffer = byteArrayOf(b.toByte())
        platform.write(buffer, 0, 1)
    }
    override fun write(b: ByteArray, off: Int, len: Int) = platform.write(b, off, len)
    override fun close() = platform.close()
}
