package com.neytron.sshcommander.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.*
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpProgressMonitor
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SftpViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ServerRepository(application)
    private val connectionManager = SshConnectionManager(application)
    
    var currentServer by mutableStateOf<Server?>(null)
    var currentPath by mutableStateOf("/")
    val remoteFiles = mutableStateListOf<RemoteFile>()
    var showHiddenFiles by mutableStateOf(false)
    
    var selectedLogin by mutableStateOf<ServerLogin?>(null)
    val logins = mutableStateListOf<ServerLogin>()
    
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    
    // Progress Tracking
    var transferProgress by mutableStateOf(0f)
    var isTransferring by mutableStateOf(false)
    
    private var sftpChannel: ChannelSftp? = null
    val selectedFiles = mutableStateListOf<RemoteFile>()

    fun connect(serverId: Int) {
        viewModelScope.launch {
            val server = repository.getServerById(serverId) ?: return@launch
            currentServer = server
            selectedLogin = null
            remoteFiles.clear()

            // Keep the login list in sync so it's fresh when returning from
            // the "Manage Logins" screen.
            loadLogins(serverId)

            // Load the default login (if explicitly marked).
            repository.getLoginsForServer(serverId).first().let { list ->
                selectedLogin = list.firstOrNull { it.isDefault }
            }
            openSftp()
        }
    }

    private fun loadLogins(serverId: Int) {
        viewModelScope.launch {
            repository.getLoginsForServer(serverId).collect { list ->
                val previous = selectedLogin
                logins.clear()
                logins.addAll(list)
                // If the selected login was removed, fall back to the default or null
                if (previous != null && list.none { it.id == previous.id }) {
                    selectedLogin = list.firstOrNull { it.isDefault }
                }
            }
        }
    }

    /**
     * Switches the active login and reconnects the SFTP session.
     */
    fun selectLogin(login: ServerLogin?) {
        val server = currentServer ?: return
        selectedLogin = login
        sftpChannel?.disconnect()
        sftpChannel = null
        openSftp()
    }

    private fun openSftp() {
        val server = currentServer ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    isLoading = true
                    errorMessage = null

                    val profile = repository.buildConnectionProfile(server, selectedLogin)
                    // SHARED SESSION: Use the shared pool to avoid kicking out the Terminal session
                    val session = connectionManager.getOrCreateSession(server, profile)
                    
                    // Close old channel if it exists
                    if (sftpChannel?.isConnected == true) {
                        sftpChannel?.disconnect()
                    }
                    
                    val channel = session.openChannel("sftp") as ChannelSftp
                    channel.connect()
                    
                    sftpChannel = channel
                    val startPath = resolveStartPath(server)
                    loadDirectory(startPath)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { 
                        errorMessage = getApplication<Application>().getString(R.string.sftp_error_prefix, e.localizedMessage ?: "")
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    /**
     * Determines which folder to open first in SFTP:
     * 1. The start path configured on the login.
     * 2. The start path configured on the server.
     * 3. The last visited folder (remembered automatically).
     * 4. The login home directory (default).
     */
    private fun resolveStartPath(server: Server): String {
        selectedLogin?.sftpStartPath?.takeIf { it.isNotBlank() }?.let { return it }
        server.sftpStartPath?.takeIf { it.isNotBlank() }?.let { return it }
        (selectedLogin?.lastSftpPath ?: server.lastSftpPath)?.takeIf { it.isNotBlank() }?.let { return it }
        return "/"
    }

    /**
     * Remembers the last visited folder so the next SFTP session starts there.
     */
    private fun rememberLastPath(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val server = currentServer ?: return@launch
            val login = selectedLogin
            if (login != null) {
                repository.updateLoginLastSftpPath(login.id, path)
            } else {
                repository.updateLastSftpPath(server.id, path)
            }
        }
    }

    /**
     * Ensures we are connected before performing any operation. 
     * This fixes the "crash" after minimizing the app.
     */
    private suspend fun ensureConnected(): Boolean {
        val server = currentServer ?: return false
        if (sftpChannel?.isConnected == true) return true
        
        return try {
            val profile = repository.buildConnectionProfile(server, selectedLogin)
            val session = connectionManager.getOrCreateSession(server, profile)
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            sftpChannel = channel
            true
        } catch (e: Exception) {
            errorMessage = getApplication<Application>().getString(R.string.err_connection_lost)
            false
        }
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            try {
                isLoading = true
                channel.cd(path)
                val pwd = channel.pwd()
                val vector = channel.ls(pwd)
                val files = mutableListOf<RemoteFile>()
                
                for (obj in vector) {
                    val entry = obj as ChannelSftp.LsEntry
                    if (entry.filename == "." || entry.filename == "..") continue
                    if (!showHiddenFiles && entry.filename.startsWith(".")) continue
                    
                    files.add(RemoteFile(
                        name = entry.filename,
                        path = if (pwd.endsWith("/")) pwd + entry.filename else "$pwd/${entry.filename}",
                        isDirectory = entry.attrs.isDir,
                        size = entry.attrs.size,
                        permissions = entry.attrs.permissionsString,
                        modifiedTime = entry.attrs.mTime.toLong() * 1000
                    ))
                }
                
                files.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                
                withContext(Dispatchers.Main) {
                    remoteFiles.clear()
                    remoteFiles.addAll(files)
                    currentPath = pwd
                    selectedFiles.clear()
                }
                rememberLastPath(pwd)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.localizedMessage }
            } finally {
                isLoading = false
            }
        }
    }

    fun downloadFile(file: RemoteFile, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            try {
                isTransferring = true
                transferProgress = 0f
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                    channel.get(file.path, output, createProgressMonitor(file.size))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.localizedMessage }
            } finally {
                isTransferring = false
            }
        }
    }

    fun downloadSelected(treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            val parentDir = DocumentFile.fromTreeUri(getApplication(), treeUri) ?: return@launch
            
            try {
                isTransferring = true
                val totalSize = selectedFiles.sumOf { it.size }
                var currentDownloaded: Long = 0
                
                selectedFiles.forEach { file ->
                    if (file.isDirectory) return@forEach // Basic impl: files only for now
                    
                    val newFile = parentDir.createFile("*/*", file.name) ?: return@forEach
                    getApplication<Application>().contentResolver.openOutputStream(newFile.uri)?.use { output ->
                        channel.get(file.path, output, object : SftpProgressMonitor {
                            override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                            override fun count(count: Long): Boolean {
                                currentDownloaded += count
                                if (totalSize > 0) transferProgress = currentDownloaded.toFloat() / totalSize
                                return true
                            }
                            override fun end() {}
                        })
                    }
                }
                withContext(Dispatchers.Main) { selectedFiles.clear() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.localizedMessage }
            } finally {
                isTransferring = false
                transferProgress = 0f
            }
        }
    }

    fun uploadFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            try {
                isTransferring = true
                transferProgress = 0f
                val fileName = getFileNameFromUri(uri) ?: "upload"
                val size = getFileSizeFromUri(uri)
                
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    channel.put(input, fileName, createProgressMonitor(size))
                }
                loadDirectory(currentPath)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.localizedMessage }
            } finally {
                isTransferring = false
            }
        }
    }

    private fun createProgressMonitor(max: Long): SftpProgressMonitor = object : SftpProgressMonitor {
        private var current: Long = 0
        override fun init(op: Int, src: String?, dest: String?, max: Long) {}
        override fun count(count: Long): Boolean {
            current += count
            if (max > 0) transferProgress = current.toFloat() / max
            return true
        }
        override fun end() { transferProgress = 1f }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = it.getString(index)
            }
        }
        return name ?: uri.path?.substringAfterLast('/')
    }

    private fun getFileSizeFromUri(uri: Uri): Long {
        var size: Long = 0
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (index != -1) size = it.getLong(index)
            }
        }
        return size
    }

    fun navigateUp() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            try {
                channel.cd("..")
                loadDirectory(channel.pwd())
            } catch (e: Exception) {}
        }
    }

    fun toggleSelection(file: RemoteFile) {
        if (selectedFiles.any { it.path == file.path }) {
            selectedFiles.removeAll { it.path == file.path }
        } else {
            selectedFiles.add(file)
        }
    }

    fun toggleHiddenFiles() {
        showHiddenFiles = !showHiddenFiles
        loadDirectory(currentPath)
    }

    fun deleteFile(file: RemoteFile) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            try {
                isLoading = true
                if (file.isDirectory) channel.rmdir(file.path)
                else channel.rm(file.path)
                loadDirectory(currentPath)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.localizedMessage }
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            try {
                isLoading = true
                selectedFiles.forEach { file ->
                    if (file.isDirectory) channel.rmdir(file.path)
                    else channel.rm(file.path)
                }
                loadDirectory(currentPath)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.localizedMessage }
            } finally {
                isLoading = false
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            try {
                isLoading = true
                val cleanName = name.trim()
                if (cleanName.isNotEmpty()) {
                    val target = if (currentPath == "/") "/$cleanName" else "$currentPath/$cleanName"
                    channel.mkdir(target)
                    loadDirectory(currentPath)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.localizedMessage }
            } finally {
                isLoading = false
            }
        }
    }

    fun renameFile(file: RemoteFile, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            val channel = sftpChannel ?: return@launch
            try {
                isLoading = true
                val cleanName = newName.trim()
                if (cleanName.isNotEmpty()) {
                    val newPath = file.path.substringBeforeLast('/', "").let { parent ->
                        if (parent.isEmpty()) "/$cleanName" else "$parent/$cleanName"
                    }
                    channel.rename(file.path, newPath)
                    loadDirectory(currentPath)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.localizedMessage }
            } finally {
                isLoading = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // PERSISTENCE FIX: We no longer disconnect the whole session here.
        // We only close the channel so that other parts of the app (like Terminal)
        // or background tasks can still use the underlying connection.
        viewModelScope.launch(Dispatchers.IO) {
            sftpChannel?.disconnect()
        }
    }
}
