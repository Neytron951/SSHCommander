package com.neytron.sshcommander.data

import android.content.Context
import com.neytron.sshcommander.security.SecurityUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ExportData(
    val servers: List<ServerWithPassword>,
    val customCommands: List<CustomCommand>,
    val logins: List<ServerLoginWithPassword>
)

data class ServerWithPassword(
    val server: Server,
    val password: String
)

data class ServerLoginWithPassword(
    val login: ServerLogin,
    val password: String
)

class ExportImportManager(private val context: Context) {
    private val repository = ServerRepository(context)
    private val dao = AppDatabase.getDatabase(context).serverDao()
    private val gson = Gson()

    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val servers = dao.getAllServers().first().map {
            ServerWithPassword(it, repository.getPassword(it.id))
        }
        val commands = dao.getAllCustomCommands().first()
        val logins = mutableListOf<ServerLoginWithPassword>()
        servers.forEach { item ->
            repository.getLoginsForServer(item.server.id).first().forEach { login ->
                logins.add(ServerLoginWithPassword(login, repository.getLoginPassword(login.id)))
            }
        }
        gson.toJson(ExportData(servers, commands, logins))
    }

    suspend fun importData(json: String) = withContext(Dispatchers.IO) {
        val type = object : TypeToken<ExportData>() {}.type
        val data: ExportData = gson.fromJson(json, type)

        // Track old server id -> new server id so logins can be re-linked.
        val serverIdMap = mutableMapOf<Int, Int>()
        data.servers.forEach { item ->
            val newId = repository.insertServer(item.server.copy(id = 0), item.password)
            serverIdMap[item.server.id] = newId
        }
        data.customCommands.forEach { cmd ->
            dao.insertCustomCommand(cmd.copy(id = 0))
        }
        data.logins.forEach { item ->
            val newServerId = serverIdMap[item.login.serverId]
            if (newServerId != null) {
                repository.insertLogin(item.login.copy(id = 0, serverId = newServerId), item.password)
            }
        }
    }
}
