package com.neytron.sshcommander.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ExportData(
    val servers: List<ServerWithPassword>,
    val customCommands: List<CustomCommand>,
    val logins: List<ServerLoginWithPassword>,
    val folders: List<ServerFolder>? = emptyList(),
    val workspaces: List<Workspace>? = emptyList(),
    val sshKeys: List<SshKeyWithPassphrase>? = emptyList()
)

data class SshKeyWithPassphrase(
    val key: SshKey,
    val passphrase: String?
)

data class ServerWithPassword(
    val server: Server,
    val password: String
)

data class ServerLoginWithPassword(
    val login: ServerLogin,
    val password: String
)

/**
 * JSON export/import of all app data. Backed by [ServerRepository] so it works
 * on both Android and desktop (old Room DAO calls replaced by the interface).
 */
class ExportImportManager(private val repository: ServerRepository) : DataBackupManager {
    private val gson = Gson()

    override suspend fun exportJson(): String = exportData()

    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val servers = repository.getServers().map {
            ServerWithPassword(it, repository.getPassword(it.id) ?: "")
        }
        val commands = repository.getAllCustomCommands().first()
        val logins = mutableListOf<ServerLoginWithPassword>()
        servers.forEach { item ->
            repository.getLoginsForServer(item.server.id).first().forEach { login ->
                logins.add(ServerLoginWithPassword(login, repository.getLoginPassword(login.id) ?: ""))
            }
        }
        val folders = repository.getFolders()
        val workspaces = repository.allWorkspaces.first()
        val sshKeys = repository.getSshKeys().map {
            SshKeyWithPassphrase(it, repository.getKeyPassphrase(it.id))
        }

        gson.toJson(ExportData(servers, commands, logins, folders, workspaces, sshKeys))
    }

    override suspend fun importJson(json: String): Unit = importData(json)

    suspend fun importData(json: String) = withContext(Dispatchers.IO) {
        val type = object : TypeToken<ExportData>() {}.type
        val data: ExportData = gson.fromJson(json, type)

        // 1. SSH Keys (needed for servers/logins)
        val keyIdMap = mutableMapOf<Int, Int>()
        (data.sshKeys ?: emptyList()).forEach { item ->
            val newId = repository.insertSshKey(item.key.copy(id = 0, passphraseKey = item.passphrase))
            keyIdMap[item.key.id] = newId
        }

        // 2. Folders
        val folderIdMap = mutableMapOf<Int, Int>()
        (data.folders ?: emptyList()).forEach { folder ->
            val newId = repository.insertFolder(folder.name)
            folderIdMap[folder.id] = newId
        }

        // 3. Servers
        val serverIdMap = mutableMapOf<Int, Int>()
        data.servers.forEach { item ->
            val newId = repository.insertServer(
                item.server.copy(
                    id = 0, 
                    folderId = item.server.folderId?.let { folderIdMap[it] },
                    sshKeyId = item.server.sshKeyId?.let { keyIdMap[it] }
                ),
                item.password
            )
            serverIdMap[item.server.id] = newId
        }
        
        // 4. Custom Commands
        data.customCommands.forEach { cmd ->
            repository.insertCustomCommand(cmd.copy(id = 0))
        }
        
        // 5. Logins
        val loginIdMap = mutableMapOf<Int, Int>()
        data.logins.forEach { item ->
            val newServerId = serverIdMap[item.login.serverId]
            if (newServerId != null) {
                val newId = repository.insertLogin(
                    item.login.copy(
                        id = 0, 
                        serverId = newServerId,
                        sshKeyId = item.login.sshKeyId?.let { keyIdMap[it] }
                    ), 
                    item.password
                )
                loginIdMap[item.login.id] = newId
            }
        }
        
        // 6. Workspaces
        (data.workspaces ?: emptyList()).forEach { ws ->
            val newItems = ws.items.map { it.copy(
                serverId = serverIdMap[it.serverId] ?: 0,
                loginId = it.loginId?.let { loginIdMap[it] }
            ) }.filter { it.serverId != 0 }
            
            if (newItems.isNotEmpty()) {
                repository.insertWorkspace(ws.copy(id = 0, items = newItems))
            }
        }
    }
}
