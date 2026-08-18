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

    /** Establishes the SFTP channel (reusing the shared SSH session). */
    fun connect()

    /** Lists the contents of [path] and updates [currentPath] on success. */
    fun listDirectory(path: String = currentPath.value)

    fun goUp()
    fun goTo(path: String)

    /** Downloads [remoteFile] from [remoteDir] into the local directory [destinationDirPath]. */
    suspend fun download(remoteDir: String, remoteFile: RemoteFile, destinationDirPath: String): Boolean

    /**
     * Reads up to [maxBytes] from the beginning of a remote file and returns
     * them as raw bytes (used for basic previews). Returns null on error.
     */
    suspend fun readRemoteFile(remotePath: String, maxBytes: Int): ByteArray?

    /** Uploads a local file (by path) into [remoteDir]. */
    suspend fun upload(localFilePath: String, remoteDir: String): Boolean

    /** Creates a directory [name] under [parentDir]. */
    suspend fun makeDirectory(parentDir: String, name: String): Boolean

    /** Deletes a remote file or directory. */
    suspend fun delete(remoteDir: String, remoteFile: RemoteFile): Boolean

    /** Current directory listing (null while not connected). */
    val files: StateFlow<List<RemoteFile>>

    fun close()
}

/** Factory supplied by each platform entry point. */
fun interface SftpSessionFactory {
    fun create(server: Server, profile: ConnectionProfile): SftpController
}
