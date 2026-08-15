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
    // Nullable so older JSON files (Android legacy / previous exports) import fine.
    val folders: List<ServerFolder>? = emptyList()
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
        gson.toJson(ExportData(servers, commands, logins, folders))
    }

    override suspend fun importJson(json: String): Unit = importData(json)

    suspend fun importData(json: String) = withContext(Dispatchers.IO) {
        val type = object : TypeToken<ExportData>() {}.type
        val data: ExportData = gson.fromJson(json, type)

        // Track old folder id -> new folder id so servers can be re-linked.
        val folderIdMap = mutableMapOf<Int, Int>()
        (data.folders ?: emptyList()).forEach { folder ->
            val newId = repository.insertFolder(folder.name)
            folderIdMap[folder.id] = newId
        }

        // Track old server id -> new server id so logins can be re-linked.
        val serverIdMap = mutableMapOf<Int, Int>()
        data.servers.forEach { item ->
            val newId = repository.insertServer(
                item.server.copy(id = 0, folderId = item.server.folderId?.let { folderIdMap[it] }),
                item.password
            )
            serverIdMap[item.server.id] = newId
        }
        data.customCommands.forEach { cmd ->
            repository.insertCustomCommand(cmd.copy(id = 0))
        }
        data.logins.forEach { item ->
            val newServerId = serverIdMap[item.login.serverId]
            if (newServerId != null) {
                repository.insertLogin(item.login.copy(id = 0, serverId = newServerId), item.password)
            }
        }
    }
}
