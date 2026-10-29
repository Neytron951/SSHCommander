package com.neytron.sshcommander.data

import com.jcraft.jsch.ChannelExec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Scanner

class HeadlessSshExecutor(
    private val repository: ServerRepository
) {
    suspend fun listServers(): List<Server> = withContext(Dispatchers.IO) {
        repository.getServers()
    }

    suspend fun findServer(identifier: String): Server {
        val servers = repository.getServers()
        servers.firstOrNull { it.id.toString() == identifier }?.let { return it }
        servers.firstOrNull { it.name.equals(identifier, ignoreCase = true) }?.let { return it }
        servers.firstOrNull { it.host.equals(identifier, ignoreCase = true) }?.let { return it }
        servers.firstOrNull { it.name.contains(identifier, ignoreCase = true) || it.host.contains(identifier, ignoreCase = true) }?.let { return it }
        
        throw IllegalArgumentException("Server not found matching: '$identifier'. Use --list to see available servers.")
    }

    suspend fun executeCommand(serverIdentifier: String, command: String, loginId: Int? = null): String = withContext(Dispatchers.IO) {
        val server = findServer(serverIdentifier)
        val logins = repository.getLoginsForServer(server.id).first()
        val login = if (loginId != null) {
            logins.firstOrNull { it.id == loginId }
        } else {
            logins.firstOrNull { it.isDefault } ?: logins.firstOrNull()
        }

        val profile = repository.buildConnectionProfile(server, login)
        val hostKeyStore = RepositoryHostKeyStore(repository)
        val connectionManager = SshConnectionManager(hostKeyStore)
        val session = connectionManager.getOrCreateSession(server, profile)

        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        val inputStream = channel.inputStream
        val errStream = channel.errStream
        channel.connect()

        val outThread = Thread {
            try {
                val buffer = ByteArray(1024)
                while (true) {
                    val len = inputStream.read(buffer)
                    if (len < 0) break
                    System.out.write(buffer, 0, len)
                    System.out.flush()
                }
            } catch (_: Exception) {}
        }
        outThread.start()

        val errThread = Thread {
            try {
                val buffer = ByteArray(1024)
                while (true) {
                    val len = errStream.read(buffer)
                    if (len < 0) break
                    System.err.write(buffer, 0, len)
                    System.err.flush()
                }
            } catch (_: Exception) {}
        }
        errThread.start()

        while (channel.isConnected) {
            Thread.sleep(100)
        }
        outThread.join(1000)
        errThread.join(1000)
        channel.disconnect()
        ""
    }

    suspend fun listCommands(): List<CustomCommand> = withContext(Dispatchers.IO) {
        repository.getAllCustomCommands().first()
    }

    suspend fun addCommand(name: String, command: String, category: String? = null): Unit = withContext(Dispatchers.IO) {
        repository.insertCustomCommand(
            CustomCommand(
                name = name,
                command = command,
                iconName = "terminal",
                colorHex = "#2196F3",
                orderIndex = 0,
                categoryName = category
            )
        )
    }

    suspend fun deleteCommand(id: Int): Unit = withContext(Dispatchers.IO) {
        val cmds = repository.getAllCustomCommands().first()
        cmds.firstOrNull { it.id == id }?.let {
            repository.deleteCustomCommand(it)
        }
    }

    suspend fun runQuickCommand(commandIdentifier: String, serverIdentifier: String, extraArgs: List<String> = emptyList()): String = withContext(Dispatchers.IO) {
        val cmds = repository.getAllCustomCommands().first()
        val cmd = cmds.firstOrNull { it.id.toString() == commandIdentifier || it.name.equals(commandIdentifier, ignoreCase = true) }
            ?: throw IllegalArgumentException("Quick command not found: '$commandIdentifier'. Use --list-commands to see available commands.")
        
        val finalCommand = resolveVariables(cmd.command, extraArgs)
        executeCommand(serverIdentifier, finalCommand)
    }

    private fun resolveVariables(command: String, providedArgs: List<String>): String {
        val regex = Regex("\\{\\{(.*?)\\}\\}")
        val vars = regex.findAll(command)
            .map { it.groupValues[1].trim() }
            .distinct()
            .toList()

        if (vars.isEmpty()) return command

        val values = mutableMapOf<String, String>()
        providedArgs.forEach { arg ->
            val parts = arg.split('=', limit = 2)
            if (parts.size == 2) {
                values[parts[0].trim()] = parts[1].trim()
            }
        }

        val scanner = Scanner(System.`in`)
        vars.forEach { v ->
            if (!values.containsKey(v)) {
                print("Enter value for variable '$v': ")
                System.out.flush()
                val input = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""
                values[v] = input
            }
        }

        var finalCmd = command
        values.forEach { (key, value) ->
            finalCmd = finalCmd.replace("{{$key}}", value)
        }
        return finalCmd
    }
}
