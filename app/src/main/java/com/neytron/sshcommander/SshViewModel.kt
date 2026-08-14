package com.neytron.sshcommander

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

class SshViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("ssh_prefs", Context.MODE_PRIVATE)

    var host by mutableStateOf(prefs.getString("host", "") ?: "")
    var port by mutableStateOf(prefs.getString("port", "22") ?: "22")
    var username by mutableStateOf(prefs.getString("username", "") ?: "")
    var password by mutableStateOf(prefs.getString("password", "") ?: "")

    var terminalOutput by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var currentCommand by mutableStateOf("")
    var totalCommands by mutableStateOf(0)
    
    private var currentSession: Session? = null
    private var currentChannel: ChannelExec? = null

    fun saveCredentials() {
        prefs.edit().apply {
            putString("host", host)
            putString("port", port)
            putString("username", username)
            putString("password", password)
            apply()
        }
    }

    fun executeCommand(command: String) {
        saveCredentials()
        isLoading = true
        errorMessage = null
        terminalOutput = "$ $command\n"
        currentCommand = ""
        totalCommands = 0

        val commands = command.lines().filter { it.isNotBlank() }
        
        viewModelScope.launch {
            try {
                if (commands.isEmpty()) {
                    terminalOutput += "No commands to execute\n"
                    return@launch
                }
                
                totalCommands = commands.size
                
                for ((index, cmd) in commands.withIndex()) {
                    if (!isActive) break
                    currentCommand = "$cmd [${index + 1}/$totalCommands]"
                    terminalOutput += ">>> Command ${index + 1}: $cmd\n"
                    
                    withContext(Dispatchers.IO) {
                        performSshCommandStreaming(cmd)
                    }
                    
                    if (index < commands.size - 1) {
                        terminalOutput += "${"=".repeat(50)}\n"
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Unknown error occurred"
                terminalOutput += "\n❌ Error: ${e.message}\n"
            } finally {
                isLoading = false
                currentCommand = ""
                totalCommands = 0
                disconnectSession()
            }
        }
    }

    private fun performSshCommandStreaming(command: String) {
        var session: Session? = null
        var channel: ChannelExec? = null
        try {
            val jsch = JSch()
            val portInt = port.toIntOrNull() ?: 22
            session = jsch.getSession(username, host, portInt)
            session.setPassword(password)

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.connect(10000)

            channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.setInputStream(null)
            
            val inputStream = channel.inputStream
            val errorStream = channel.errStream
            
            channel.connect()
            
            currentSession = session
            currentChannel = channel
            
            val inputReader = BufferedReader(InputStreamReader(inputStream))
            val errorReader = BufferedReader(InputStreamReader(errorStream))
            
            var line = inputReader.readLine()
            while (line != null && isLoading) {
                terminalOutput += "$line\n"
                line = inputReader.readLine()
            }
            
            // Read any remaining error output
            var errorLine = errorReader.readLine()
            while (errorLine != null && isLoading) {
                terminalOutput += "❌ $errorLine\n"
                errorLine = errorReader.readLine()
            }
            
            // Wait for channel to close
            while (channel.isConnected && isLoading) {
                Thread.sleep(100)
            }
            
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }
    
    fun clearTerminal() {
        terminalOutput = ""
    }
    
    fun stopExecution() {
        isLoading = false
        currentCommand = ""
        totalCommands = 0
        disconnectSession()
        terminalOutput += "\n⏹ Execution stopped by user\n"
    }
    
    private fun disconnectSession() {
        try {
            currentChannel?.sendSignal("INT") // Send Ctrl+C
        } catch (e: Exception) {
            // Ignore if signal fails
        }
        currentChannel?.disconnect()
        currentSession?.disconnect()
        currentChannel = null
        currentSession = null
    }
}
