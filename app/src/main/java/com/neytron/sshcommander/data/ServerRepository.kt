package com.neytron.sshcommander.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.neytron.sshcommander.security.SecurityUtils
import com.neytron.sshcommander.widget.ServerWidgetProvider
import kotlinx.coroutines.flow.Flow

class ServerRepository(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).serverDao()

    val allServers: Flow<List<Server>> = dao.getAllServers()

    suspend fun getServerById(id: Int): Server? = dao.getServerById(id)

    suspend fun insertServer(server: Server, password: String): Int {
        val id = dao.insertServer(server).toInt()
        SecurityUtils.savePassword(context, id, password)
        refreshWidgets()
        return id
    }

    suspend fun updateServer(server: Server, password: String?) {
        dao.updateServer(server)
        password?.let {
            SecurityUtils.savePassword(context, server.id, it)
        }
        refreshWidgets()
    }

    suspend fun deleteServer(server: Server) {
        dao.deleteLoginsForServer(server.id)
        dao.deleteServer(server)
        SecurityUtils.deletePassword(context, server.id)
        refreshWidgets()
    }

    fun getPassword(serverId: Int): String {
        return SecurityUtils.getPassword(context, serverId)
    }

    // ---- Server Logins ----

    fun getLoginsForServer(serverId: Int): Flow<List<ServerLogin>> = dao.getLoginsForServer(serverId)

    suspend fun insertLogin(login: ServerLogin, password: String): Int {
        val id = dao.insertLogin(login).toInt()
        SecurityUtils.saveLoginPassword(context, id, password)
        return id
    }

    suspend fun updateLogin(login: ServerLogin, password: String?) {
        dao.updateLogin(login)
        password?.let {
            SecurityUtils.saveLoginPassword(context, login.id, it)
        }
    }

    suspend fun deleteLogin(login: ServerLogin) {
        dao.deleteLogin(login)
        SecurityUtils.deleteLoginPassword(context, login.id)
    }

    suspend fun deleteLoginsForServer(serverId: Int) {
        dao.deleteLoginsForServer(serverId)
    }

    fun getLoginPassword(loginId: Int): String {
        return SecurityUtils.getLoginPassword(context, loginId)
    }

    suspend fun updateLastSftpPath(serverId: Int, path: String) {
        dao.updateLastSftpPath(serverId, path)
    }

    suspend fun updateLoginLastSftpPath(loginId: Int, path: String) {
        dao.updateLoginLastSftpPath(loginId, path)
    }

    /**
     * Builds the authentication profile for a connection attempt.
     * If [login] is null the server's own main credentials are used.
     */
    suspend fun buildConnectionProfile(server: Server, login: ServerLogin?): ConnectionProfile {
        return if (login == null) {
            ConnectionProfile(
                username = server.username,
                password = getPassword(server.id),
                privateKeyPath = server.privateKeyPath,
                passphrase = server.passphraseKey?.takeIf { it.isNotBlank() }?.let { getPassword(server.id) }
            )
        } else {
            ConnectionProfile(
                username = login.username,
                password = getLoginPassword(login.id),
                privateKeyPath = login.privateKeyPath,
                passphrase = login.passphraseKey?.takeIf { it.isNotBlank() }?.let { getLoginPassword(login.id) }
            )
        }
    }

    private fun refreshWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Refresh the universal widget
        val componentName = ComponentName(context, ServerWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isNotEmpty()) {
            val intent = Intent(context, ServerWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            }
            context.sendBroadcast(intent)
        }
    }
}
