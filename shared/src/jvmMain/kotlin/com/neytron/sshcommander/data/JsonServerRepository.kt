package com.neytron.sshcommander.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * File-backed [ServerRepository] for desktop / any JVM host.
 *
 * Layout inside [dataDir]:
 *   servers.json     — the server list (IDs, host, username, ...)
 *   passwords.json   — server id → password (plain, until Phase 3 secure storage)
 *   logins.json      — saved per-server logins
 *   login_passwords.json — login id → password
 *   commands.json    — custom commands
 *   history.json     — command history per server
 *   folders.json     — server grouping folders
 *   keys.json        — managed SSH key pairs
 *   key_passwords.json — key id → passphrase
 *
 * NOTE: passwords are stored in plain text here on purpose — secure storage
 * (Windows DPAPI / Android Keystore) is a dedicated phase-3 task.
 */
class JsonServerRepository(
    private val dataDir: File,
    private val secureStorage: SecureStorage? = null
) : ServerRepository {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        // Kotlin data classes have no no-arg constructor; Gson normally
        // instantiates them via sun.misc.Unsafe, which is unavailable inside a
        // jpackage runtime image. Provide InstanceCreators so deserialization
        // works in the packaged (MSI/EXE) build.
        .registerTypeAdapter(Server::class.java, InstanceCreator { _ ->
            Server(name = "", host = "", username = "", isPinned = false, sshKeyId = null)
        })
        .registerTypeAdapter(ServerLogin::class.java, InstanceCreator { _ ->
            ServerLogin(serverId = 0, label = "", username = "", sshKeyId = null)
        })
        .registerTypeAdapter(CustomCommand::class.java, InstanceCreator { _ ->
            CustomCommand(name = "", command = "", iconName = "Default", colorHex = "", orderIndex = 0)
        })
        .registerTypeAdapter(CommandHistoryEntity::class.java, InstanceCreator { _ ->
            CommandHistoryEntity(serverId = 0, command = "", output = "")
        })
        .registerTypeAdapter(ServerFolder::class.java, InstanceCreator { _ ->
            ServerFolder(name = "")
        })
        .registerTypeAdapter(Workspace::class.java, InstanceCreator { _ ->
            Workspace(name = "")
        })
        .registerTypeAdapter(WorkspaceItem::class.java, InstanceCreator { _ ->
            WorkspaceItem(serverId = 0)
        })
        .registerTypeAdapter(SshKey::class.java, InstanceCreator { _ ->
            SshKey(name = "", type = "RSA")
        })
        .create()
    private val mutex = Mutex()
    private val serversFile = File(dataDir, "servers.json")
    private val passwordsFile = File(dataDir, "passwords.json")
    private val loginsFile = File(dataDir, "logins.json")
    private val loginPasswordsFile = File(dataDir, "login_passwords.json")
    private val commandsFile = File(dataDir, "commands.json")
    private val historyFile = File(dataDir, "history.json")
    private val foldersFile = File(dataDir, "folders.json")
    private val workspacesFile = File(dataDir, "workspaces.json")
    private val keysFile = File(dataDir, "keys.json")
    private val keyPasswordsFile = File(dataDir, "key_passwords.json")

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    override val allServers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val _folders = MutableStateFlow<List<ServerFolder>>(emptyList())
    override val allFolders: StateFlow<List<ServerFolder>> = _folders.asStateFlow()

    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    override val allWorkspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _sshKeys = MutableStateFlow<List<SshKey>>(emptyList())
    override val allSshKeys: StateFlow<List<SshKey>> = _sshKeys.asStateFlow()

    private val _logins = MutableStateFlow<List<ServerLogin>>(emptyList())
    private val _commands = MutableStateFlow<List<CustomCommand>>(emptyList())
    private val _history = MutableStateFlow<List<CommandHistoryEntity>>(emptyList())

    init {
        dataDir.mkdirs()
        _servers.value = readServers()
        _folders.value = readFolders()
        _logins.value = readLogins()
        _commands.value = readCommands()
        _history.value = readHistory()
        _workspaces.value = readWorkspaces()
        _sshKeys.value = readSshKeys()
    }

    override suspend fun getServers(): List<Server> = withContext(Dispatchers.IO) {
        mutex.withLock { _servers.value }
    }

    override suspend fun getServerById(id: Int): Server? = withContext(Dispatchers.IO) {
        mutex.withLock { _servers.value.firstOrNull { it.id == id } }
    }

    // ---- Server Folders ----

    override suspend fun getFolders(): List<ServerFolder> = withContext(Dispatchers.IO) {
        mutex.withLock { _folders.value }
    }

    override suspend fun insertFolder(name: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _folders.value
            val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
            _folders.update { current + ServerFolder(id = newId, name = name) }
            writeFolders(_folders.value)
            newId
        }
    }

    override suspend fun updateFolder(folder: ServerFolder): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _folders.update { list -> list.map { if (it.id == folder.id) folder else it } }
            writeFolders(_folders.value)
        }
    }

    override suspend fun deleteFolder(folderId: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _folders.update { list -> list.filterNot { it.id == folderId } }
            writeFolders(_folders.value)
            // Servers in the deleted folder move to the unfiled group.
            _servers.update { list -> list.map { if (it.folderId == folderId) it.copy(folderId = null) else it } }
            writeServers(_servers.value)
        }
    }

    override fun getWidgetServers(): Flow<List<Server>> =
        _servers.map { list -> list.filter { it.showInWidget } }

    override suspend fun insertServer(server: Server, password: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _servers.value
            val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
            val persisted = server.copy(id = newId)
            _servers.update { current + persisted }
            writeServers(_servers.value)
            storePassword(newId, password)
            newId
        }
    }

    override suspend fun updateServer(server: Server, password: String?): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _servers.value
            _servers.update { list -> list.map { if (it.id == server.id) server else it } }
            writeServers(_servers.value)
            password?.let { storePassword(server.id, it) }
        }
    }

    override suspend fun deleteServer(serverId: Int) = withContext(Dispatchers.IO) {
        mutex.withLock {
            _servers.update { list -> list.filterNot { it.id == serverId } }
            writeServers(_servers.value)
            removePassword(serverId)
            // Cascade: remove this server's logins and their passwords
            val removedLogins = _logins.value.filter { it.serverId == serverId }
            removedLogins.forEach { removeLoginPassword(it.id) }
            _logins.update { list -> list.filterNot { it.serverId == serverId } }
            writeLogins(_logins.value)
            // Remove history for the server
            _history.update { list -> list.filterNot { it.serverId == serverId } }
            writeHistory(_history.value)
        }
    }

    override suspend fun getPassword(serverId: Int): String? = withContext(Dispatchers.IO) {
        mutex.withLock { loadPassword(serverId) }
    }

    override suspend fun buildConnectionProfile(server: Server): ConnectionProfile {
        val managedKey = server.sshKeyId?.let { getSshKeyById(it) }
        return ConnectionProfile(
            username = server.username,
            password = getPassword(server.id) ?: "",
            privateKeyPath = server.privateKeyPath ?: managedKey?.privateKeyPath,
            privateKeyContent = managedKey?.privateKeyContent,
            publicKeyContent = managedKey?.publicKeyContent,
            passphrase = server.passphraseKey?.takeIf { it.isNotBlank() } ?: managedKey?.id?.let { loadKeyPassphrase(it) }
        )
    }

    override suspend fun buildConnectionProfile(server: Server, login: ServerLogin?): ConnectionProfile {
        if (login == null) return buildConnectionProfile(server)
        
        val managedKey = login.sshKeyId?.let { getSshKeyById(it) }
        return ConnectionProfile(
            username = login.username,
            password = getLoginPassword(login.id) ?: "",
            privateKeyPath = login.privateKeyPath ?: managedKey?.privateKeyPath,
            privateKeyContent = managedKey?.privateKeyContent,
            publicKeyContent = managedKey?.publicKeyContent,
            passphrase = login.passphraseKey?.takeIf { it.isNotBlank() }
                ?.let { getLoginPassword(login.id) }
                ?: managedKey?.id?.let { loadKeyPassphrase(it) }
        )
    }

    // ---- Server Logins ----

    override fun getLoginsForServer(serverId: Int): Flow<List<ServerLogin>> =
        _logins.map { list -> list.filter { it.serverId == serverId }.sortedBy { it.id } }

    override suspend fun insertLogin(login: ServerLogin, password: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _logins.value
            val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
            val persisted = login.copy(id = newId)
            _logins.update { current + persisted }
            writeLogins(_logins.value)
            storeLoginPassword(newId, password)
            newId
        }
    }

    override suspend fun updateLogin(login: ServerLogin, password: String?): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _logins.update { list -> list.map { if (it.id == login.id) login else it } }
            writeLogins(_logins.value)
            password?.let { storeLoginPassword(login.id, it) }
        }
    }

    override suspend fun deleteLogin(login: ServerLogin): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _logins.update { list -> list.filterNot { it.id == login.id } }
            writeLogins(_logins.value)
            removeLoginPassword(login.id)
        }
    }

    override suspend fun deleteLoginsForServer(serverId: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val removed = _logins.value.filter { it.serverId == serverId }
            removed.forEach { removeLoginPassword(it.id) }
            _logins.update { list -> list.filterNot { it.serverId == serverId } }
            writeLogins(_logins.value)
        }
    }

    override suspend fun getLoginPassword(loginId: Int): String? = withContext(Dispatchers.IO) {
        mutex.withLock { loadLoginPassword(loginId) }
    }

    // ---- Custom Commands ----

    override fun getAllCustomCommands(): Flow<List<CustomCommand>> =
        _commands.map { list -> list.sortedBy { it.orderIndex } }

    override suspend fun insertCustomCommand(command: CustomCommand): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _commands.value
            val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
            _commands.update { current + command.copy(id = newId) }
            writeCommands(_commands.value)
        }
    }

    override suspend fun updateCustomCommand(command: CustomCommand): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _commands.update { list -> list.map { if (it.id == command.id) command else it } }
            writeCommands(_commands.value)
        }
    }

    override suspend fun deleteCustomCommand(command: CustomCommand): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _commands.update { list -> list.filterNot { it.id == command.id } }
            writeCommands(_commands.value)
        }
    }

    override suspend fun getCommandCategories(): List<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            _commands.value.mapNotNull { it.categoryName }.distinct().sorted()
        }
    }

    // ---- Workspaces ----

    override suspend fun insertWorkspace(workspace: Workspace): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _workspaces.value
            val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
            val persisted = workspace.copy(id = newId)
            _workspaces.update { current + persisted }
            writeWorkspaces(_workspaces.value)
            newId
        }
    }

    override suspend fun updateWorkspace(workspace: Workspace): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _workspaces.update { list -> list.map { if (it.id == workspace.id) workspace else it } }
            writeWorkspaces(_workspaces.value)
        }
    }

    override suspend fun deleteWorkspace(id: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _workspaces.update { list -> list.filterNot { it.id == id } }
            writeWorkspaces(_workspaces.value)
        }
    }

    // ---- Command History ----

    override fun getHistoryForServer(serverId: Int): Flow<List<CommandHistoryEntity>> =
        _history.map { list -> list.filter { it.serverId == serverId }.sortedByDescending { it.timestamp } }

    override suspend fun insertHistory(history: CommandHistoryEntity): Long = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _history.value
            val newId = (current.maxOfOrNull { it.id }?.toLong() ?: 0L) + 1
            _history.update { current + history.copy(id = newId.toInt()) }
            writeHistory(_history.value)
            newId
        }
    }

    override suspend fun updateHistoryOutput(id: Long, output: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _history.update { list ->
                list.map { if (it.id.toLong() == id) it.copy(output = output) else it }
            }
            writeHistory(_history.value)
        }
    }

    override suspend fun clearHistoryForServer(serverId: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _history.update { list -> list.filterNot { it.serverId == serverId } }
            writeHistory(_history.value)
        }
    }

    // ---- SFTP paths ----

    override suspend fun updateLastSftpPath(serverId: Int, path: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _servers.update { list -> list.map { if (it.id == serverId) it.copy(lastSftpPath = path) else it } }
            writeServers(_servers.value)
        }
    }

    override suspend fun updateLoginLastSftpPath(loginId: Int, path: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _logins.update { list -> list.map { if (it.id == loginId) it.copy(lastSftpPath = path) else it } }
            writeLogins(_logins.value)
        }
    }

    // ---- SSH Keys ----

    override suspend fun getSshKeys(): List<SshKey> = withContext(Dispatchers.IO) {
        mutex.withLock { _sshKeys.value }
    }

    override suspend fun getSshKeyById(id: Int): SshKey? = withContext(Dispatchers.IO) {
        mutex.withLock { _sshKeys.value.firstOrNull { it.id == id } }
    }

    override suspend fun insertSshKey(key: SshKey): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _sshKeys.value
            val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
            val persisted = key.copy(id = newId, createdAt = System.currentTimeMillis())
            _sshKeys.update { current + persisted }
            writeSshKeys(_sshKeys.value)
            key.passphraseKey?.let { storeKeyPassphrase(newId, it) }
            newId
        }
    }

    override suspend fun updateSshKey(key: SshKey): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _sshKeys.update { list -> list.map { if (it.id == key.id) key else it } }
            writeSshKeys(_sshKeys.value)
            key.passphraseKey?.let { storeKeyPassphrase(key.id, it) }
        }
    }

    override suspend fun deleteSshKey(id: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            _sshKeys.update { list -> list.filterNot { it.id == id } }
            writeSshKeys(_sshKeys.value)
            removeKeyPassphrase(id)
        }
    }

    override suspend fun getKeyPassphrase(id: Int): String? = withContext(Dispatchers.IO) {
        mutex.withLock { loadKeyPassphrase(id) }
    }

    override suspend fun provisionUser(
        serverId: Int,
        username: String,
        publicKey: String,
        password: String?
    ): Result<Unit> {
        val session = SshConnectionManager.getActiveSession(serverId)
            ?: return Result.failure(Exception("No active session found for server $serverId"))
        
        return UserProvisioningService().provisionUser(session, username, publicKey, password)
    }

    // ---- password helpers ----

    private suspend fun storePassword(serverId: Int, password: String) {
        val secure = secureStorage
        if (secure != null) {
            secure.put("server-$serverId", password)
        } else {
            val pw = readPasswords().toMutableMap()
            pw[serverId] = password
            writePasswords(pw)
        }
    }

    private suspend fun loadPassword(serverId: Int): String? {
        val secure = secureStorage
        return if (secure != null) {
            secure.get("server-$serverId")
        } else {
            readPasswords()[serverId]
        }
    }

    private suspend fun removePassword(serverId: Int) {
        val secure = secureStorage
        if (secure != null) {
            secure.remove("server-$serverId")
        } else {
            val pw = readPasswords().toMutableMap()
            pw.remove(serverId)
            writePasswords(pw)
        }
    }

    // ---- login password helpers ----

    private suspend fun storeLoginPassword(loginId: Int, password: String) {
        val secure = secureStorage
        if (secure != null) {
            secure.put("login-$loginId", password)
        } else {
            val pw = readLoginPasswords().toMutableMap()
            pw[loginId] = password
            writeLoginPasswords(pw)
        }
    }

    private suspend fun loadLoginPassword(loginId: Int): String? {
        val secure = secureStorage
        return if (secure != null) {
            secure.get("login-$loginId")
        } else {
            readLoginPasswords()[loginId]
        }
    }

    private suspend fun removeLoginPassword(loginId: Int) {
        val secure = secureStorage
        if (secure != null) {
            secure.remove("login-$loginId")
        } else {
            val pw = readLoginPasswords().toMutableMap()
            pw.remove(loginId)
            writeLoginPasswords(pw)
        }
    }

    // ---- key passphrase helpers ----

    private suspend fun storeKeyPassphrase(keyId: Int, passphrase: String) {
        val secure = secureStorage
        if (secure != null) {
            secure.put("key-$keyId", passphrase)
        } else {
            val pw = readKeyPassphrases().toMutableMap()
            pw[keyId] = passphrase
            writeKeyPassphrases(pw)
        }
    }

    private suspend fun loadKeyPassphrase(keyId: Int): String? {
        val secure = secureStorage
        return if (secure != null) {
            secure.get("key-$keyId")
        } else {
            readKeyPassphrases()[keyId]
        }
    }

    private suspend fun removeKeyPassphrase(keyId: Int) {
        val secure = secureStorage
        if (secure != null) {
            secure.remove("key-$keyId")
        } else {
            val pw = readKeyPassphrases().toMutableMap()
            pw.remove(keyId)
            writeKeyPassphrases(pw)
        }
    }

    // ---- private persistence helpers ----

    private fun readServers(): List<Server> {
        if (!serversFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<Server>>() {}.type
            val list: List<Server> = gson.fromJson(serversFile.readText(), type)
            list ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeServers(servers: List<Server>) {
        serversFile.writeText(gson.toJson(servers))
    }

    private fun readFolders(): List<ServerFolder> {
        if (!foldersFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<ServerFolder>>() {}.type
            val list: List<ServerFolder> = gson.fromJson(foldersFile.readText(), type)
            list ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeFolders(folders: List<ServerFolder>) {
        foldersFile.writeText(gson.toJson(folders))
    }

    private fun readPasswords(): Map<Int, String> {
        if (!passwordsFile.exists()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val raw: Map<String, String> = gson.fromJson(passwordsFile.readText(), type) ?: emptyMap()
            raw.mapKeys { it.key.toInt() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun writePasswords(passwords: Map<Int, String> = readPasswords()) {
        passwordsFile.writeText(gson.toJson(passwords.mapKeys { it.key.toString() }))
    }

    private fun readLoginPasswords(): Map<Int, String> {
        if (!loginPasswordsFile.exists()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val raw: Map<String, String> = gson.fromJson(loginPasswordsFile.readText(), type) ?: emptyMap()
            raw.mapKeys { it.key.toInt() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun writeLoginPasswords(passwords: Map<Int, String> = readLoginPasswords()) {
        loginPasswordsFile.writeText(gson.toJson(passwords.mapKeys { it.key.toString() }))
    }

    private fun readLogins(): List<ServerLogin> {
        if (!loginsFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<ServerLogin>>() {}.type
            val list: List<ServerLogin> = gson.fromJson(loginsFile.readText(), type)
            list ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeLogins(logins: List<ServerLogin>) {
        loginsFile.writeText(gson.toJson(logins))
    }

    private fun readCommands(): List<CustomCommand> {
        if (!commandsFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<CustomCommand>>() {}.type
            val list: List<CustomCommand> = gson.fromJson(commandsFile.readText(), type)
            list ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeCommands(commands: List<CustomCommand>) {
        commandsFile.writeText(gson.toJson(commands))
    }

    private fun readHistory(): List<CommandHistoryEntity> {
        if (!historyFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<CommandHistoryEntity>>() {}.type
            val list: List<CommandHistoryEntity> = gson.fromJson(historyFile.readText(), type)
            list ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeHistory(history: List<CommandHistoryEntity>) {
        historyFile.writeText(gson.toJson(history))
    }

    private fun readWorkspaces(): List<Workspace> {
        if (!workspacesFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<Workspace>>() {}.type
            val list: List<Workspace> = gson.fromJson(workspacesFile.readText(), type)
            list ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeWorkspaces(workspaces: List<Workspace>) {
        workspacesFile.writeText(gson.toJson(workspaces))
    }

    private fun readSshKeys(): List<SshKey> {
        if (!keysFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<SshKey>>() {}.type
            val list: List<SshKey> = gson.fromJson(keysFile.readText(), type)
            list ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeSshKeys(keys: List<SshKey>) {
        keysFile.writeText(gson.toJson(keys))
    }

    private fun readKeyPassphrases(): Map<Int, String> {
        if (!keyPasswordsFile.exists()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val raw: Map<String, String> = gson.fromJson(keyPasswordsFile.readText(), type) ?: emptyMap()
            raw.mapKeys { it.key.toInt() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun writeKeyPassphrases(passwords: Map<Int, String>) {
        keyPasswordsFile.writeText(gson.toJson(passwords.mapKeys { it.key.toString() }))
    }
}
