package com.neytron.sshcommander.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.data.AppSettings
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.ServerRepository
import kotlinx.coroutines.launch

class AddEditServerViewModel(
    private val repository: ServerRepository,
    private val settings: AppSettings
) : ViewModel() {
    var name by mutableStateOf("")
    var host by mutableStateOf("")
    var port by mutableStateOf("22")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var iconName by mutableStateOf("Default")
    var showInWidget by mutableStateOf(false)
    var sftpStartPath by mutableStateOf("")
    var folderId by mutableStateOf<Int?>(null)

    var nameError by mutableStateOf<String?>(null)
    var hostError by mutableStateOf<String?>(null)
    var usernameError by mutableStateOf<String?>(null)

    private var currentServerId: Int? = null

    fun loadServer(serverId: Int) {
        viewModelScope.launch {
            repository.getServerById(serverId)?.let { server ->
                currentServerId = server.id
                name = server.name
                host = server.host
                port = server.port.toString()
                username = server.username
                password = repository.getPassword(server.id) ?: ""
                iconName = server.iconName
                showInWidget = server.showInWidget
                sftpStartPath = server.sftpStartPath ?: ""
                folderId = server.folderId
            }
        }
    }

    private fun validate(): Boolean {
        var isValid = true
        if (name.isBlank()) { nameError = "Name cannot be empty"; isValid = false } else { nameError = null }
        if (host.isBlank()) { hostError = "Host/IP cannot be empty"; isValid = false } else { hostError = null }
        if (username.isBlank()) { usernameError = "Username cannot be empty"; isValid = false } else { usernameError = null }
        return isValid
    }

    fun saveServer(onComplete: () -> Unit) {
        if (!validate()) return

        viewModelScope.launch {
            val server = Server(
                id = currentServerId ?: 0,
                name = name,
                host = host,
                port = port.toIntOrNull() ?: 22,
                username = username,
                passwordKey = "",
                iconName = iconName,
                showInWidget = showInWidget,
                sftpStartPath = sftpStartPath.trim().ifBlank { null },
                folderId = folderId
            )
            if (currentServerId == null) {
                currentServerId = repository.insertServer(server, password)
            } else {
                repository.updateServer(server, password)
            }
            onComplete()
        }
    }
}
