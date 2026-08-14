package com.neytron.sshcommander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.data.NetworkUtils
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ServerRepository(application)
    val servers = repository.allServers

    private val _serverStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val serverStatuses: StateFlow<Map<Int, Boolean>> = _serverStatuses.asStateFlow()

    private var statusCheckJob: kotlinx.coroutines.Job? = null

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            repository.deleteServer(server)
        }
    }

    /**
     * Checks TCP reachability on the server's SSH port (22 by default).
     * A guard prevents overlapping checks so repeated calls (e.g. periodic
     * refresh) don't pile up network traffic on the phone.
     */
    fun checkStatuses(serverList: List<Server>) {
        if (statusCheckJob?.isActive == true) return
        statusCheckJob = viewModelScope.launch(Dispatchers.IO) {
            val statuses = mutableMapOf<Int, Boolean>()
            serverList.forEach { server ->
                statuses[server.id] = NetworkUtils.isPortOpen(server.host, server.port, timeoutMs = 2000)
            }
            _serverStatuses.value = statuses
        }
    }

    /** Resets cached statuses (used after a manual refresh request). */
    fun clearStatusCache() {
        _serverStatuses.value = emptyMap()
    }
}
