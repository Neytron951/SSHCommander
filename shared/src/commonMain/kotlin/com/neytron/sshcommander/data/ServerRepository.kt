package com.neytron.sshcommander.data

import kotlinx.coroutines.flow.Flow

/**
 * Cross-platform server persistence.
 *
 * Android will eventually back this with Room; the desktop implementation
 * persists to a local JSON file. The UI only depends on this interface.
 */
interface ServerRepository {
    /** All stored servers, ordered as inserted. */
    val allServers: Flow<List<Server>>

    /** Loads the current list of servers (for non-collect flows). */
    suspend fun getServers(): List<Server>

    /** Returns a single server by id, or null. */
    suspend fun getServerById(id: Int): Server?

    /** Servers with the "show in widget" flag set. */
    fun getWidgetServers(): Flow<List<Server>>

    /**
     * Saves a new server, persisting its password too.
     * @return the server id assigned by the storage layer.
     */
    suspend fun insertServer(server: Server, password: String): Int

    suspend fun updateServer(server: Server, password: String?)

    suspend fun deleteServer(serverId: Int)

    /** Returns the stored password for a server, or null. */
    suspend fun getPassword(serverId: Int): String?

    /** Builds an authentication profile for a connection attempt. */
    suspend fun buildConnectionProfile(server: Server): ConnectionProfile

    /** Builds an authentication profile for a connection attempt using a saved login. */
    suspend fun buildConnectionProfile(server: Server, login: ServerLogin?): ConnectionProfile

    // ---- Server Folders ----

    /** All folders, ordered as created. */
    val allFolders: Flow<List<ServerFolder>>

    /** Loads the current folder list (for non-collect flows). */
    suspend fun getFolders(): List<ServerFolder>

    /** Creates a new folder. @return the assigned folder id. */
    suspend fun insertFolder(name: String): Int

    suspend fun updateFolder(folder: ServerFolder)

    /** Deletes a folder; its servers move to the unfiled group (folderId = null). */
    suspend fun deleteFolder(folderId: Int)

    // ---- Server Logins ----

    fun getLoginsForServer(serverId: Int): Flow<List<ServerLogin>>

    suspend fun insertLogin(login: ServerLogin, password: String): Int

    suspend fun updateLogin(login: ServerLogin, password: String?)

    suspend fun deleteLogin(login: ServerLogin)

    suspend fun deleteLoginsForServer(serverId: Int)

    /** Returns the stored password for a login, or null. */
    suspend fun getLoginPassword(loginId: Int): String?

    // ---- Custom Commands ----

    fun getAllCustomCommands(): Flow<List<CustomCommand>>

    suspend fun insertCustomCommand(command: CustomCommand)

    suspend fun updateCustomCommand(command: CustomCommand)

    suspend fun deleteCustomCommand(command: CustomCommand)

    suspend fun getCommandCategories(): List<String>

    // ---- Workspaces ----

    val allWorkspaces: Flow<List<Workspace>>

    suspend fun insertWorkspace(workspace: Workspace): Int

    suspend fun updateWorkspace(workspace: Workspace)

    suspend fun deleteWorkspace(id: Int)

    // ---- Command History ----

    fun getHistoryForServer(serverId: Int): Flow<List<CommandHistoryEntity>>

    suspend fun insertHistory(history: CommandHistoryEntity): Long

    suspend fun updateHistoryOutput(id: Long, output: String)

    suspend fun clearHistoryForServer(serverId: Int)

    // ---- SFTP paths ----

    suspend fun updateLastSftpPath(serverId: Int, path: String)

    suspend fun updateLoginLastSftpPath(loginId: Int, path: String)

    // ---- SSH Keys ----

    /** All stored SSH keys. */
    val allSshKeys: Flow<List<SshKey>>

    suspend fun getSshKeys(): List<SshKey>

    suspend fun getSshKeyById(id: Int): SshKey?

    suspend fun insertSshKey(key: SshKey): Int

    suspend fun updateSshKey(key: SshKey)

    suspend fun deleteSshKey(id: Int)

    suspend fun getKeyPassphrase(id: Int): String?

    /**
     * Provisions a new user on the remote server using an active connection.
     * @param serverId the target server ID
     * @param username the new username to create
     * @param publicKey the public key to authorize
     * @param password optional password for the new user
     */
    suspend fun provisionUser(serverId: Int, username: String, publicKey: String, password: String?): Result<Unit>
}
