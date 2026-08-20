package com.neytron.sshcommander.sftp

import com.neytron.sshcommander.data.RemoteFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Logic for synchronizing a local directory with a remote SFTP directory.
 */
class SftpSyncEngine(
    private val controller: SftpController
) {
    sealed class SyncAction {
        data class Upload(val local: File, val remotePath: String) : SyncAction()
        data class Download(val remote: RemoteFile, val local: File) : SyncAction()
    }

    private val _syncQueue = MutableStateFlow<List<SyncAction>>(emptyList())
    val syncQueue: StateFlow<List<SyncAction>> = _syncQueue

    /**
     * Compares local and remote folders and populates the sync queue.
     * Simple logic: if file doesn't exist on remote or is newer locally -> upload.
     */
    suspend fun planSync(localDir: File, remotePath: String) {
        val remoteFiles = controller.listFiles(remotePath)
        val localFiles = localDir.listFiles() ?: emptyArray()

        val actions = mutableListOf<SyncAction>()

        localFiles.forEach { local ->
            if (local.isDirectory) return@forEach // Recursive sync is a future task
            
            val remote = remoteFiles.find { it.name == local.name }
            if (remote == null || local.lastModified() > remote.modifiedTime) {
                actions.add(SyncAction.Upload(local, "$remotePath/${local.name}"))
            }
        }
        
        _syncQueue.value = actions
    }

    suspend fun executeSync() {
        val current = _syncQueue.value
        current.forEach { action ->
            when (action) {
                is SyncAction.Upload -> {
                    // controller.uploadFile(action.local, action.remotePath)
                    // TODO: implement actual upload in controller if not exists
                }
                is SyncAction.Download -> {
                    // controller.downloadFile(action.remote, action.local)
                }
            }
        }
        _syncQueue.value = emptyList()
    }
}
