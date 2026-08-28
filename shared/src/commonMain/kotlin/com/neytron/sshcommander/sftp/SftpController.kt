package com.neytron.sshcommander.sftp

import com.neytron.sshcommander.data.ConnectionProfile
import com.neytron.sshcommander.data.RemoteFile
import com.neytron.sshcommander.data.Server
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over an SFTP channel so the shared UI can browse the remote
 * filesystem without depending on the concrete JSch implementation.
 */
interface SftpController {
    val isLoading: StateFlow<Boolean>
    val isConnected: StateFlow<Boolean>
    val error: StateFlow<String?>
    val currentPath: StateFlow<String>
    val files: StateFlow<List<RemoteFile>>
    
    val showHiddenFiles: StateFlow<Boolean>
    val selectedFiles: StateFlow<Set<String>>
    val transferProgress: StateFlow<Float>
    val isTransferring: StateFlow<Boolean>

    /** Establishes the SFTP channel (reusing the shared SSH session). */
    fun connect()

    /** Lists the contents of [path] and updates [currentPath] on success. */
    suspend fun listDirectory(path: String = currentPath.value)

    /** Lists the contents of [path] and returns them. */
    suspend fun listFiles(path: String): List<RemoteFile>

    fun goUp()
    fun goTo(path: String)
    fun toggleHiddenFiles()
    fun toggleSelection(path: String)
    fun clearSelection()

    /** Downloads [remoteFile] from [remoteDir] into the local file [target]. */
    suspend fun download(remoteDir: String, remoteFile: RemoteFile, target: com.neytron.sshcommander.ui.PlatformTransferFile): Boolean

    /** Downloads [remoteFile] from [remoteDir] into the local directory [targetDir]. */
    suspend fun downloadToDir(remoteDir: String, remoteFile: RemoteFile, targetDir: com.neytron.sshcommander.ui.PlatformTransferDir): Boolean

    /**
     * Reads up to [maxBytes] from the beginning of a remote file and returns
     * them as raw bytes (used for basic previews). Returns null on error.
     */
    suspend fun readRemoteFile(remotePath: String, maxBytes: Int): ByteArray?

    /** Uploads local files into [remoteDir]. */
    suspend fun upload(files: List<com.neytron.sshcommander.ui.PlatformTransferFile>, remoteDir: String): Boolean

    /** Uploads a single local file (by path) into [remoteDir]. */
    suspend fun upload(localFilePath: String, remoteDir: String): Boolean

    /** Creates a directory [name] under [parentDir]. */
    suspend fun makeDirectory(parentDir: String, name: String): Boolean

    /** Renames a remote file. */
    suspend fun rename(remotePath: String, newName: String): Boolean

    /** Deletes a remote file or directory. */
    suspend fun delete(remoteDir: String, remoteFile: RemoteFile): Boolean

    /** Reads the whole remote file as a string. */
    suspend fun readRemoteText(remotePath: String): String?

    /** Saves a string as a remote file. */
    suspend fun writeRemoteText(remotePath: String, text: String): Boolean

    fun close()
}

/** Factory supplied by each platform entry point. */
fun interface SftpSessionFactory {
    fun create(server: Server, profile: ConnectionProfile): SftpController
}
