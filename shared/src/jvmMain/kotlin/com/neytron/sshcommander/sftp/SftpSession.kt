package com.neytron.sshcommander.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpATTRS
import com.neytron.sshcommander.data.ConnectionProfile
import com.neytron.sshcommander.data.HostKeyStore
import com.neytron.sshcommander.data.RemoteFile
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.SshConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

    override fun listDirectory(path: String) {
        scope.launch(Dispatchers.IO) {
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
                    val attrs: SftpATTRS = ce.attrs
                    val rf = RemoteFile(
                        name = name,
                        path = "$norm/$name",
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
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to list directory"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun goUp() {
        val current = _currentPath.value
        if (current == "/") return
        val parent = current.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { "/" }
        listDirectory(parent)
    }

    override fun goTo(path: String) = listDirectory(path)

    override suspend fun download(remoteDir: String, remoteFile: RemoteFile, destinationDirPath: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val channel = currentChannel ?: return@withContext false
                if (remoteFile.isDirectory) return@withContext false
                val outFile = File(destinationDirPath, remoteFile.name)
                channel.get("$remoteDir/${remoteFile.name}", outFile.absolutePath)
                true
            } catch (e: Exception) {
                _error.value = e.message ?: "Download failed"
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

    override suspend fun delete(remoteDir: String, remoteFile: RemoteFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val channel = currentChannel ?: return@withContext false
            val path = "$remoteDir/${remoteFile.name}"
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

    override fun close() {
        currentChannel?.disconnect()
        currentChannel = null
        _isConnected.value = false
        scope.cancel()
    }

    private fun normalize(path: String): String {
        val cleaned = path.trim().replace('\\', '/')
        return if (cleaned.startsWith('/')) cleaned else "/$cleaned"
    }
}
