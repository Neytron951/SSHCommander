package com.neytron.sshcommander.data

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserProvisioningService {

    suspend fun provisionUser(
        session: Session,
        username: String,
        publicKey: String,
        password: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!session.isConnected) {
                return@withContext Result.failure(Exception("SSH Session is not connected"))
            }

            // 1. Create user if not exists
            // We use sudo -S to read password from stdin if needed, 
            // but usually we hope for passwordless sudo or already being root.
            val createUserCmd = "sudo useradd -m -s /bin/bash $username"
            executeCommand(session, createUserCmd)

            // 2. Set password if provided
            password?.let {
                val setPwCmd = "echo \"$username:$it\" | sudo chpasswd"
                executeCommand(session, setPwCmd)
            }

            // 3. Setup SSH directory
            val homeDir = "/home/$username"
            val setupSshCmd = """
                sudo mkdir -p $homeDir/.ssh
                sudo chmod 700 $homeDir/.ssh
                echo "$publicKey" | sudo tee $homeDir/.ssh/authorized_keys > /dev/null
                sudo chmod 600 $homeDir/.ssh/authorized_keys
                sudo chown -R $username:$username $homeDir/.ssh
            """.trimIndent()
            
            executeCommand(session, setupSshCmd)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeCommand(session: Session, command: String): String {
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(cmdWrapper(command))
        val errorStream = channel.errStream
        val inputStream = channel.inputStream
        
        channel.connect()
        
        val output = inputStream.bufferedReader().readText()
        val error = errorStream.bufferedReader().readText()
        
        val exitStatus = channel.exitStatus
        channel.disconnect()

        if (exitStatus != 0 && exitStatus != -1) {
            throw Exception("Command failed with exit code $exitStatus: $error")
        }
        
        return output
    }

    private fun cmdWrapper(cmd: String): String {
        // We use a subshell to execute multiple lines if needed
        return "bash -c '${cmd.replace("'", "'\\''")}'"
    }
}
