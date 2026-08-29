package com.neytron.sshcommander.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.data.*
import com.neytron.sshcommander.sftp.SftpController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SftpViewModel(private val repository: ServerRepository) : ViewModel(), SftpController {
    var currentServer by mutableStateOf<Server?>(null)
    var sessionId by mutableIntStateOf(-1)

    private var activeController by mutableStateOf<SftpController?>(null)

    override val isLoading: StateFlow<Boolean> get() = activeController?.isLoading ?: MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> get() = activeController?.isConnected ?: MutableStateFlow(false)
    override val error: StateFlow<String?> get() = activeController?.error ?: MutableStateFlow(null)
    override val currentPath: StateFlow<String> get() = activeController?.currentPath ?: MutableStateFlow("/")
    override val files: StateFlow<List<RemoteFile>> get() = activeController?.files ?: MutableStateFlow(emptyList())

    override val showHiddenFiles: StateFlow<Boolean> get() = activeController?.showHiddenFiles ?: MutableStateFlow(false)
    override val selectedFiles: StateFlow<Set<String>> get() = activeController?.selectedFiles ?: MutableStateFlow(emptySet())
    override val transferProgress: StateFlow<Float> get() = activeController?.transferProgress ?: MutableStateFlow(0f)
    override val isTransferring: StateFlow<Boolean> get() = activeController?.isTransferring ?: MutableStateFlow(false)

    var selectedLogin by mutableStateOf<ServerLogin?>(null)
    val logins = mutableStateListOf<ServerLogin>()

    fun setServer(serverId: Int, sessionId: Int) {
        this.sessionId = sessionId
        viewModelScope.launch {
            val server = repository.getServerById(serverId) ?: return@launch
            currentServer = server
            
            val list = repository.getLoginsForServer(serverId).first()
            logins.clear()
            logins.addAll(list)

            val bundle = SessionManager.getBundle(sessionId)
            if (bundle != null) {
                activeController = bundle.sftp
                selectedLogin = list.firstOrNull { it.id == bundle.lastLoginId }
                // Ensure the list is loaded if it's currently empty
                if (activeController?.files?.value?.isEmpty() == true) {
                    activeController?.connect()
                }
            } else {
                val defaultLogin = list.firstOrNull { it.isDefault }
                selectedLogin = defaultLogin
                val profile = repository.buildConnectionProfile(server, defaultLogin)
                val newBundle = SessionManager.getOrCreateBundle(sessionId, server, profile, RepositoryHostKeyStore(repository))
                activeController = newBundle.sftp
                newBundle.lastLoginId = defaultLogin?.id
            }
        }
    }

    fun selectLogin(login: ServerLogin?) {
        if (selectedLogin?.id == login?.id) return
        val server = currentServer ?: return
        val sid = sessionId
        if (sid < 0) return
        
        selectedLogin = login
        viewModelScope.launch {
            val profile = repository.buildConnectionProfile(server, login)
            SessionManager.closeSession(sid)
            val bundle = SessionManager.getOrCreateBundle(sid, server, profile, RepositoryHostKeyStore(repository))
            activeController = bundle.sftp
            bundle.lastLoginId = login?.id
        }
    }

    override fun connect() { activeController?.connect() }
    override suspend fun listDirectory(path: String) { activeController?.listDirectory(path) }
    override suspend fun listFiles(path: String): List<RemoteFile> = activeController?.listFiles(path) ?: emptyList()
    override fun goUp() { activeController?.goUp() }
    override fun goTo(path: String) { activeController?.goTo(path) }
    override fun toggleHiddenFiles() { activeController?.toggleHiddenFiles() }
    override fun toggleSelection(path: String) { activeController?.toggleSelection(path) }
    override fun clearSelection() { activeController?.clearSelection() }
    
    override suspend fun download(remoteDir: String, remoteFile: RemoteFile, target: PlatformTransferFile): Boolean = 
        activeController?.download(remoteDir, remoteFile, target) ?: false
        
    override suspend fun downloadToDir(remoteDir: String, remoteFile: RemoteFile, targetDir: PlatformTransferDir): Boolean =
        activeController?.downloadToDir(remoteDir, remoteFile, targetDir) ?: false

    override suspend fun readRemoteFile(remotePath: String, maxBytes: Int): ByteArray? = 
        activeController?.readRemoteFile(remotePath, maxBytes)
        
    override suspend fun upload(files: List<PlatformTransferFile>, remoteDir: String): Boolean = 
        activeController?.upload(files, remoteDir) ?: false

    override suspend fun upload(localFilePath: String, remoteDir: String): Boolean = 
        activeController?.upload(localFilePath, remoteDir) ?: false

    override suspend fun makeDirectory(parentDir: String, name: String): Boolean = 
        activeController?.makeDirectory(parentDir, name) ?: false
        
    override suspend fun rename(remotePath: String, newName: String): Boolean =
        activeController?.rename(remotePath, newName) ?: false

    override suspend fun delete(remoteDir: String, remoteFile: RemoteFile): Boolean = 
        activeController?.delete(remoteDir, remoteFile) ?: false

    override suspend fun readRemoteText(remotePath: String): String? = activeController?.readRemoteText(remotePath)
    override suspend fun writeRemoteText(remotePath: String, text: String): Boolean = activeController?.writeRemoteText(remotePath, text) ?: false
    
    override fun close() { /* Managed by SessionManager */ }

    override fun onCleared() {
        super.onCleared()
        // Don't close here, managed by SessionManager
    }
}
